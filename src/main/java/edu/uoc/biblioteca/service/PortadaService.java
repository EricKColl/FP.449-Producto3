package edu.uoc.biblioteca.service;

import edu.uoc.biblioteca.repository.AutorRepository;
import edu.uoc.biblioteca.repository.LibroRepository;
import edu.uoc.biblioteca.repository.PrestamoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Datos públicos de la portada: solo cifras y catálogo, nunca datos de usuarios. */
@Service
@Transactional(readOnly = true)
public class PortadaService {

    private final LibroRepository libroRepository;
    private final AutorRepository autorRepository;
    private final PrestamoRepository prestamoRepository;
    private final LibroService libroService;

    public PortadaService(LibroRepository libroRepository, AutorRepository autorRepository,
                          PrestamoRepository prestamoRepository, LibroService libroService) {
        this.libroRepository = libroRepository;
        this.autorRepository = autorRepository;
        this.prestamoRepository = prestamoRepository;
        this.libroService = libroService;
    }

    public Portada obtener() {
        long ejemplares = libroRepository.sumarEjemplares();
        List<LibroDisponible> novedades = libroRepository.findTop6ByOrderByIdDesc().stream()
                .map(libro -> new LibroDisponible(libro, libroService.ejemplaresDisponibles(libro)))
                .toList();
        return new Portada(
                libroRepository.count(),
                autorRepository.count(),
                ejemplares,
                ejemplares - prestamoRepository.countByFechaDevolucionIsNull(),
                novedades,
                libroRepository.contarPorGenero(),
                autorRepository.listarConLibros().stream().limit(6).toList());
    }

    public record Portada(long libros, long autores, long ejemplares, long disponibles,
                          List<LibroDisponible> novedades, List<Cifra> generos, List<AutorDestacado> autoresDestacados) {
    }
}
