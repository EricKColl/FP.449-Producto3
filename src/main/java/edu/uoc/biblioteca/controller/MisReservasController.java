package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.model.Reserva;
import edu.uoc.biblioteca.security.CuentaUsuario;
import edu.uoc.biblioteca.service.ReservaService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Las reservas del usuario de la sesión: reservar un libro sin ejemplares y cancelar las propias. */
@Controller
@RequestMapping("/user/reservas")
public class MisReservasController {

    private final ReservaService reservaService;

    public MisReservasController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal CuentaUsuario cuenta, Model model) {
        model.addAttribute("reservas", reservaService.listarDeUsuario(cuenta.getId()));
        return "user/reservas";
    }

    @PostMapping
    public String reservar(@AuthenticationPrincipal CuentaUsuario cuenta, @RequestParam Long libroId,
                           RedirectAttributes redirectAttributes) {
        try {
            Reserva reserva = reservaService.reservar(cuenta.getId(), libroId);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Reserva registrada: «" + reserva.getLibro().getTitulo() + "»");
            return "redirect:/user/reservas";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("mensajeError", e.getMessage());
            return "redirect:/libros/" + libroId;
        }
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@AuthenticationPrincipal CuentaUsuario cuenta, @PathVariable Long id,
                           RedirectAttributes redirectAttributes) {
        try {
            Reserva reserva = reservaService.cancelar(id, cuenta.getId());
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Reserva cancelada: «" + reserva.getLibro().getTitulo() + "»");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/user/reservas";
    }
}
