package edu.uoc.biblioteca.service;

import edu.uoc.biblioteca.model.Rol;
import edu.uoc.biblioteca.model.Usuario;
import edu.uoc.biblioteca.repository.PrestamoRepository;
import edu.uoc.biblioteca.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PrestamoRepository prestamoRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PrestamoRepository prestamoRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.prestamoRepository = prestamoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listar() {
        return usuarioRepository.findAllByOrderByApellidosAscNombreAsc();
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el usuario " + id));
    }

    public boolean dniRepetido(Usuario usuario) {
        return otroUsuario(usuarioRepository.findByDni(normalizarDni(usuario.getDni())), usuario);
    }

    public boolean emailRepetido(Usuario usuario) {
        return otroUsuario(usuarioRepository.findByEmail(normalizarEmail(usuario.getEmail())), usuario);
    }

    /** Quitarle el rol al único administrador dejaría la aplicación sin nadie que pueda gestionarla. */
    public boolean esUltimoAdmin(Long id) {
        return buscarPorId(id).getRol() == Rol.ADMIN && usuarioRepository.countByRol(Rol.ADMIN) == 1;
    }

    @Transactional
    public Usuario crear(Usuario usuario) {
        usuario.setId(null);
        normalizar(usuario);
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario actualizar(Long id, Usuario datos) {
        Usuario usuario = buscarPorId(id);
        usuario.setDni(datos.getDni());
        usuario.setNombre(datos.getNombre());
        usuario.setApellidos(datos.getApellidos());
        usuario.setEmail(datos.getEmail());
        usuario.setTelefono(datos.getTelefono());
        usuario.setFechaAlta(datos.getFechaAlta());
        usuario.setRol(datos.getRol());
        // Si la contraseña se deja en blanco al editar, se conserva la que tenía
        if (datos.getPassword() != null && !datos.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(datos.getPassword()));
        }
        normalizar(usuario);
        return usuario;
    }

    /** Un usuario con préstamos no se borra, para conservar el historial (la clave ajena es ON DELETE RESTRICT). */
    @Transactional
    public Usuario eliminar(Long id, String emailSesion) {
        Usuario usuario = buscarPorId(id);
        if (usuario.getEmail().equals(emailSesion)) {
            throw new IllegalStateException("No puedes eliminar la cuenta con la que has iniciado sesión");
        }
        if (prestamoRepository.existsByUsuarioId(id)) {
            throw new IllegalStateException("No se puede eliminar a " + usuario.getNombreCompleto()
                    + " porque tiene préstamos registrados");
        }
        usuarioRepository.delete(usuario);
        return usuario;
    }

    private boolean otroUsuario(Optional<Usuario> encontrado, Usuario usuario) {
        return encontrado.filter(otro -> !otro.getId().equals(usuario.getId())).isPresent();
    }

    // El DNI se guarda con la letra en mayúscula y el email en minúsculas, así no se cuelan duplicados
    private void normalizar(Usuario usuario) {
        usuario.setDni(normalizarDni(usuario.getDni()));
        usuario.setEmail(normalizarEmail(usuario.getEmail()));
        if (usuario.getTelefono() != null && usuario.getTelefono().isBlank()) {
            usuario.setTelefono(null);
        }
    }

    private static String normalizarDni(String dni) {
        return dni == null ? null : dni.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
