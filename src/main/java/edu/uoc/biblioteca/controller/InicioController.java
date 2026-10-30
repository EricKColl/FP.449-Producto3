package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.service.PanelService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioController {

    private final PanelService panelService;

    public InicioController(PanelService panelService) {
        this.panelService = panelService;
    }

    @GetMapping("/")
    public String inicio(Model model, HttpServletRequest peticion) {
        model.addAttribute("resumen", panelService.obtenerResumen());
        // Los préstamos llevan nombres de usuarios: solo se cargan para quien los puede ver
        if (peticion.isUserInRole("ADMIN")) {
            model.addAttribute("ultimos", panelService.ultimosPrestamos());
        }
        return "index";
    }
}
