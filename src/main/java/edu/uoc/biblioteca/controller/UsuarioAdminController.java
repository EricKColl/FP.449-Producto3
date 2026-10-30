package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.model.Rol;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.security.Principal;

/**
 * Alta, edición y borrado de usuarios. Las consultas están en {@link UsuarioController}.
 * Se separan para que en el Producto 3 baste con mover este controlador a /admin/usuarios.
 */
@Controller
@RequestMapping("/usuarios")
public class UsuarioAdminController {

    private final UsuarioService usuarioService;

    public UsuarioAdminController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @InitBinder("usuario")
    void configurarFormulario(WebDataBinder binder) {
        // El identificador procede de la ruta; el formulario solo modifica estos campos.
        binder.setAllowedFields("dni", "nombre", "apellidos", "email", "telefono", "fechaAlta", "password", "rol");
    }

    @ModelAttribute("roles")
    Rol[] roles() {
        return Rol.values();
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "usuarios/formulario";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("usuario") Usuario usuario, BindingResult resultado,
                        RedirectAttributes redirectAttributes) {
        comprobarReglas(usuario, resultado);
        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            resultado.rejectValue("password", "password.vacia", "La contraseña es obligatoria");
        }
        if (resultado.hasErrors()) {
            return "usuarios/formulario";
        }
        Usuario guardado = usuarioService.crear(usuario);
        redirectAttributes.addFlashAttribute("mensajeExito",
                "Usuario «" + guardado.getNombreCompleto() + "» dado de alta correctamente");
        return "redirect:/usuarios/" + guardado.getId();
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("usuario", usuarioService.buscarPorId(id));
        return "usuarios/formulario";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("usuario") Usuario usuario, BindingResult resultado,
                             RedirectAttributes redirectAttributes) {
        usuarioService.buscarPorId(id);
        usuario.setId(id);
        comprobarReglas(usuario, resultado);
        if (usuario.getRol() != Rol.ADMIN && usuarioService.esUltimoAdmin(id)) {
            resultado.rejectValue("rol", "rol.ultimoAdmin", "Tiene que quedar al menos un administrador");
        }
        if (resultado.hasErrors()) {
            return "usuarios/formulario";
        }
        Usuario actualizado = usuarioService.actualizar(id, usuario);
        redirectAttributes.addFlashAttribute("mensajeExito",
                "Usuario «" + actualizado.getNombreCompleto() + "» actualizado correctamente");
        return "redirect:/usuarios/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Principal sesion, RedirectAttributes redirectAttributes) {
        try {
            Usuario eliminado = usuarioService.eliminar(id, sesion.getName());
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Usuario «" + eliminado.getNombreCompleto() + "» eliminado correctamente");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    /** DNI y email únicos: se comprueban aquí para mostrar el error junto al campo. */
    private void comprobarReglas(Usuario usuario, BindingResult resultado) {
        if (!resultado.hasFieldErrors("dni") && usuarioService.dniRepetido(usuario)) {
            resultado.rejectValue("dni", "dni.repetido", "Ya existe otro usuario con este DNI");
        }
        if (!resultado.hasFieldErrors("email") && usuarioService.emailRepetido(usuario)) {
            resultado.rejectValue("email", "email.repetido", "Ya existe otro usuario con este email");
        }
        // Límite de BCrypt: solo usa los 72 primeros bytes de la contraseña
        String password = usuario.getPassword();
        if (password != null && !password.isBlank()
                && (password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72)) {
            resultado.rejectValue("password", "password.longitud", "La contraseña debe tener entre 8 y 72 caracteres");
        }
    }
}
