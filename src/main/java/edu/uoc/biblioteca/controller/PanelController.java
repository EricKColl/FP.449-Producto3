package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.service.PanelService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Panel del administrador, en /admin: solo el rol ADMIN llega aquí. */
@Controller
public class PanelController {

    private final PanelService panelService;

    public PanelController(PanelService panelService) {
        this.panelService = panelService;
    }

    @GetMapping("/admin/panel")
    public String panel(Model model) {
        model.addAttribute("panel", panelService.obtenerPanel());
        return "admin/panel";
    }
}
