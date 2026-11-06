package edu.uoc.biblioteca.service;

import edu.uoc.biblioteca.model.EstadoReserva;
import edu.uoc.biblioteca.model.Libro;
import edu.uoc.biblioteca.model.Prestamo;
import edu.uoc.biblioteca.model.Reserva;
import edu.uoc.biblioteca.model.Usuario;
import edu.uoc.biblioteca.repository.LibroRepository;
import edu.uoc.biblioteca.repository.PrestamoRepository;
import edu.uoc.biblioteca.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/** Inicio del cliente: sus préstamos, reservas y favoritos, y libros que le pueden interesar. */
@Service
@Transactional(readOnly = true)
public class InicioClienteService {

    /** Libros que se recomiendan en el inicio. */
    static final int RECOMENDADOS = 4;

    private final UsuarioService usuarioService;
    private final LibroService libroService;
    private final LibroRepository libroRepository;
    private final PrestamoRepository prestamoRepository;
    private final ReservaRepository reservaRepository;

    public InicioClienteService(UsuarioService usuarioService, LibroService libroService, LibroRepository libroRepository,
                                PrestamoRepository prestamoRepository, ReservaRepository reservaRepository) {
        this.usuarioService = usuarioService;
        this.libroService = libroService;
        this.libroRepository = libroRepository;
        this.prestamoRepository = prestamoRepository;
        this.reservaRepository = reservaRepository;
    }

    public InicioCliente obtener(Long usuarioId) {
        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        List<Prestamo> enCurso = prestamoRepository.findByUsuarioIdAndFechaDevolucionIsNullOrderByFechaDevolucionPrevistaAsc(usuarioId);
        List<ReservaActiva> reservas = reservaRepository.findByUsuarioIdOrderByFechaReservaDescIdDesc(usuarioId).stream()
                .filter(Reserva::isActiva)
                .map(reserva -> new ReservaActiva(reserva, libroService.ejemplaresDisponibles(reserva.getLibro()) > 0))
                .toList();
        List<LibroDisponible> favoritos = usuario.getFavoritos().stream().map(this::conDisponibles).toList();
        boolean bloqueado = prestamoRepository.existsByUsuarioIdAndFechaDevolucionIsNullAndFechaDevolucionPrevistaBefore(
                usuarioId, LocalDate.now());
        int libres = Math.max(0, PrestamoService.MAX_PRESTAMOS_ABIERTOS - enCurso.size());
        return new InicioCliente(usuario, enCurso, reservas, favoritos, recomendados(usuario, enCurso),
                prestamoRepository.countByUsuarioId(usuarioId), bloqueado ? 0 : libres, bloqueado);
    }

    // Libros disponibles de los géneros que ya lee o ha marcado, que todavía no tiene ni son favoritos
    private List<LibroDisponible> recomendados(Usuario usuario, List<Prestamo> enCurso) {
        Set<Libro> suyos = new HashSet<>(usuario.getFavoritos());
        enCurso.forEach(prestamo -> suyos.add(prestamo.getLibro()));
        Set<String> generos = new HashSet<>();
        Stream.concat(usuario.getFavoritos().stream(),
                        prestamoRepository.findByUsuarioIdOrderByFechaPrestamoDescIdDesc(usuario.getId()).stream()
                                .map(Prestamo::getLibro))
                .map(Libro::getGenero).filter(Objects::nonNull).forEach(generos::add);
        List<LibroDisponible> candidatos = libroRepository.findAllByOrderByTituloAsc().stream()
                .filter(libro -> !suyos.contains(libro))
                .map(this::conDisponibles)
                .filter(LibroDisponible::prestable)
                .toList();
        // Primero los de sus géneros; si no llegan, se completa con el resto del catálogo disponible
        return Stream.concat(
                        candidatos.stream().filter(l -> generos.contains(l.libro().getGenero())),
                        candidatos.stream().filter(l -> !generos.contains(l.libro().getGenero())))
                .limit(RECOMENDADOS)
                .toList();
    }

    private LibroDisponible conDisponibles(Libro libro) {
        return new LibroDisponible(libro, libroService.ejemplaresDisponibles(libro));
    }

    public record InicioCliente(Usuario usuario, List<Prestamo> enCurso, List<ReservaActiva> reservas,
                                List<LibroDisponible> favoritos, List<LibroDisponible> recomendados,
                                long prestamosTotales, int prestamosLibres, boolean bloqueado) {

        public int maxPrestamos() {
            return PrestamoService.MAX_PRESTAMOS_ABIERTOS;
        }

        public long reservasListas() {
            return reservas.stream().filter(ReservaActiva::disponible).count();
        }
    }

    /** Reserva activa y si ya hay un ejemplar libre para pedirlo. */
    public record ReservaActiva(Reserva reserva, boolean disponible) {

        public EstadoReserva estado() {
            return reserva.getEstado();
        }
    }
}
