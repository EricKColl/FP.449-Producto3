package edu.uoc.biblioteca.controller;
import edu.uoc.biblioteca.security.CuentaUsuario;
import edu.uoc.biblioteca.service.FavoritoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import edu.uoc.biblioteca.model.Libro;
import edu.uoc.biblioteca.service.LibroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Consulta pública del catálogo; la escritura está en {@link LibroAdminController}. */
@Controller
@RequestMapping("/libros")
public class LibroController {

    private final LibroService libroService;
    private final FavoritoService favoritoService;

    public LibroController(LibroService libroService, FavoritoService favoritoService) {
        this.libroService = libroService;
        this.favoritoService = favoritoService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("libros", libroService.buscar(q));
        model.addAttribute("q", q);
        return "libros/lista";
    }

    // Solo números: así una ruta antigua como /libros/nuevo responde 404 en lugar de 400
    @GetMapping("/{id:\\d+}")
    public String detalle(@PathVariable Long id, @AuthenticationPrincipal CuentaUsuario cuenta, Model
            model) {
        Libro libro = libroService.buscarPorId(id);
        model.addAttribute("libro", libro);
        model.addAttribute("disponibles", libroService.ejemplaresDisponibles(libro));
        // Sin sesión, cuenta es null y la vista no enseña los botones de usuario
        model.addAttribute("esFavorito", cuenta != null && favoritoService.esFavorito(cuenta.getId(), id));
        return "libros/detalle";
    }
}
