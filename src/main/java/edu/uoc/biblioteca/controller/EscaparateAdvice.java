package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.model.Libro;
import edu.uoc.biblioteca.service.PortadaService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/** Libros del anillo de portadas que acompaña al formulario de login y al de registro: ${escaparate}. */
@ControllerAdvice(assignableTypes = {LoginController.class, RegistroController.class})
public class EscaparateAdvice {

    private final PortadaService portadaService;

    public EscaparateAdvice(PortadaService portadaService) {
        this.portadaService = portadaService;
    }

    @ModelAttribute("escaparate")
    public List<Libro> escaparate() {
        return portadaService.escaparate();
    }
}
