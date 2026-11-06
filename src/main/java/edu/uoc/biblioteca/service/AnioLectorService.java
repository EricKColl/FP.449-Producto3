package edu.uoc.biblioteca.service;

import edu.uoc.biblioteca.model.Autor;
import edu.uoc.biblioteca.model.Libro;
import edu.uoc.biblioteca.model.Prestamo;
import edu.uoc.biblioteca.model.Rol;
import edu.uoc.biblioteca.model.Usuario;
import edu.uoc.biblioteca.repository.PrestamoRepository;
import edu.uoc.biblioteca.repository.ReservaRepository;
import edu.uoc.biblioteca.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * «Tu año lector»: el resumen del año de un socio, al estilo de los resúmenes anuales de las apps de música.
 * Todo sale de sus préstamos del año en curso; no se guarda nada nuevo.
 */
@Service
@Transactional(readOnly = true)
public class AnioLectorService {

    private static final Locale ES = Locale.forLanguageTag("es");

    private final UsuarioService usuarioService;
    private final PrestamoRepository prestamoRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;

    public AnioLectorService(UsuarioService usuarioService, PrestamoRepository prestamoRepository,
                             ReservaRepository reservaRepository, UsuarioRepository usuarioRepository) {
        this.usuarioService = usuarioService;
        this.prestamoRepository = prestamoRepository;
        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Niveles por libros prestados en el año. Los nombres no tienen género: valen para cualquier socio. */
    public record Nivel(int numero, String nombre, String descripcion) {
    }

    /**
     * @param puntualidad porcentaje de devoluciones a tiempo, o -1 si todavía no ha devuelto ninguno
     * @param superaA     porcentaje de socios que han leído menos este año
     */
    public record AnioLector(int anio, Usuario usuario, List<Libro> libros, long dias, List<Cifra> generos,
                             Cifra autor, List<Cifra> meses, String mesFavorito, int puntualidad, long renovados,
                             long reservas, int favoritos, int superaA, Nivel nivel) {

        public int prestamos() {
            return libros.size();
        }

        public Cifra genero() {
            return generos.isEmpty() ? null : generos.getFirst();
        }

        /** Préstamos del mes con más préstamos, para dibujar las barras en proporción. */
        public long maximoMes() {
            return meses.stream().mapToLong(Cifra::valor).max().orElse(0);
        }

        public long semanas() {
            return Math.round(dias / 7.0);
        }
    }

    public AnioLector resumen(Long usuarioId) {
        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        LocalDate hoy = LocalDate.now();
        int anio = hoy.getYear();
        LocalDate inicio = LocalDate.of(anio, 1, 1);

        // Del más antiguo al más reciente: así las portadas salen en el orden en que se leyeron
        List<Prestamo> delAnio = prestamoRepository.findByUsuarioIdOrderByFechaPrestamoDescIdDesc(usuarioId).stream()
                .filter(p -> !p.getFechaPrestamo().isBefore(inicio))
                .sorted(Comparator.comparing(Prestamo::getFechaPrestamo).thenComparing(Prestamo::getId))
                .toList();

        long dias = delAnio.stream()
                .mapToLong(p -> Math.max(1, ChronoUnit.DAYS.between(p.getFechaPrestamo(),
                        p.isDevuelto() ? p.getFechaDevolucion() : hoy)))
                .sum();

        Map<String, Long> porGenero = new HashMap<>();
        Map<String, Long> porAutor = new HashMap<>();
        for (Prestamo p : delAnio) {
            Libro libro = p.getLibro();
            if (libro.getGenero() != null) {
                porGenero.merge(libro.getGenero(), 1L, Long::sum);
            }
            for (Autor autor : libro.getAutores()) {
                porAutor.merge(autor.getNombreCompleto(), 1L, Long::sum);
            }
        }
        List<Cifra> generos = ordenar(porGenero);
        List<Cifra> autores = ordenar(porAutor);

        Map<Month, Long> porMes = new LinkedHashMap<>();
        for (Month mes : Month.values()) {
            porMes.put(mes, 0L);
        }
        delAnio.forEach(p -> porMes.merge(p.getFechaPrestamo().getMonth(), 1L, Long::sum));
        List<Cifra> meses = new ArrayList<>();
        porMes.forEach((mes, n) -> meses.add(new Cifra(mes.getDisplayName(TextStyle.SHORT, ES).replace(".", ""), n)));
        String mesFavorito = porMes.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .max(Map.Entry.<Month, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                .map(e -> e.getKey().getDisplayName(TextStyle.FULL_STANDALONE, ES))
                .orElse(null);

        List<Prestamo> devueltos = delAnio.stream().filter(Prestamo::isDevuelto).toList();
        int puntualidad = devueltos.isEmpty() ? -1 : (int) Math.round(100.0
                * devueltos.stream().filter(p -> !p.getFechaDevolucion().isAfter(p.getFechaDevolucionPrevista())).count()
                / devueltos.size());

        long reservas = reservaRepository.findByUsuarioIdOrderByFechaReservaDescIdDesc(usuarioId).stream()
                .filter(r -> !r.getFechaReserva().isBefore(inicio))
                .count();

        return new AnioLector(anio, usuario, delAnio.stream().map(Prestamo::getLibro).toList(), dias, generos,
                autores.isEmpty() ? null : autores.getFirst(), meses, mesFavorito, puntualidad,
                delAnio.stream().filter(Prestamo::isRenovado).count(), reservas, usuario.getFavoritos().size(),
                superaA(usuarioId, delAnio.size(), inicio), nivel(delAnio.size()));
    }

    /** Porcentaje de socios con menos préstamos este año que este. */
    private int superaA(Long usuarioId, int propios, LocalDate inicio) {
        Map<Long, Long> porSocio = new HashMap<>();
        usuarioRepository.findAllByOrderByApellidosAscNombreAsc().stream()
                .filter(u -> u.getRol() == Rol.USER && !u.getId().equals(usuarioId))
                .forEach(u -> porSocio.put(u.getId(), 0L));
        prestamoRepository.findByFechaPrestamoGreaterThanEqual(inicio)
                .forEach(p -> porSocio.computeIfPresent(p.getUsuario().getId(), (id, n) -> n + 1));
        if (porSocio.isEmpty()) {
            return 100;
        }
        long menos = porSocio.values().stream().filter(n -> n < propios).count();
        return (int) Math.round(100.0 * menos / porSocio.size());
    }

    static Nivel nivel(int prestamos) {
        if (prestamos >= 10) {
            return new Nivel(5, "Leyenda de BackLord", "Tu nombre ya está escrito en la historia de la biblioteca.");
        }
        if (prestamos >= 6) {
            return new Nivel(4, "Saga épica", "Lo tuyo no es un libro: es una colección entera.");
        }
        if (prestamos >= 3) {
            return new Nivel(3, "Trama en marcha", "La historia se pone interesante y tú no la sueltas.");
        }
        if (prestamos >= 1) {
            return new Nivel(2, "Primer capítulo", "Ya has empezado. Lo mejor está en las páginas que vienen.");
        }
        return new Nivel(1, "Página en blanco", "Tu historia de este año todavía está por escribir.");
    }

    // De más a menos; a igualdad, por orden alfabético
    private static List<Cifra> ordenar(Map<String, Long> cuentas) {
        return cuentas.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(e -> new Cifra(e.getKey(), e.getValue()))
                .toList();
    }
}
