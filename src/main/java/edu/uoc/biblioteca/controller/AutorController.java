package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.model.Autor;
import edu.uoc.biblioteca.model.Libro;
import edu.uoc.biblioteca.service.AutorService;
import edu.uoc.biblioteca.service.LibroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Comparator;

@Controller
@RequestMapping("/autores")
public class AutorController {

    private final AutorService autorService;
    private final LibroService libroService;

    public AutorController(AutorService autorService, LibroService libroService) {
        this.autorService = autorService;
        this.libroService = libroService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("autores", autorService.listar());
        return "autores/lista";
    }

    // Solo números: así una ruta antigua como /autores/nuevo responde 404 en lugar de 400
    @GetMapping("/{id:\\d+}")
    public String detalle(@PathVariable Long id, Model model) {
        Autor autor = autorService.buscarPorId(id);
        model.addAttribute("autor", autor);
        // Su bibliografía, de la más antigua a la más reciente, con los ejemplares libres de cada libro
        model.addAttribute("libros", libroService.conDisponibles(autor.getLibros().stream()
                .sorted(Comparator.comparing(Libro::getAnioPublicacion, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList()));
        return "autores/detalle";
    }
}
