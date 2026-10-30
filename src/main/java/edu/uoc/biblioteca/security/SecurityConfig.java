package edu.uoc.biblioteca.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Control de acceso de toda la aplicación. Lo que no se abre aquí expresamente queda reservado a ADMIN,
 * así que una ruta nueva nunca nace pública por descuido.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filtroDeSeguridad(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers("/css/**", "/img/**", "/webjars/**", "/error", "/actuator/health").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/user/**").authenticated()
                        // Formularios de escritura que aún no están en /admin (se mueven en el turno 3)
                        .requestMatchers(HttpMethod.GET, "/libros/nuevo", "/libros/*/editar",
                                "/autores/nuevo", "/autores/*/editar").hasRole("ADMIN")
                        // El catálogo se puede consultar sin cuenta
                        .requestMatchers(HttpMethod.GET, "/", "/libros", "/libros/*", "/autores", "/autores/*").permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .formLogin(login -> login
                        .loginPage("/login")
                        .usernameParameter("email")
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?salida")
                        .permitAll());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
