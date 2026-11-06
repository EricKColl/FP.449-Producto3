package edu.uoc.biblioteca.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freno contra los ataques de fuerza bruta en el login: tras varios intentos fallidos seguidos con el mismo email,
 * ese email queda bloqueado unos minutos aunque luego se escriba bien la contraseña.
 * <p>
 * Se cuentan también los emails que no existen, así el aviso de bloqueo no revela qué cuentas hay.
 * Los datos viven en memoria: al reiniciar la aplicación se olvidan, que para una biblioteca es suficiente.
 */
@Component
public class IntentosDeAcceso {

    /** Por encima de este tamaño se limpian los registros caducados, para que la memoria no crezca sin límite. */
    private static final int LIMPIAR_A_PARTIR_DE = 10_000;

    private final int maximo;
    private final Duration bloqueo;
    private final Clock reloj;
    private final Map<String, Registro> registros = new ConcurrentHashMap<>();

    private record Registro(int fallos, Instant bloqueadoHasta) {
    }

    @Autowired
    public IntentosDeAcceso(@Value("${biblioteca.login.intentos-maximos:5}") int maximo,
                            @Value("${biblioteca.login.minutos-bloqueo:5}") long minutos) {
        this(maximo, Duration.ofMinutes(minutos), Clock.systemUTC());
    }

    IntentosDeAcceso(int maximo, Duration bloqueo, Clock reloj) {
        this.maximo = maximo;
        this.bloqueo = bloqueo;
        this.reloj = reloj;
    }

    /** Si el email está bloqueado ahora mismo. */
    public boolean bloqueado(String email) {
        Registro registro = registros.get(clave(email));
        return registro != null && registro.bloqueadoHasta() != null && reloj.instant().isBefore(registro.bloqueadoHasta());
    }

    /** Apunta un intento fallido. Devuelve true si con este intento el email queda bloqueado. */
    public boolean registrarFallo(String email) {
        if (registros.size() > LIMPIAR_A_PARTIR_DE) {
            limpiar();
        }
        Instant ahora = reloj.instant();
        Registro registro = registros.compute(clave(email), (k, anterior) -> {
            int fallos = anterior == null || anterior.bloqueadoHasta() != null ? 1 : anterior.fallos() + 1;
            return fallos >= maximo ? new Registro(0, ahora.plus(bloqueo)) : new Registro(fallos, null);
        });
        return registro.bloqueadoHasta() != null;
    }

    /** Al entrar bien se olvidan los fallos anteriores. */
    public void registrarExito(String email) {
        registros.remove(clave(email));
    }

    public long minutosDeBloqueo() {
        return bloqueo.toMinutes();
    }

    private void limpiar() {
        Instant ahora = reloj.instant();
        registros.entrySet().removeIf(e -> e.getValue().bloqueadoHasta() == null
                || !ahora.isBefore(e.getValue().bloqueadoHasta()));
    }

    private static String clave(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
