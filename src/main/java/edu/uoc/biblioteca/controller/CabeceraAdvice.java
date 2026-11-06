package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.security.CuentaUsuario;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

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
}
