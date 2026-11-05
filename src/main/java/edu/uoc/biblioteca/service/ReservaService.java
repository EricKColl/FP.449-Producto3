package edu.uoc.biblioteca.service;
import edu.uoc.biblioteca.model.EstadoReserva;
import edu.uoc.biblioteca.model.Libro;
import edu.uoc.biblioteca.model.Reserva;
import edu.uoc.biblioteca.model.Usuario;
import edu.uoc.biblioteca.repository.ReservaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
@Service
@Transactional(readOnly = true)
public class ReservaService {
    private final ReservaRepository reservaRepository;
    private final UsuarioService usuarioService;
    private final LibroService libroService;
    public ReservaService(ReservaRepository reservaRepository, UsuarioService usuarioService, LibroService
            libroService) {
        this.reservaRepository = reservaRepository;
        this.usuarioService = usuarioService;
        this.libroService = libroService;
    }
    public List<Reserva> listarDeUsuario(Long usuarioId) {
        return reservaRepository.findByUsuarioIdOrderByFechaReservaDescIdDesc(usuarioId);
    }
    /** Solo se reserva lo que no se puede pedir prestado, y una sola vez por usuario. */
    @Transactional
    public Reserva reservar(Long usuarioId, Long libroId) {
        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        Libro libro = libroService.buscarPorId(libroId);
        if (libroService.ejemplaresDisponibles(libro) > 0) {
            throw new IllegalStateException("«" + libro.getTitulo() + "» tiene ejemplares disponibles: puedes pedirlo prestado");
        }
        if (reservaRepository.existsByUsuarioIdAndLibroIdAndEstado(usuarioId, libroId,
                EstadoReserva.ACTIVA)) {
            throw new IllegalStateException("Ya tienes una reserva activa de «" + libro.getTitulo() + "»");
        }
        return reservaRepository.save(new Reserva(usuario, libro));
    }
    /** Busca por id y por usuario: la reserva de otro se comporta como si no existiera (404). */
    @Transactional
    public Reserva cancelar(Long id, Long usuarioId) {
        Reserva reserva = reservaRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la reserva " + id));
        if (!reserva.isActiva()) {
            throw new IllegalStateException("Esa reserva ya estaba cancelada");
        }
        reserva.cancelar();
        return reserva;
    }
}