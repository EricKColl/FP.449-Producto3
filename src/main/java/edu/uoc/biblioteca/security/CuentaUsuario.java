package edu.uoc.biblioteca.security;

import edu.uoc.biblioteca.model.Usuario;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

/**
 * Usuario con la sesión iniciada. Añade el identificador para que las rutas /user/* trabajen siempre
 * con la cuenta de la sesión: @AuthenticationPrincipal CuentaUsuario cuenta.
 */
public class CuentaUsuario extends User {

    private final Long id;

    public CuentaUsuario(Usuario usuario) {
        super(usuario.getEmail(), usuario.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())));
        this.id = usuario.getId();
    }

    public Long getId() {
        return id;
    }
}
