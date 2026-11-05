package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.model.Prestamo;
import edu.uoc.biblioteca.security.CuentaUsuario;
import edu.uoc.biblioteca.service.PrestamoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Los préstamos del usuario de la sesión. El usuario nunca llega del formulario: sale de la sesión. */
@Controller
@RequestMapping("/user/prestamos")
public class MisPrestamosController {

    private final PrestamoService prestamoService;

    public MisPrestamosController(PrestamoService prestamoService) {
        this.prestamoService = prestamoService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal CuentaUsuario cuenta, Model model) {
        model.addAttribute("prestamos", prestamoService.listarDeUsuario(cuenta.getId()));
        return "user/prestamos";
    }

    // Las reglas son las mismas que en /admin/prestamos: están en PrestamoService.crear
    @PostMapping
    public String pedir(@AuthenticationPrincipal CuentaUsuario cuenta, @RequestParam Long libroId,
                        RedirectAttributes redirectAttributes) {
        try {
            Prestamo prestamo = prestamoService.crear(cuenta.getId(), libroId);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Préstamo registrado: «" + prestamo.getLibro().getTitulo() + "»");
            return "redirect:/user/prestamos";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("mensajeError", e.getMessage());
            return "redirect:/libros/" + libroId;
        }
    }
}
