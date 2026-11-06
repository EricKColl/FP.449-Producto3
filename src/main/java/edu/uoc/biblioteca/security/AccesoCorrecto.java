package edu.uoc.biblioteca.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;

import java.io.IOException;

/**
 * Qué pasa al iniciar sesión bien: se olvidan los intentos fallidos y se vuelve a donde estaba el usuario.
 * <ol>
 *     <li>Si intentó abrir una página protegida, Spring Security la recuerda y vuelve a ella.</li>
 *     <li>Si pulsó «Iniciar sesión» desde una página (por ejemplo, la ficha de un libro), vuelve a esa página:
 *     llega en el parámetro {@code volver} y solo se acepta si es una ruta de esta misma aplicación.</li>
 *     <li>Si no, cada rol va a su inicio ({@code /inicio}).</li>
 * </ol>
 */
public class AccesoCorrecto extends SavedRequestAwareAuthenticationSuccessHandler {

    private final IntentosDeAcceso intentos;
    private final RequestCache paginasGuardadas = new HttpSessionRequestCache();

    public AccesoCorrecto(IntentosDeAcceso intentos) {
        this.intentos = intentos;
        setDefaultTargetUrl("/inicio");
        setRequestCache(paginasGuardadas);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication autenticacion) throws ServletException, IOException {
        intentos.registrarExito(autenticacion.getName());
        String volver = request.getParameter("volver");
        if (paginasGuardadas.getRequest(request, response) == null && esRutaPropia(volver)) {
            clearAuthenticationAttributes(request);
            getRedirectStrategy().sendRedirect(request, response, volver);
            return;
        }
        super.onAuthenticationSuccess(request, response, autenticacion);
    }

    /**
     * Solo rutas internas: empiezan por una barra y no por dos (//otro-dominio.com), sin esquema, sin barras
     * invertidas ni saltos de línea. Así el parámetro no sirve para mandar a nadie a una web falsa.
     */
    static boolean esRutaPropia(String ruta) {
        if (ruta == null || ruta.length() > 300 || !ruta.startsWith("/") || ruta.startsWith("//")) {
            return false;
        }
        if (ruta.contains("\\") || ruta.contains("://") || ruta.chars().anyMatch(Character::isISOControl)) {
            return false;
        }
        return !ruta.startsWith("/login") && !ruta.startsWith("/registro") && !ruta.startsWith("/logout")
                && !ruta.startsWith("/sesion") && !ruta.startsWith("/avisos");
    }
}
