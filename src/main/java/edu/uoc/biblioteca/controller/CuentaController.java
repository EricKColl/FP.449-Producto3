package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.model.EstadoReserva;
import edu.uoc.biblioteca.model.Usuario;
import edu.uoc.biblioteca.repository.PrestamoRepository;
import edu.uoc.biblioteca.repository.ReservaRepository;
import edu.uoc.biblioteca.security.CuentaUsuario;
import edu.uoc.biblioteca.service.InicioClienteService;
import edu.uoc.biblioteca.service.UsuarioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Rutas /user/*: lo que cada usuario hace con su propia cuenta. */
@Controller
@RequestMapping("/user")
public class CuentaController {

    private final UsuarioService usuarioService;
    private final InicioClienteService inicioClienteService;
    private final PrestamoRepository prestamoRepository;
    private final ReservaRepository reservaRepository;

    public CuentaController(UsuarioService usuarioService, InicioClienteService inicioClienteService,
                            PrestamoRepository prestamoRepository, ReservaRepository reservaRepository) {
        this.usuarioService = usuarioService;
        this.inicioClienteService = inicioClienteService;
        this.prestamoRepository = prestamoRepository;
        this.reservaRepository = reservaRepository;
    }

    // Solo el rol USER: SecurityConfig reserva /user/** a los clientes, salvo /user/cuenta
    @GetMapping("/inicio")
    public String inicio(@AuthenticationPrincipal CuentaUsuario cuenta, Model model) {
        model.addAttribute("inicio", inicioClienteService.obtener(cuenta.getId()));
        return "user/inicio";
    }

    @GetMapping("/cuenta")
    public String cuenta(@AuthenticationPrincipal CuentaUsuario cuenta, Model model) {
        Usuario usuario = usuarioService.buscarPorId(cuenta.getId());
        model.addAttribute("usuario", usuario);
        model.addAttribute("prestamosTotales", prestamoRepository.countByUsuarioId(usuario.getId()));
        model.addAttribute("prestamosEnCurso", prestamoRepository.countByUsuarioIdAndFechaDevolucionIsNull(usuario.getId()));
        model.addAttribute("reservasActivas", reservaRepository.countByUsuarioIdAndEstado(usuario.getId(), EstadoReserva.ACTIVA));
        model.addAttribute("favoritos", usuario.getFavoritos().size());
        model.addAttribute("diasConCuenta", ChronoUnit.DAYS.between(usuario.getFechaAlta(), LocalDate.now()));
        return "user/cuenta";
    }

    // Cualquier cuenta (cliente o administrador) cambia aquí su propia contraseña: el id sale de la sesión
    @PostMapping("/cuenta/password")
    public String cambiarPassword(@AuthenticationPrincipal CuentaUsuario cuenta,
                                  @RequestParam(defaultValue = "") String actual,
                                  @RequestParam(defaultValue = "") String nueva,
                                  @RequestParam(defaultValue = "") String repetida,
                                  RedirectAttributes redirectAttributes) {
        try {
            usuarioService.cambiarPassword(cuenta.getId(), actual, nueva, repetida);
            redirectAttributes.addFlashAttribute("mensajeExito", "Contraseña cambiada. La próxima vez entra con la nueva");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("mensajeError", e.getMessage());
            redirectAttributes.addFlashAttribute("errorPassword", e.getMessage());
        }
        return "redirect:/user/cuenta#seguridad";
    }
}
