package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.model.Prestamo;
import edu.uoc.biblioteca.security.CuentaUsuario;
import edu.uoc.biblioteca.service.PrestamoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        List<Prestamo> prestamos = prestamoService.listarDeUsuario(cuenta.getId());
        model.addAttribute("prestamos", prestamos);
        // Para cada préstamo en curso, por qué no se puede renovar ("" si se puede)
        Map<Long, String> renovacion = new HashMap<>();
        prestamos.stream().filter(p -> !p.isDevuelto()).forEach(p -> {
            String motivo = prestamoService.motivoNoRenovable(p);
            renovacion.put(p.getId(), motivo == null ? "" : motivo);
        });
        model.addAttribute("renovacion", renovacion);
        model.addAttribute("diasRenovacion", PrestamoService.DIAS_RENOVACION);
        return "user/prestamos";
    }

    // Solo sus propios préstamos: el de otro usuario da 404 (PrestamoService.renovar)
    @PostMapping("/{id}/renovar")
    public String renovar(@AuthenticationPrincipal CuentaUsuario cuenta, @PathVariable Long id,
                          RedirectAttributes redirectAttributes) {
        try {
            Prestamo prestamo = prestamoService.renovar(id, cuenta.getId());
            redirectAttributes.addFlashAttribute("mensajeExito", "Renovado: «" + prestamo.getLibro().getTitulo()
                    + "» hasta el " + prestamo.getFechaDevolucionPrevista().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/user/prestamos";
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
