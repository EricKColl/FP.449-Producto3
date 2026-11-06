package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.security.IntentosDeAcceso;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

/** Muestra el formulario; el POST /login lo procesa Spring Security. */
@Controller
public class LoginController {

    private final IntentosDeAcceso intentos;

    public LoginController(IntentosDeAcceso intentos) {
        this.intentos = intentos;
    }

    @GetMapping("/login")
    public String login(Principal sesion, Model model) {
        model.addAttribute("minutosBloqueo", intentos.minutosDeBloqueo());
        return sesion == null ? "login" : "redirect:/inicio";
    }

    /**
     * El aviso de «tu sesión va a caducar» llama aquí para seguir conectado: basta con una petición para que
     * la sesión cuente de nuevo desde cero. No devuelve nada (204).
     */
    @GetMapping("/sesion/renovar")
    public ResponseEntity<Void> renovarSesion() {
        return ResponseEntity.noContent().build();
    }
}
