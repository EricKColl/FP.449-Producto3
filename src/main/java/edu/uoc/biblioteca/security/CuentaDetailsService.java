package edu.uoc.biblioteca.security;

import edu.uoc.biblioteca.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/** Spring Security busca aquí la cuenta al iniciar sesión. El nombre de usuario es el email. */
@Service
public class CuentaDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CuentaDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return usuarioRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .map(CuentaUsuario::new)
                .orElseThrow(() -> new UsernameNotFoundException("No existe la cuenta " + email));
    }
}
