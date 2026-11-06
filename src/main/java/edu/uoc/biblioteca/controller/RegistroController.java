package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.model.Usuario;
import edu.uoc.biblioteca.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

/** Registro público de socios; el rol y la fecha de alta los fija el servicio. */
@Controller
public class RegistroController {

    private final UsuarioService usuarioService;

    public RegistroController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @InitBinder("usuario")
    void configurarFormulario(WebDataBinder binder) {
        // El registro no permite elegir identificador, rol ni fecha de alta.
        binder.setAllowedFields("dni", "nombre", "apellidos", "email", "telefono", "password");
    }

    @GetMapping("/registro")
    public String registro(Principal sesion, Model model) {
        if (sesion != null) {
            return "redirect:/";
        }
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("usuario") Usuario usuario, BindingResult resultado,
                            @RequestParam(defaultValue = "") String confirmarPassword, Principal sesion) {
        if (sesion != null) {
            return "redirect:/";
        }
        if (!resultado.hasFieldErrors("dni") && usuarioService.dniRepetido(usuario)) {
            resultado.rejectValue("dni", "dni.repetido", "Ya existe otro usuario con este DNI");
        }
        if (!resultado.hasFieldErrors("email") && usuarioService.emailRepetido(usuario)) {
            resultado.rejectValue("email", "email.repetido", "Ya existe otro usuario con este email");
        }
        String password = usuario.getPassword();
        if (password == null || password.isBlank()) {
            resultado.rejectValue("password", "password.vacia", "La contraseña es obligatoria");
        } else if (!UsuarioService.passwordValida(password)) {
            resultado.rejectValue("password", "password.longitud", UsuarioService.problemaPassword(password));
        } else if (!password.equals(confirmarPassword)) {
            resultado.rejectValue("password", "password.confirmacion", "Las contraseñas no coinciden");
        }
        if (resultado.hasErrors()) {
            return "registro";
        }
        usuarioService.registrar(usuario);
        return "redirect:/login?registrado";
    }
}
