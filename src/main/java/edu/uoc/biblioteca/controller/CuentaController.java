package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.security.CuentaUsuario;
import edu.uoc.biblioteca.service.UsuarioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Rutas /user/*: lo que cada usuario hace con su propia cuenta. */
@Controller
@RequestMapping("/user")
public class CuentaController {

    private final UsuarioService usuarioService;

    public CuentaController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/cuenta")
    public String cuenta(@AuthenticationPrincipal CuentaUsuario cuenta, Model model) {
        model.addAttribute("usuario", usuarioService.buscarPorId(cuenta.getId()));
        return "user/cuenta";
    }
}
