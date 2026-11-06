package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.model.Reserva;
import edu.uoc.biblioteca.security.CuentaUsuario;
import edu.uoc.biblioteca.model.Prestamo;
import edu.uoc.biblioteca.service.PrestamoService;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Las reservas del usuario de la sesión: reservar un libro sin ejemplares y cancelar las propias. */
@Controller
@RequestMapping("/user/reservas")
public class MisReservasController {

    private final ReservaService reservaService;
    private final PrestamoService prestamoService;

    public MisReservasController(ReservaService reservaService, PrestamoService prestamoService) {
        this.reservaService = reservaService;
        this.prestamoService = prestamoService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal CuentaUsuario cuenta, Model model) {
        List<Reserva> reservas = reservaService.listarDeUsuario(cuenta.getId());
        model.addAttribute("reservas", reservas);
        // Para cada reserva activa: su puesto en la cola y cuántos ejemplares quedan para este usuario
        // (los libres van por orden de reserva). Si le toca, la tarjeta ofrece «Pedir»
        Map<Long, Integer> puestos = new HashMap<>();
        Map<Long, Long> disponibles = new HashMap<>();
        reservas.stream().filter(Reserva::isActiva).forEach(r -> {
            puestos.put(r.getId(), prestamoService.puestoEnCola(r));
            disponibles.put(r.getLibro().getId(), prestamoService.ejemplaresParaUsuario(r.getLibro(), cuenta.getId()));
        });
        model.addAttribute("puestos", puestos);
        model.addAttribute("disponibles", disponibles);
        // Reservas cerradas porque el socio se llevó el libro (no las canceló él)
        Set<Long> atendidas = new HashSet<>();
        List<Prestamo> suyos = prestamoService.listarDeUsuario(cuenta.getId());
        reservas.stream().filter(r -> !r.isActiva()).forEach(r -> {
            if (suyos.stream().anyMatch(p -> p.getLibro().getId().equals(r.getLibro().getId())
                    && !p.getFechaPrestamo().isBefore(r.getFechaReserva()))) {
                atendidas.add(r.getId());
            }
        });
        model.addAttribute("atendidas", atendidas);
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
