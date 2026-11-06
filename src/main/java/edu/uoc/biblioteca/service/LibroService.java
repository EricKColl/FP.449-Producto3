package edu.uoc.biblioteca.service;

import edu.uoc.biblioteca.model.Autor;
import edu.uoc.biblioteca.model.Libro;
import edu.uoc.biblioteca.repository.AutorRepository;
import edu.uoc.biblioteca.repository.LibroRepository;
import edu.uoc.biblioteca.repository.PrestamoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class LibroService {

    private final LibroRepository libroRepository;
    private final AutorRepository autorRepository;
    private final PrestamoRepository prestamoRepository;

    public LibroService(LibroRepository libroRepository, AutorRepository autorRepository,
                        PrestamoRepository prestamoRepository) {
        this.libroRepository = libroRepository;
        this.autorRepository = autorRepository;
        this.prestamoRepository = prestamoRepository;
    }

    public List<Libro> buscar(String texto) {
        List<Libro> libros = libroRepository.findAllByOrderByTituloAsc();
        if (texto == null || texto.isBlank()) {
            return libros;
        }
        String buscado = texto.trim().toLowerCase(Locale.ROOT);
        return libros.stream()
                .filter(libro -> libro.getTitulo().toLowerCase(Locale.ROOT).contains(buscado)
                        || libro.getIsbn().toLowerCase(Locale.ROOT).contains(buscado)
                        || libro.getAutoresTexto().toLowerCase(Locale.ROOT).contains(buscado)
                        || (libro.getGenero() != null && libro.getGenero().toLowerCase(Locale.ROOT).contains(buscado)))
                .toList();
    }

    public Libro buscarPorId(Long id) {
        return libroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el libro " + id));
    }

    public List<Autor> listarAutores() {
        return autorRepository.findAllByOrderByApellidosAscNombreAsc();
    }

    public long ejemplaresDisponibles(Libro libro) {
        return libro.getEjemplares()
                - prestamoRepository.countByLibroIdAndFechaDevolucionIsNull(libro.getId());
    }

    public boolean isbnRepetido(Libro libro) {
        return libroRepository.findByIsbn(libro.getIsbn())
                .filter(otro -> !otro.getId().equals(libro.getId()))
                .isPresent();
    }

    @Transactional
    public Libro crear(Libro libro) {
        libro.setId(null);
        Libro guardado = libroRepository.save(libro);
        // Mantiene también el lado inverso en el contexto de persistencia actual.
        guardado.getAutores().forEach(autor -> autor.getLibros().add(guardado));
        return guardado;
    }

    @Transactional
    public Libro actualizar(Long id, Libro datos) {
        Libro libro = buscarPorId(id);
        libro.setIsbn(datos.getIsbn());
        libro.setTitulo(datos.getTitulo());
        libro.setEditorial(datos.getEditorial());
        libro.setAnioPublicacion(datos.getAnioPublicacion());
        libro.setGenero(datos.getGenero());
        libro.setEjemplares(datos.getEjemplares());
        // Se usa una copia para poder modificar la colección durante el recorrido.
        new ArrayList<>(libro.getAutores()).forEach(libro::removeAutor);
        datos.getAutores().forEach(libro::addAutor);
        return libro;
    }

    @Transactional
    public Libro eliminar(Long id) {
        Libro libro = buscarPorId(id);
        if (prestamoRepository.existsByLibroId(id)) {
            throw new IllegalStateException("No se puede eliminar «" + libro.getTitulo()
                    + "» porque tiene préstamos registrados");
        }
        new ArrayList<>(libro.getAutores()).forEach(libro::removeAutor);
        libroRepository.delete(libro);
        return libro;
    }
}
