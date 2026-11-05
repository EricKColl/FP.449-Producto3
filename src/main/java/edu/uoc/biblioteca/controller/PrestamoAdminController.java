package edu.uoc.biblioteca.controller;
import edu.uoc.biblioteca.model.EstadoPrestamo;
import edu.uoc.biblioteca.model.Prestamo;
import edu.uoc.biblioteca.service.PrestamoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
/** Gestión de todos los préstamos: solo para el rol ADMIN. */
@Controller
@RequestMapping("/admin/prestamos")
public class PrestamoAdminController {
    private final PrestamoService prestamoService;
    public PrestamoAdminController(PrestamoService prestamoService) {
        this.prestamoService = prestamoService;
    }
    // Listado, con filtro opcional: /prestamos?estado=VENCIDO
    @GetMapping
    public String listar(@RequestParam(required = false) EstadoPrestamo estado, Model model) {
        model.addAttribute("prestamos", prestamoService.listar(estado));
        model.addAttribute("estados", EstadoPrestamo.values());
        model.addAttribute("estadoSeleccionado", estado);
        return "prestamos/lista";
    }
    // Formulario de nuevo préstamo
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("usuarios", prestamoService.listarUsuariosPrestatarios());
        model.addAttribute("maxPrestamos", PrestamoService.MAX_PRESTAMOS_ABIERTOS);
        model.addAttribute("libros", prestamoService.listarLibrosConDisponibles());
        return "prestamos/form";
    }
    // Registrar un préstamo nuevo
    @PostMapping
    public String crear(@RequestParam Long usuarioId,
                        @RequestParam Long libroId,

                        RedirectAttributes redirectAttributes) {
        try {
            Prestamo prestamo = prestamoService.crear(usuarioId, libroId);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Préstamo registrado: «" + prestamo.getLibro().getTitulo() + "»");
            return "redirect:/admin/prestamos";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("mensajeError", e.getMessage());
            return "redirect:/admin/prestamos/nuevo";
        }
    }
    // Registrar la devolución de un préstamo
    @PostMapping("/{id}/devolver")
    public String devolver(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Prestamo prestamo = prestamoService.registrarDevolucion(id);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Devolución registrada: «" + prestamo.getLibro().getTitulo() + "»");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/admin/prestamos";
    }
}