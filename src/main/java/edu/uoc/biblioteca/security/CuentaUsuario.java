package edu.uoc.biblioteca.security;

import edu.uoc.biblioteca.model.Usuario;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

/**
 * Usuario con la sesión iniciada. Añade el identificador para que las rutas /user/* trabajen siempre
 * con la cuenta de la sesión: @AuthenticationPrincipal CuentaUsuario cuenta.
 * El nombre y las iniciales son para la cabecera: sec:authentication="principal.nombre".
 */
public class CuentaUsuario extends User {

    private final Long id;
    private final String nombre;
    private final String iniciales;

    public CuentaUsuario(Usuario usuario) {
        this(usuario, false);
    }

    /** Con {@code bloqueada}, Spring Security rechaza el acceso aunque la contraseña sea correcta (IntentosDeAcceso). */
    public CuentaUsuario(Usuario usuario, boolean bloqueada) {
        super(usuario.getEmail(), usuario.getPassword(), true, true, true, !bloqueada,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())));
        this.id = usuario.getId();
        this.nombre = usuario.getNombreCompleto();
        this.iniciales = inicial(usuario.getNombre()) + inicial(usuario.getApellidos());
    }

    private static String inicial(String texto) {
        return texto == null || texto.isBlank() ? "" : texto.strip().substring(0, 1).toUpperCase();
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getIniciales() {
        return iniciales;
    }
}
