package edu.uoc.biblioteca.service;

import edu.uoc.biblioteca.model.EstadoReserva;
import edu.uoc.biblioteca.repository.ReservaRepository;
import edu.uoc.biblioteca.service.AvisoService.Aviso;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Avisos en tiempo real con Server-Sent Events: cada página abierta con sesión mantiene una conexión
 * (GET /avisos/flujo) y, cuando cambia algo que le afecta, el servidor le manda su lista de avisos nueva.
 * Así, si la biblioteca registra la devolución de un libro reservado, a quien lo reservó le suena la campanita
 * en ese momento, sin recargar.
 * <p>
 * Los avisos se envían después de confirmar la transacción (si se deshace, no se avisa de nada) y en otro hilo,
 * para no retrasar la respuesta de quien hizo la operación.
 */
@Component
public class AvisosEnVivo implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(AvisosEnVivo.class);

    /** Una conexión dura media hora como mucho; el navegador vuelve a conectar solo. */
    static final Duration DURACION = Duration.ofMinutes(30);

    private final AvisoService avisoService;
    private final ReservaRepository reservaRepository;
    private final Map<Long, List<SseEmitter>> socios = new ConcurrentHashMap<>();
    private final List<SseEmitter> administracion = new CopyOnWriteArrayList<>();

    public AvisosEnVivo(AvisoService avisoService, ReservaRepository reservaRepository) {
        this.avisoService = avisoService;
        this.reservaRepository = reservaRepository;
    }

    /** Abre la conexión de una página. El socio recibe sus avisos; la administración, los suyos. */
    public SseEmitter conectar(Long usuarioId, boolean admin) {
        SseEmitter emisor = new SseEmitter(DURACION.toMillis());
        List<SseEmitter> lista = admin ? administracion : socios.computeIfAbsent(usuarioId, id -> new CopyOnWriteArrayList<>());
        lista.add(emisor);
        Runnable quitar = () -> lista.remove(emisor);
        emisor.onCompletion(quitar);
        emisor.onTimeout(quitar);
        emisor.onError(e -> quitar.run());
        // Primer mensaje: confirma la conexión (y cuánto esperar antes de reintentar si se corta)
        enviar(emisor, SseEmitter.event().name("conectado").data("ok").reconnectTime(5_000), quitar);
        return emisor;
    }

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void alCambiar(AvisosCambiados cambio) {
        // Afectados: quien hizo la operación y quienes esperan ese libro (su puesto o su ejemplar cambian)
        Set<Long> afectados = new HashSet<>();
        if (cambio.usuarioId() != null) {
            afectados.add(cambio.usuarioId());
        }
        if (cambio.libroId() != null) {
            reservaRepository.findByLibroIdAndEstadoOrderByFechaReservaAscIdAsc(cambio.libroId(), EstadoReserva.ACTIVA)
                    .forEach(r -> afectados.add(r.getUsuario().getId()));
        }
        for (Long id : afectados) {
            List<SseEmitter> conexiones = socios.get(id);
            if (conexiones != null && !conexiones.isEmpty()) {
                List<Aviso> avisos = avisoService.deSocio(id);
                conexiones.forEach(e -> enviar(e, SseEmitter.event().name("avisos").data(avisos, MediaType.APPLICATION_JSON),
                        () -> conexiones.remove(e)));
            }
        }
        if (!administracion.isEmpty()) {
            List<Aviso> avisos = avisoService.deAdministracion();
            administracion.forEach(e -> enviar(e, SseEmitter.event().name("avisos").data(avisos, MediaType.APPLICATION_JSON),
                    () -> administracion.remove(e)));
        }
    }

    /**
     * Cada 25 segundos, un comentario vacío por cada conexión: así ningún proxy la corta por inactividad y se
     * detectan las páginas que ya se cerraron.
     */
    @Scheduled(fixedRate = 25_000, initialDelay = 25_000)
    public void latido() {
        // Ningún fallo con una conexión debe parar el latido de las demás: enviar() ya los recoge
        socios.values().forEach(lista -> lista.forEach(e -> enviar(e, SseEmitter.event().comment("latido"), () -> lista.remove(e))));
        administracion.forEach(e -> enviar(e, SseEmitter.event().comment("latido"), () -> administracion.remove(e)));
        socios.values().removeIf(List::isEmpty);
    }

    // --- Apagado: el servidor espera a que terminen las peticiones en curso antes de pararse, y una conexión de
    // avisos puede durar media hora. Por eso se cierran todas justo antes (esta fase se para antes que el
    // servidor web): los navegadores vuelven a conectar solos cuando la aplicación arranca de nuevo ---

    private volatile boolean enMarcha;

    @Override
    public void start() {
        enMarcha = true;
    }

    @Override
    public void stop() {
        enMarcha = false;
        socios.values().forEach(lista -> lista.forEach(AvisosEnVivo::cerrar));
        administracion.forEach(AvisosEnVivo::cerrar);
        socios.clear();
        administracion.clear();
    }

    @Override
    public boolean isRunning() {
        return enMarcha;
    }

    private static void cerrar(SseEmitter emisor) {
        try {
            emisor.complete();
        } catch (IllegalStateException e) {
            // Ya estaba cerrada
            log.debug("Conexión de avisos ya cerrada: {}", e.getMessage());
        }
    }

    /** Conexiones abiertas ahora mismo (para los tests y para comprobar que se limpian). */
    public int conexiones() {
        return administracion.size() + socios.values().stream().mapToInt(List::size).sum();
    }

    private static void enviar(SseEmitter emisor, SseEmitter.SseEventBuilder evento, Runnable siFalla) {
        try {
            emisor.send(evento);
        } catch (IOException | IllegalStateException e) {
            // La página se cerró: se quita la conexión sin más. Tomcat ya la ha dado por terminada, así que
            // no se vuelve a tocar (completarla otra vez lanzaría otra excepción y pararía el latido)
            log.debug("Conexión de avisos cerrada: {}", e.getMessage());
            siFalla.run();
        }
    }
}
