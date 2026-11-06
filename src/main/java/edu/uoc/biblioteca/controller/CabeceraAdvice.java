package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.security.CuentaUsuario;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Arrays;
import java.util.Locale;

/**
 * Nombre e iniciales de quien tiene la sesión iniciada, para la cabecera de todas las páginas: ${sesion.nombre}.
 * Si la sesión no es una CuentaUsuario (por ejemplo, un usuario simulado en un test), se usa el email.
 */
@ControllerAdvice
public class CabeceraAdvice {

    @ModelAttribute("sesion")
    public Sesion sesion(Authentication autenticacion) {
        if (autenticacion == null || autenticacion instanceof AnonymousAuthenticationToken) {
            return null;
        }
        if (autenticacion.getPrincipal() instanceof CuentaUsuario cuenta) {
            return new Sesion(cuenta.getNombre(), cuenta.getIniciales());
        }
        String nombre = autenticacion.getName();
        return new Sesion(nombre, nombre.isEmpty() ? "?" : nombre.substring(0, 1).toUpperCase(Locale.ROOT));
    }

    public record Sesion(String nombre, String iniciales) {
    }

    /**
     * Página actual, para que «Iniciar sesión» vuelva a ella después: /login?volver=/libros/3.
     * Solo en peticiones GET (después de un POST no tiene sentido volver) y nunca desde el propio login.
     */
    @ModelAttribute("rutaActual")
    public String rutaActual(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        if (!"GET".equals(request.getMethod()) || "/".equals(ruta) || ruta.startsWith("/login")
                || ruta.startsWith("/registro") || ruta.startsWith("/error")) {
            return null;
        }
        return request.getQueryString() == null ? ruta : ruta + "?" + request.getQueryString();
    }

    /**
     * Segundos de inactividad que aguanta la sesión, para avisar antes de que caduque (js/app.js).
     * Con «Recordarme» no hace falta avisar: al caducar, la cookie vuelve a abrir la sesión sola.
     */
    @ModelAttribute("sesionCaduca")
    public Integer sesionCaduca(HttpServletRequest request, Authentication autenticacion) {
        if (autenticacion == null || autenticacion instanceof AnonymousAuthenticationToken
                || autenticacion instanceof RememberMeAuthenticationToken) {
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null && Arrays.stream(cookies).anyMatch(c -> "recordarme".equals(c.getName()))) {
            return null;
        }
        HttpSession sesion = request.getSession(false);
        return sesion == null || sesion.getMaxInactiveInterval() <= 0 ? null : sesion.getMaxInactiveInterval();
    }
}
