package edu.uoc.biblioteca.service;
import edu.uoc.biblioteca.model.Libro;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
/** Libros favoritos de cada usuario: la relación N:M usuario_favorito. */
@Service
@Transactional(readOnly = true)
public class FavoritoService {
    private final UsuarioService usuarioService;
    private final LibroService libroService;
    public FavoritoService(UsuarioService usuarioService, LibroService libroService) {
        this.usuarioService = usuarioService;
        this.libroService = libroService;
    }
    public Set<Libro> listar(Long usuarioId) {
        return usuarioService.buscarPorId(usuarioId).getFavoritos();
    }
    public boolean esFavorito(Long usuarioId, Long libroId) {
        return listar(usuarioId).contains(libroService.buscarPorId(libroId));
    }
    @Transactional
    public Libro marcar(Long usuarioId, Long libroId) {
        Libro libro = libroService.buscarPorId(libroId);
        usuarioService.buscarPorId(usuarioId).getFavoritos().add(libro);
        return libro;
    }
    @Transactional
    public Libro quitar(Long usuarioId, Long libroId) {
        Libro libro = libroService.buscarPorId(libroId);
        usuarioService.buscarPorId(usuarioId).getFavoritos().remove(libro);
        return libro;
    }
}