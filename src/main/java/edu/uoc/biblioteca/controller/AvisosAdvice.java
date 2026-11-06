package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.security.CuentaUsuario;
import edu.uoc.biblioteca.service.AvisoService;
import edu.uoc.biblioteca.service.AvisoService.Aviso;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;

/**
 * Avisos de la campanita para la cabecera de todas las páginas: ${avisos}. Solo con sesión iniciada y en las
 * páginas que se ven (GET); null si no aplica, y entonces la cabecera no muestra la campanita.
 * El servicio se pide con ObjectProvider para que los tests de un solo controlador (@WebMvcTest) no lo necesiten.
 */
@ControllerAdvice
public class AvisosAdvice {

    private final ObjectProvider<AvisoService> avisoService;

    public AvisosAdvice(ObjectProvider<AvisoService> avisoService) {
        this.avisoService = avisoService;
    }

    @ModelAttribute("avisos")
    public List<Aviso> avisos(Authentication autenticacion, HttpServletRequest request) {
        AvisoService servicio = avisoService.getIfAvailable();
        if (servicio == null || !"GET".equals(request.getMethod()) || !devuelvePagina(request)
                || autenticacion == null || !(autenticacion.getPrincipal() instanceof CuentaUsuario cuenta)) {
            return null;
        }
        boolean admin = cuenta.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return admin ? servicio.deAdministracion() : servicio.deSocio(cuenta.getId());
    }

    /**
     * Si la petición pinta una página. Las que devuelven datos (JSON de las sugerencias, la conexión de avisos en
     * tiempo real, renovar la sesión) no necesitan la campanita. Además, en la conexión de avisos, que dura minutos,
     * consultar la base de datos aquí dejaría ocupada una conexión del pool todo ese tiempo.
     */
    static boolean devuelvePagina(HttpServletRequest request) {
        if (!(request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE) instanceof HandlerMethod metodo)) {
            return true;
        }
        Class<?> tipo = metodo.getMethod().getReturnType();
        return !metodo.hasMethodAnnotation(ResponseBody.class)
                && !AnnotatedElementUtils.hasAnnotation(metodo.getBeanType(), ResponseBody.class)
                && !ResponseEntity.class.isAssignableFrom(tipo)
                && !ResponseBodyEmitter.class.isAssignableFrom(tipo);
    }
}
