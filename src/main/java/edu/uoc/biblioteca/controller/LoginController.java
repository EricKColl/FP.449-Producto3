package edu.uoc.biblioteca.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

/** Muestra el formulario; el POST /login lo procesa Spring Security. */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(Principal sesion) {
        return sesion == null ? "login" : "redirect:/";
    }
}
