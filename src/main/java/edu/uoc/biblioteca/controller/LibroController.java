package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.model.Libro;
import edu.uoc.biblioteca.security.CuentaUsuario;
import edu.uoc.biblioteca.service.FavoritoService;
import edu.uoc.biblioteca.service.LibroService;
import edu.uoc.biblioteca.service.PrestamoService;
import edu.uoc.biblioteca.service.ReservaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final PrestamoService prestamoService;
    private final ReservaService reservaService;

    public LibroController(LibroService libroService, FavoritoService favoritoService,
                           PrestamoService prestamoService, ReservaService reservaService) {
        this.libroService = libroService;
        this.favoritoService = favoritoService;
        this.prestamoService = prestamoService;
        this.reservaService = reservaService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("libros", libroService.conDisponibles(libroService.buscar(q)));
        model.addAttribute("generos", libroService.generos());
        model.addAttribute("q", q);
        return "libros/lista";
    }

    // Solo números: así una ruta antigua como /libros/nuevo responde 404 en lugar de 400
    @GetMapping("/{id:\\d+}")
    public String detalle(@PathVariable Long id, @AuthenticationPrincipal CuentaUsuario cuenta, Model model,
                          HttpServletRequest peticion) {
        Libro libro = libroService.buscarPorId(id);
        model.addAttribute("libro", libro);
        model.addAttribute("disponibles", libroService.ejemplaresDisponibles(libro));
        // Sin sesión, cuenta es null y la vista no enseña los botones de usuario
        boolean conCuenta = cuenta != null;
        model.addAttribute("esFavorito", conCuenta && favoritoService.esFavorito(cuenta.getId(), id));
        model.addAttribute("loTienePrestado", conCuenta && prestamoService.tieneSinDevolver(cuenta.getId(), id));
        model.addAttribute("loTieneReservado", conCuenta && reservaService.tieneReservaActiva(cuenta.getId(), id));
        model.addAttribute("relacionados", libroService.relacionados(libro, 6));
        // El historial lleva nombres de usuarios: solo se carga para el administrador
        if (peticion.isUserInRole("ADMIN")) {
            model.addAttribute("historial", prestamoService.historialDeLibro(id));
        }
        return "libros/detalle";
    }
}
