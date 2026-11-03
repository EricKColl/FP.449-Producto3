package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.model.Autor;
import edu.uoc.biblioteca.service.AutorService;
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

/**
 * Alta, edición y borrado de autores. Las consultas están en {@link AutorController}.
 * Solo para el rol ADMIN.
 */
@Controller
@RequestMapping("/admin/autores")
public class AutorAdminController {

    private final AutorService autorService;

    public AutorAdminController(AutorService autorService) {
        this.autorService = autorService;
    }

    @InitBinder("autor")
    void configurarFormulario(WebDataBinder binder) {
        // El identificador procede de la ruta; el formulario solo modifica estos campos.
        binder.setAllowedFields("nombre", "apellidos", "nacionalidad", "fechaNacimiento");
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("autor", new Autor());
        return "autores/formulario";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("autor") Autor autor, BindingResult resultado,
                        RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            return "autores/formulario";
        }
        Autor guardado = autorService.crear(autor);
        redirectAttributes.addFlashAttribute("mensajeExito",
                "Autor «" + guardado.getNombreCompleto() + "» creado correctamente");
        return "redirect:/autores/" + guardado.getId();
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("autor", autorService.buscarPorId(id));
        return "autores/formulario";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("autor") Autor autor, BindingResult resultado,
                             RedirectAttributes redirectAttributes) {
        autorService.buscarPorId(id);
        autor.setId(id);
        if (resultado.hasErrors()) {
            return "autores/formulario";
        }
        Autor actualizado = autorService.actualizar(id, autor);
        redirectAttributes.addFlashAttribute("mensajeExito",
                "Autor «" + actualizado.getNombreCompleto() + "» actualizado correctamente");
        return "redirect:/autores/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Autor eliminado = autorService.eliminar(id);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Autor «" + eliminado.getNombreCompleto() + "» eliminado correctamente");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/autores";
    }
}
