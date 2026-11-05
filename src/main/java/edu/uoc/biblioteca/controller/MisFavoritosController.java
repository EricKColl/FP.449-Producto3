package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.model.Libro;
import edu.uoc.biblioteca.security.CuentaUsuario;
import edu.uoc.biblioteca.service.FavoritoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Los libros favoritos del usuario de la sesión. */
@Controller
@RequestMapping("/user/favoritos")
public class MisFavoritosController {

    private final FavoritoService favoritoService;

    public MisFavoritosController(FavoritoService favoritoService) {
        this.favoritoService = favoritoService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal CuentaUsuario cuenta, Model model) {
        model.addAttribute("favoritos", favoritoService.listar(cuenta.getId()));
        return "user/favoritos";
    }

    @PostMapping("/{libroId}")
    public String marcar(@AuthenticationPrincipal CuentaUsuario cuenta, @PathVariable Long libroId,
                         RedirectAttributes redirectAttributes) {
        Libro libro = favoritoService.marcar(cuenta.getId(), libroId);
        redirectAttributes.addFlashAttribute("mensajeExito", "«" + libro.getTitulo() + "» añadido a tus favoritos");
        return "redirect:/libros/" + libroId;
    }

    // Se quita desde la ficha del libro o desde la lista: cada uno vuelve a su página
    @PostMapping("/{libroId}/quitar")
    public String quitar(@AuthenticationPrincipal CuentaUsuario cuenta, @PathVariable Long libroId,
                         @RequestParam(defaultValue = "false") boolean desdeFicha,
                         RedirectAttributes redirectAttributes) {
        Libro libro = favoritoService.quitar(cuenta.getId(), libroId);
        redirectAttributes.addFlashAttribute("mensajeExito", "«" + libro.getTitulo() + "» quitado de tus favoritos");
        return desdeFicha ? "redirect:/libros/" + libroId : "redirect:/user/favoritos";
    }
}
