package edu.uoc.biblioteca.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

/**
 * Control de acceso de toda la aplicación. Lo que no se abre aquí expresamente queda reservado a ADMIN,
 * así que una ruta nueva nunca nace pública por descuido.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Solo se ejecuta JavaScript servido por la propia aplicación: ni scripts escritos dentro del HTML ni de
     * otros dominios. Así, aunque alguien colara texto en una página, el navegador no lo ejecutaría.
     * Los estilos admiten el atributo style porque algunas barras de progreso calculan su ancho en la vista.
     */
    static final String POLITICA_DE_CONTENIDO = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data: blob:",
            "font-src 'self' data:",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    /** Días que dura «Recordarme» en este equipo. */
    static final int DIAS_RECORDARME = 14;

    @Bean
    SecurityFilterChain filtroDeSeguridad(HttpSecurity http, IntentosDeAcceso intentos,
                                          CuentaDetailsService cuentas,
                                          @Value("${biblioteca.recordarme.clave:${random.uuid}}") String claveRecordarme)
            throws Exception {
        // El token CSRF se genera al empezar cada petición y no al pintar el formulario: en una página larga,
        // como el login con su escaparate, la respuesta ya se está enviando cuando Thymeleaf llega al formulario
        // y entonces no se podría crear la sesión que guarda el token. Se mantiene la protección contra BREACH.
        XorCsrfTokenRequestAttributeHandler tokenCsrf = new XorCsrfTokenRequestAttributeHandler();
        tokenCsrf.setCsrfRequestAttributeName(null);
        http
                .csrf(csrf -> csrf.csrfTokenRequestHandler(tokenCsrf))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers("/css/**", "/img/**", "/js/**", "/webjars/**", "/error", "/actuator/health").permitAll()
                        // formLogin().permitAll() solo abre /login exacto, no /login?registrado
                        .requestMatchers("/login", "/registro").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Cualquier cuenta tiene su inicio y sus datos; préstamos, reservas y favoritos son del cliente
                        .requestMatchers("/inicio", "/user/cuenta", "/user/cuenta/password", "/sesion/renovar").authenticated()
                        .requestMatchers("/user/**").hasRole("USER")
                        // La portada y el catálogo se pueden consultar sin cuenta
                        .requestMatchers(HttpMethod.GET, "/", "/libros", "/libros/*", "/autores", "/autores/*").permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .formLogin(login -> login
                        .loginPage("/login")
                        .usernameParameter("email")
                        // Tras entrar vuelve a la página de la que venía o, si no, cada rol va a su inicio
                        .successHandler(new AccesoCorrecto(intentos))
                        .failureHandler((request, response, error) -> {
                            // Cada fallo cuenta; al llegar al máximo, el email queda bloqueado unos minutos
                            String email = request.getParameter("email");
                            boolean bloqueado = error instanceof LockedException || intentos.bloqueado(email)
                                    || intentos.registrarFallo(email);
                            response.sendRedirect(request.getContextPath() + (bloqueado ? "/login?bloqueado" : "/login?error"));
                        })
                        .permitAll())
                // «Recordarme»: una cookie firmada mantiene la sesión durante DIAS_RECORDARME días en ese equipo.
                // La clave de la firma sale de la configuración (variable BIBLIOTECA_RECORDARME_CLAVE); si no se da,
                // se inventa una al arrancar y las cookies dejan de valer al reiniciar, que es lo más prudente
                .rememberMe(recordar -> recordar
                        .key(claveRecordarme)
                        .rememberMeParameter("recordarme")
                        .rememberMeCookieName("recordarme")
                        .tokenValiditySeconds(DIAS_RECORDARME * 24 * 60 * 60)
                        .userDetailsService(cuentas))
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?salida")
                        .permitAll())
                .headers(cabeceras -> cabeceras
                        .contentSecurityPolicy(csp -> csp.policyDirectives(POLITICA_DE_CONTENIDO))
                        .referrerPolicy(referer -> referer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy",
                                "camera=(), microphone=(), geolocation=(), payment=()")));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
