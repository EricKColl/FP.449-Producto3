package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.security.CuentaUsuario;
import edu.uoc.biblioteca.service.AvisosEnVivo;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Conexión de avisos en tiempo real de cada página con sesión (js/app.js la abre con EventSource). */
@RestController
public class AvisosController {

    private final AvisosEnVivo avisosEnVivo;

    public AvisosController(AvisosEnVivo avisosEnVivo) {
        this.avisosEnVivo = avisosEnVivo;
    }

    // El usuario sale siempre de la sesión: nadie puede escuchar los avisos de otro
    @GetMapping(value = "/avisos/flujo", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter flujo(@AuthenticationPrincipal CuentaUsuario cuenta) {
        boolean admin = cuenta.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return avisosEnVivo.conectar(cuenta.getId(), admin);
    }
}
