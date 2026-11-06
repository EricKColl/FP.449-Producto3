package edu.uoc.biblioteca.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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

    @Bean
    SecurityFilterChain filtroDeSeguridad(HttpSecurity http) throws Exception {
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
                        .requestMatchers("/inicio", "/user/cuenta").authenticated()
                        .requestMatchers("/user/**").hasRole("USER")
                        // La portada y el catálogo se pueden consultar sin cuenta
                        .requestMatchers(HttpMethod.GET, "/", "/libros", "/libros/*", "/autores", "/autores/*").permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .formLogin(login -> login
                        .loginPage("/login")
                        .usernameParameter("email")
                        // Tras entrar, cada rol va a su inicio; si venía de una página protegida, vuelve a ella
                        .defaultSuccessUrl("/inicio")
                        .permitAll())
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
