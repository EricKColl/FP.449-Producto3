package edu.uoc.biblioteca.service;

import edu.uoc.biblioteca.model.EstadoReserva;
import edu.uoc.biblioteca.model.Libro;
import edu.uoc.biblioteca.model.Prestamo;
import edu.uoc.biblioteca.model.Reserva;
import edu.uoc.biblioteca.model.Rol;
import edu.uoc.biblioteca.model.Usuario;
import edu.uoc.biblioteca.repository.AutorRepository;
import edu.uoc.biblioteca.repository.LibroRepository;
import edu.uoc.biblioteca.repository.PrestamoRepository;
import edu.uoc.biblioteca.repository.ReservaRepository;
import edu.uoc.biblioteca.repository.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Panel del administrador: todo lo que pasa en la biblioteca, en cifras, gráficas y tablas de trabajo. */
@Service
@Transactional(readOnly = true)
public class PanelService {

    /** Meses que muestra la gráfica de actividad. */
    static final int MESES_ACTIVIDAD = 6;

    private final LibroRepository libroRepository;
    private final AutorRepository autorRepository;
    private final UsuarioRepository usuarioRepository;
    private final PrestamoRepository prestamoRepository;
    private final ReservaRepository reservaRepository;
    private final LibroService libroService;

    public PanelService(LibroRepository libroRepository, AutorRepository autorRepository,
                        UsuarioRepository usuarioRepository, PrestamoRepository prestamoRepository,
                        ReservaRepository reservaRepository, LibroService libroService) {
        this.libroRepository = libroRepository;
        this.autorRepository = autorRepository;
        this.usuarioRepository = usuarioRepository;
        this.prestamoRepository = prestamoRepository;
        this.reservaRepository = reservaRepository;
        this.libroService = libroService;
    }

    public Panel obtenerPanel() {
        LocalDate hoy = LocalDate.now();
        long ejemplares = libroRepository.sumarEjemplares();
        long abiertos = prestamoRepository.countByFechaDevolucionIsNull();
        long vencidos = prestamoRepository.countByFechaDevolucionIsNullAndFechaDevolucionPrevistaBefore(hoy);
        long devueltos = prestamoRepository.countByFechaDevolucionIsNotNull();

        Indicadores indicadores = new Indicadores(
                libroRepository.count(), ejemplares, abiertos, autorRepository.count(),
                usuarioRepository.countByRol(Rol.USER),
                usuarioRepository.countByRolAndFechaAltaGreaterThanEqual(Rol.USER, hoy.minusDays(30)),
                abiertos - vencidos, vencidos, devueltos,
                prestamoRepository.contarDevueltosEnPlazo(),
                reservaRepository.countByEstado(EstadoReserva.ACTIVA));

        List<LibroDisponible> agotados = libroRepository.findAllByOrderByTituloAsc().stream()
                .map(libro -> new LibroDisponible(libro, libroService.ejemplaresDisponibles(libro)))
                .filter(libro -> !libro.prestable())
                .toList();

        return new Panel(indicadores, actividad(hoy), libroRepository.contarPorGenero(),
                prestamoRepository.contarPorLibro(PageRequest.of(0, 5)),
                prestamoRepository.findByFechaDevolucionIsNullAndFechaDevolucionPrevistaBeforeOrderByFechaDevolucionPrevistaAsc(hoy),
                colaDeReservas(), agotados,
                prestamoRepository.findTop5ByOrderByFechaPrestamoDescIdDesc(),
                usuarioRepository.findTop5ByRolOrderByFechaAltaDescIdDesc(Rol.USER));
    }

    // Préstamos y devoluciones de cada uno de los últimos meses, incluido el actual
    private List<Mes> actividad(LocalDate hoy) {
        YearMonth actual = YearMonth.from(hoy);
        LocalDate desde = actual.minusMonths(MESES_ACTIVIDAD - 1L).atDay(1);
        Map<YearMonth, Integer> prestados = new HashMap<>();
        Map<YearMonth, Integer> devueltos = new HashMap<>();
        prestamoRepository.findByFechaPrestamoGreaterThanEqual(desde)
                .forEach(p -> prestados.merge(YearMonth.from(p.getFechaPrestamo()), 1, Integer::sum));
        prestamoRepository.findByFechaDevolucionGreaterThanEqual(desde)
                .forEach(p -> devueltos.merge(YearMonth.from(p.getFechaDevolucion()), 1, Integer::sum));
        List<Mes> meses = new ArrayList<>();
        for (int i = MESES_ACTIVIDAD - 1; i >= 0; i--) {
            YearMonth mes = actual.minusMonths(i);
            String nombre = mes.getMonth().getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es"));
            meses.add(new Mes(nombre.replace(".", ""), prestados.getOrDefault(mes, 0), devueltos.getOrDefault(mes, 0)));
        }
        return meses;
    }

    // Reservas activas con su puesto en la cola de cada libro y si ya hay un ejemplar para el primero
    private List<ReservaEnCola> colaDeReservas() {
        Map<Long, Integer> puestos = new HashMap<>();
        List<ReservaEnCola> cola = new ArrayList<>();
        for (Reserva reserva : reservaRepository.findByEstadoOrderByFechaReservaAscIdAsc(EstadoReserva.ACTIVA)) {
            Libro libro = reserva.getLibro();
            int puesto = puestos.merge(libro.getId(), 1, Integer::sum);
            cola.add(new ReservaEnCola(reserva, puesto, libroService.ejemplaresDisponibles(libro) >= puesto));
        }
        return cola;
    }

    public record Panel(Indicadores indicadores, List<Mes> actividad, List<Cifra> generos, List<Cifra> masPrestados,
                        List<Prestamo> vencidos, List<ReservaEnCola> reservas, List<LibroDisponible> agotados,
                        List<Prestamo> ultimosPrestamos, List<Usuario> ultimosClientes) {
    }

    public record Mes(String nombre, int prestamos, int devoluciones) {
    }

    public record ReservaEnCola(Reserva reserva, int puesto, boolean listaParaPrestar) {
    }

    /** Cifras del panel y los porcentajes que se calculan con ellas. */
    public record Indicadores(long titulos, long ejemplares, long ejemplaresPrestados, long autores,
                              long clientes, long clientesNuevos, long activos, long vencidos, long devueltos,
                              long devueltosEnPlazo, long reservasActivas) {

        public long ejemplaresLibres() {
            return ejemplares - ejemplaresPrestados;
        }

        /** Parte de los ejemplares que están prestados ahora mismo. */
        public int ocupacion() {
            return porcentaje(ejemplaresPrestados, ejemplares);
        }

        /** Parte de los préstamos sin devolver que ya están fuera de plazo. */
        public int porcentajeVencidos() {
            return porcentaje(vencidos, activos + vencidos);
        }

        /** Parte de las devoluciones que llegaron a tiempo. */
        public int puntualidad() {
            return porcentaje(devueltosEnPlazo, devueltos);
        }

        public long prestamosTotales() {
            return activos + vencidos + devueltos;
        }

        public int porcentajeDe(long cantidad) {
            return porcentaje(cantidad, prestamosTotales());
        }

        private static int porcentaje(long parte, long total) {
            return total == 0 ? 0 : (int) Math.round(parte * 100.0 / total);
        }
    }
}
