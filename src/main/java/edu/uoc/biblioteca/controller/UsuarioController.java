package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.model.Prestamo;
import edu.uoc.biblioteca.model.Reserva;
import edu.uoc.biblioteca.service.PrestamoService;
import edu.uoc.biblioteca.service.ReservaService;
import edu.uoc.biblioteca.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final PrestamoService prestamoService;
    private final ReservaService reservaService;

    public UsuarioController(UsuarioService usuarioService, PrestamoService prestamoService, ReservaService reservaService) {
        this.usuarioService = usuarioService;
        this.prestamoService = prestamoService;
        this.reservaService = reservaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listarConPrestamos());
        return "usuarios/lista";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        model.addAttribute("usuario", usuarioService.buscarPorId(id));
        // Su actividad: préstamos del más reciente al más antiguo y reservas activas
        List<Prestamo> historial = prestamoService.listarDeUsuario(id);
        model.addAttribute("historial", historial);
        model.addAttribute("enCurso", historial.stream().filter(p -> !p.isDevuelto()).count());
        model.addAttribute("vencidos", historial.stream().filter(p -> p.getDiasRetraso() > 0).count());
        model.addAttribute("reservas", reservaService.listarDeUsuario(id).stream().filter(Reserva::isActiva).toList());
        return "usuarios/detalle";
    }
}
