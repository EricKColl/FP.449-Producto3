package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.model.Libro;
import edu.uoc.biblioteca.security.CuentaUsuario;
import edu.uoc.biblioteca.service.BusquedaService;
import edu.uoc.biblioteca.service.BusquedaService.Sugerencias;
import edu.uoc.biblioteca.service.FavoritoService;
import edu.uoc.biblioteca.service.LibroService;
import edu.uoc.biblioteca.service.PrestamoService;
import edu.uoc.biblioteca.service.ReservaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/** Consulta pública del catálogo; la escritura está en {@link LibroAdminController}. */
@Controller
@RequestMapping("/libros")
public class LibroController {

    private final LibroService libroService;
    private final FavoritoService favoritoService;
    private final PrestamoService prestamoService;
    private final ReservaService reservaService;
    private final BusquedaService busquedaService;

    public LibroController(LibroService libroService, FavoritoService favoritoService,
                           PrestamoService prestamoService, ReservaService reservaService,
                           BusquedaService busquedaService) {
        this.busquedaService = busquedaService;
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

    /**
     * Sugerencias del buscador mientras se escribe (js/app.js), en JSON. Es pública como el catálogo y solo
     * devuelve datos que ya enseña la web: título, autores, año y ejemplares libres.
     */
    @GetMapping(value = "/sugerencias", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Sugerencias sugerencias(@RequestParam(defaultValue = "") String q) {
        return busquedaService.sugerir(q.length() > 80 ? q.substring(0, 80) : q);
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
        // Ejemplares que puede llevarse quien mira la ficha: los libres van primero a quien lo reservó antes
        model.addAttribute("paraTi", conCuenta ? prestamoService.ejemplaresParaUsuario(libro, cuenta.getId())
                : libroService.ejemplaresDisponibles(libro));
        model.addAttribute("relacionados", libroService.relacionados(libro, 6));
        // El historial lleva nombres de usuarios: solo se carga para el administrador
        if (peticion.isUserInRole("ADMIN")) {
            model.addAttribute("historial", prestamoService.historialDeLibro(id));
        }
        return "libros/detalle";
    }
}
