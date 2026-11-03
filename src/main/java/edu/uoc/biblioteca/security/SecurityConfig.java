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
                        // formLogin().permitAll() solo abre /login exacto, no /login?registrado
                        .requestMatchers("/login", "/registro").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/user/**").authenticated()
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
