package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.model.Libro;
import edu.uoc.biblioteca.service.LibroService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Alta, edición y borrado de libros: solo para el rol ADMIN. */
@Controller
@RequestMapping("/admin/libros")
public class LibroAdminController {

    private final LibroService libroService;

    public LibroAdminController(LibroService libroService) {
        this.libroService = libroService;
    }

    @InitBinder("libro")
    void configurarFormulario(WebDataBinder binder) {
        // El identificador procede de la ruta; el formulario solo modifica estos campos.
        binder.setAllowedFields("isbn", "titulo", "editorial", "anioPublicacion",
                "genero", "ejemplares", "autores", "_autores");
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("libro", new Libro());
        return formulario(model);
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("libro") Libro libro, BindingResult resultado,
                        Model model, RedirectAttributes redirectAttributes) {
        comprobarReglas(libro, resultado);
        if (resultado.hasErrors()) {
            return formulario(model);
        }
        Libro guardado = libroService.crear(libro);
        redirectAttributes.addFlashAttribute("mensajeExito",
                "Libro «" + guardado.getTitulo() + "» creado correctamente");
        return "redirect:/libros/" + guardado.getId();
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("libro", libroService.buscarPorId(id));
        return formulario(model);
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("libro") Libro libro, BindingResult resultado,
                             Model model, RedirectAttributes redirectAttributes) {
        libroService.buscarPorId(id);
        libro.setId(id);
        comprobarReglas(libro, resultado);
        if (resultado.hasErrors()) {
            return formulario(model);
        }
        libroService.actualizar(id, libro);
        redirectAttributes.addFlashAttribute("mensajeExito",
                "Libro «" + libro.getTitulo() + "» actualizado correctamente");
        return "redirect:/libros/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Libro eliminado = libroService.eliminar(id);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Libro «" + eliminado.getTitulo() + "» eliminado correctamente");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/libros";
    }

    private String formulario(Model model) {
        model.addAttribute("todosLosAutores", libroService.listarAutores());
        return "libros/formulario";
    }

    private void comprobarReglas(Libro libro, BindingResult resultado) {
        if (libro.getAutores() == null || libro.getAutores().isEmpty()
                || libro.getAutores().contains(null)) {
            resultado.rejectValue("autores", "autores.vacio", "Selecciona al menos un autor válido");
        }
        if (!resultado.hasFieldErrors("isbn") && libroService.isbnRepetido(libro)) {
            resultado.rejectValue("isbn", "isbn.repetido", "Ya existe otro libro con este ISBN");
        }
    }
}
