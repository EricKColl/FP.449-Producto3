package edu.uoc.biblioteca.service;

import edu.uoc.biblioteca.model.Libro;
import edu.uoc.biblioteca.repository.AutorRepository;
import edu.uoc.biblioteca.repository.LibroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Sugerencias del buscador mientras se escribe: libros por título, autor, género o ISBN y autores por nombre.
 * No distingue mayúsculas ni tildes («garcia» encuentra «García»). Primero lo que empieza por lo escrito.
 */
@Service
@Transactional(readOnly = true)
public class BusquedaService {

    /** Mínimo de letras para empezar a sugerir. */
    public static final int MINIMO = 2;
    static final int MAX_LIBROS = 6;
    static final int MAX_AUTORES = 3;

    private static final Pattern MARCAS = Pattern.compile("\\p{M}+");

    private final LibroRepository libroRepository;
    private final AutorRepository autorRepository;
    private final LibroService libroService;

    public BusquedaService(LibroRepository libroRepository, AutorRepository autorRepository, LibroService libroService) {
        this.libroRepository = libroRepository;
        this.autorRepository = autorRepository;
        this.libroService = libroService;
    }

    public record Sugerencias(List<LibroSugerido> libros, List<AutorSugerido> autores) {
    }

    /** Lo justo para pintar la sugerencia: el tono de la portada (0-5) es el mismo que en el catálogo. */
    public record LibroSugerido(long id, String titulo, String autores, Integer anio, long disponibles, int tono) {
    }

    public record AutorSugerido(long id, String nombre, String nacionalidad, int libros) {
    }

    public Sugerencias sugerir(String texto) {
        String buscado = normalizar(texto);
        if (buscado.length() < MINIMO) {
            return new Sugerencias(List.of(), List.of());
        }
        List<LibroSugerido> libros = libroRepository.findAllByOrderByTituloAsc().stream()
                .filter(libro -> coincide(libro, buscado))
                .sorted(Comparator.comparing((Libro libro) -> !normalizar(libro.getTitulo()).startsWith(buscado)))
                .limit(MAX_LIBROS)
                .map(libro -> new LibroSugerido(libro.getId(), libro.getTitulo(), libro.getAutoresTexto(),
                        libro.getAnioPublicacion(), libroService.ejemplaresDisponibles(libro), (int) (libro.getId() % 6)))
                .toList();
        List<AutorSugerido> autores = autorRepository.findAllByOrderByApellidosAscNombreAsc().stream()
                .filter(autor -> normalizar(autor.getNombreCompleto()).contains(buscado))
                .limit(MAX_AUTORES)
                .map(autor -> new AutorSugerido(autor.getId(), autor.getNombreCompleto(), autor.getNacionalidad(),
                        autor.getLibros().size()))
                .toList();
        return new Sugerencias(libros, autores);
    }

    private static boolean coincide(Libro libro, String buscado) {
        return normalizar(libro.getTitulo()).contains(buscado)
                || normalizar(libro.getAutoresTexto()).contains(buscado)
                || normalizar(libro.getGenero()).contains(buscado)
                || normalizar(libro.getIsbn()).replace("-", "").contains(buscado.replace("-", ""));
    }

    /** Minúsculas y sin tildes ni diéresis: «Pérez» y «perez» se comparan igual. */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinMarcas = MARCAS.matcher(Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)).replaceAll("");
        return sinMarcas.toLowerCase(Locale.ROOT);
    }
}
