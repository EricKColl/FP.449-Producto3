package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.service.PortadaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Portada pública y reparto de cada cuenta a su inicio según el rol. */
@Controller
public class InicioController {

    private final PortadaService portadaService;

    public InicioController(PortadaService portadaService) {
        this.portadaService = portadaService;
    }

    @GetMapping("/")
    public String portada(Model model) {
        model.addAttribute("portada", portadaService.obtener());
        return "index";
    }

    // El administrador ve el panel de toda la biblioteca; el cliente, su propio inicio
    @GetMapping("/inicio")
    public String inicio(HttpServletRequest peticion) {
        return peticion.isUserInRole("ADMIN") ? "redirect:/admin/panel" : "redirect:/user/inicio";
    }
}
