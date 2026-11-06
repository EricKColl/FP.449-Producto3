package edu.uoc.biblioteca.service;

import edu.uoc.biblioteca.model.EstadoPrestamo;
import edu.uoc.biblioteca.model.EstadoReserva;
import edu.uoc.biblioteca.model.Libro;
import edu.uoc.biblioteca.model.Prestamo;
import edu.uoc.biblioteca.model.Reserva;
import edu.uoc.biblioteca.model.Rol;
import edu.uoc.biblioteca.model.Usuario;
import edu.uoc.biblioteca.repository.LibroRepository;
import edu.uoc.biblioteca.repository.PrestamoRepository;
import edu.uoc.biblioteca.repository.ReservaRepository;
import edu.uoc.biblioteca.repository.UsuarioRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PrestamoService {

    /** Préstamos sin devolver que puede tener a la vez un mismo usuario. */
    public static final int MAX_PRESTAMOS_ABIERTOS = 3;

    private final PrestamoRepository prestamoRepository;
    private final LibroRepository libroRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;
    private final ApplicationEventPublisher eventos;

    public PrestamoService(PrestamoRepository prestamoRepository,
                           LibroRepository libroRepository,
                           UsuarioRepository usuarioRepository,
                           ReservaRepository reservaRepository,
                           ApplicationEventPublisher eventos) {
        this.eventos = eventos;
        this.prestamoRepository = prestamoRepository;
        this.libroRepository = libroRepository;
        this.usuarioRepository = usuarioRepository;
        this.reservaRepository = reservaRepository;
    }

    /** Todos los préstamos (del más reciente al más antiguo) o solo los de un estado. */
    public List<Prestamo> listar(EstadoPrestamo estado) {
        List<Prestamo> prestamos = prestamoRepository.findAllByOrderByFechaPrestamoDesc();
        if (estado == null) {
            return prestamos;
        }
        // El estado no está en la base de datos (se calcula con las fechas), por eso se filtra aquí
        return prestamos.stream()
                .filter(prestamo -> prestamo.getEstado() == estado)
                .toList();
    }

    /** Los préstamos de un usuario, del más reciente al más antiguo. */
    public List<Prestamo> listarDeUsuario(Long usuarioId) {
        return prestamoRepository.findByUsuarioIdOrderByFechaPrestamoDescIdDesc(usuarioId);
    }

    /** Historial de un libro, del préstamo más reciente al más antiguo. */
    public List<Prestamo> historialDeLibro(Long libroId) {
        return prestamoRepository.findByLibroIdOrderByFechaPrestamoDescIdDesc(libroId);
    }

    public boolean tieneSinDevolver(Long usuarioId, Long libroId) {
        return prestamoRepository.existsByUsuarioIdAndLibroIdAndFechaDevolucionIsNull(usuarioId, libroId);
    }

    public Prestamo buscarPorId(Long id) {
        return prestamoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el préstamo " + id));
    }

    /** Usuarios para el formulario de préstamo, indicando quién no puede llevarse otro libro y por qué. */
    public List<UsuarioPrestatario> listarUsuariosPrestatarios() {
        return usuarioRepository.findAllByOrderByApellidosAscNombreAsc().stream()
                .filter(usuario -> usuario.getRol() == Rol.USER)
                .map(this::comprobarUsuario)
                .toList();
    }

    /** Catálogo para el formulario de préstamo, con los ejemplares libres de cada libro. */
    public List<LibroDisponible> listarLibrosConDisponibles() {
        return libroRepository.findAllByOrderByTituloAsc().stream()
                .map(libro -> new LibroDisponible(libro, ejemplaresDisponibles(libro)))
                .toList();
    }

    @Transactional
    public Prestamo crear(Long usuarioId, Long libroId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el usuario " + usuarioId));
        Libro libro = libroRepository.findById(libroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el libro " + libroId));
        if (usuario.getRol() != Rol.USER) {
            throw new IllegalStateException("Las cuentas de administración no piden préstamos");
        }
        UsuarioPrestatario prestatario = comprobarUsuario(usuario);
        if (!prestatario.puedePedir()) {
            throw new IllegalStateException(usuario.getNombreCompleto() + " no puede llevarse otro libro: "
                    + prestatario.motivo());
        }
        if (tieneSinDevolver(usuarioId, libroId)) {
            throw new IllegalStateException(usuario.getNombreCompleto() + " ya tiene «" + libro.getTitulo()
                    + "» sin devolver");
        }
        if (ejemplaresDisponibles(libro) <= 0) {
            throw new IllegalStateException(
                    "No quedan ejemplares disponibles de «" + libro.getTitulo() + "»");
        }
        if (ejemplaresParaUsuario(libro, usuarioId) <= 0) {
            throw new IllegalStateException("Los ejemplares libres de «" + libro.getTitulo()
                    + "» están apartados para quien lo reservó antes");
        }
        // Si lo tenía reservado, la reserva queda atendida: se cierra y sale de la cola
        reservaRepository.findFirstByUsuarioIdAndLibroIdAndEstado(usuarioId, libroId, EstadoReserva.ACTIVA)
                .ifPresent(Reserva::cancelar);
        Prestamo prestamo = prestamoRepository.save(new Prestamo(usuario, libro));
        eventos.publishEvent(new AvisosCambiados(libroId, usuarioId));
        return prestamo;
    }

    /**
     * Ejemplares que puede llevarse este usuario. Los libres se reparten por orden de reserva: primero quien
     * reservó antes. Quien está el N.º en la cola necesita N ejemplares libres; quien no lo ha reservado,
     * que sobre alguno después de atender a toda la cola.
     */
    public long ejemplaresParaUsuario(Libro libro, Long usuarioId) {
        List<Reserva> cola = colaDe(libro.getId());
        int delante = cola.size();
        for (int i = 0; i < cola.size(); i++) {
            if (cola.get(i).getUsuario().getId().equals(usuarioId)) {
                delante = i;
                break;
            }
        }
        return ejemplaresDisponibles(libro) - delante;
    }

    /** Puesto (1, 2, 3...) de una reserva activa en la cola de su libro; 0 si no está en la cola. */
    public int puestoEnCola(Reserva reserva) {
        List<Reserva> cola = colaDe(reserva.getLibro().getId());
        for (int i = 0; i < cola.size(); i++) {
            if (cola.get(i).getId().equals(reserva.getId())) {
                return i + 1;
            }
        }
        return 0;
    }

    private List<Reserva> colaDe(Long libroId) {
        return reservaRepository.findByLibroIdAndEstadoOrderByFechaReservaAscIdAsc(libroId, EstadoReserva.ACTIVA);
    }

    /** Días que añade cada renovación (una por préstamo). */
    public static final int DIAS_RENOVACION = Prestamo.DIAS_PRESTAMO;

    /**
     * Por qué no se puede renovar un préstamo, o null si se puede. Solo una vez, dentro de plazo y si nadie
     * espera ese libro: con reservas en cola, el ejemplar tiene que volver a tiempo.
     */
    public String motivoNoRenovable(Prestamo prestamo) {
        if (prestamo.isDevuelto()) {
            return "Ya está devuelto";
        }
        if (prestamo.getDiasRetraso() > 0) {
            return "Está fuera de plazo: hay que devolverlo";
        }
        if (prestamo.isRenovado()) {
            return "Ya se renovó una vez";
        }
        if (!colaDe(prestamo.getLibro().getId()).isEmpty()) {
            return "Otros socios esperan este libro";
        }
        return null;
    }

    /** El socio alarga DIAS_RENOVACION días un préstamo suyo. El préstamo de otro se comporta como si no existiera. */
    @Transactional
    public Prestamo renovar(Long id, Long usuarioId) {
        Prestamo prestamo = buscarPorId(id);
        if (!prestamo.getUsuario().getId().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el préstamo " + id);
        }
        String motivo = motivoNoRenovable(prestamo);
        if (motivo != null) {
            throw new IllegalStateException("No se puede renovar «" + prestamo.getLibro().getTitulo() + "»: "
                    + motivo.substring(0, 1).toLowerCase() + motivo.substring(1));
        }
        prestamo.setFechaDevolucionPrevista(prestamo.getFechaDevolucionPrevista().plusDays(DIAS_RENOVACION));
        eventos.publishEvent(new AvisosCambiados(prestamo.getLibro().getId(), usuarioId));
        return prestamo;
    }

    @Transactional
    public Prestamo registrarDevolucion(Long id) {
        Prestamo prestamo = buscarPorId(id);
        if (prestamo.isDevuelto()) {
            throw new IllegalStateException("Ese préstamo ya estaba devuelto");
        }
        prestamo.setFechaDevolucion(LocalDate.now());
        // Quien lo tenía deja de tener el aviso; a quien lo reservó le puede llegar «Ya puedes pedirlo»
        eventos.publishEvent(new AvisosCambiados(prestamo.getLibro().getId(), prestamo.getUsuario().getId()));
        return prestamo;
    }

    // Reglas de la biblioteca: nada nuevo con préstamos vencidos y como mucho MAX_PRESTAMOS_ABIERTOS a la vez
    private UsuarioPrestatario comprobarUsuario(Usuario usuario) {
        long abiertos = prestamoRepository.countByUsuarioIdAndFechaDevolucionIsNull(usuario.getId());
        String motivo = null;
        if (prestamoRepository.existsByUsuarioIdAndFechaDevolucionIsNullAndFechaDevolucionPrevistaBefore(
                usuario.getId(), LocalDate.now())) {
            motivo = "tiene préstamos vencidos";
        } else if (abiertos >= MAX_PRESTAMOS_ABIERTOS) {
            motivo = "ya tiene " + MAX_PRESTAMOS_ABIERTOS + " préstamos sin devolver";
        }
        return new UsuarioPrestatario(usuario, abiertos, motivo);
    }

    long ejemplaresDisponibles(Libro libro) {
        return libro.getEjemplares()
                - prestamoRepository.countByLibroIdAndFechaDevolucionIsNull(libro.getId());
    }
}
