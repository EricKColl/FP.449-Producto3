package edu.uoc.biblioteca.security;

import edu.uoc.biblioteca.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mejoras de seguridad de las cuentas: cambiar la contraseña, freno a la fuerza bruta, «Recordarme»,
 * volver a la página de origen tras entrar y renovar la sesión.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class CuentaSeguraTest {

    private static final String MARTA = "marta.sanchez@example.com";
    private static final String CLAVE = "Usuario1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService cuentas;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private IntentosDeAcceso intentos;

    // El registro de intentos vive en memoria y lo comparten todos los tests: se deja limpio
    @AfterEach
    void olvidarIntentos() {
        intentos.registrarExito(MARTA);
        intentos.registrarExito("nadie@example.com");
    }

    private RequestPostProcessor marta() {
        return user(cuentas.loadUserByUsername(MARTA));
    }

    private ResultActions entrar(String email, String clave) throws Exception {
        return mockMvc.perform(post("/login").with(csrf()).param("email", email).param("password", clave));
    }

    private ResultActions cambiar(String actual, String nueva, String repetida) throws Exception {
        return mockMvc.perform(post("/user/cuenta/password").with(marta()).with(csrf())
                .param("actual", actual).param("nueva", nueva).param("repetida", repetida));
    }

    private String passwordGuardada() {
        return usuarioRepository.findByEmail(MARTA).orElseThrow().getPassword();
    }

    // --- Cambiar la contraseña ---

    @Test
    void cambiaSuContrasenaYEntraConLaNueva() throws Exception {
        cambiar(CLAVE, "OtraClave-2026", "OtraClave-2026")
                .andExpect(redirectedUrl("/user/cuenta#seguridad"))
                .andExpect(flash().attributeExists("mensajeExito"));

        assertThat(passwordEncoder.matches("OtraClave-2026", passwordGuardada())).isTrue();
        entrar(MARTA, "OtraClave-2026").andExpect(authenticated());
    }

    @Test
    void noCambiaLaContrasenaSiAlgoFalla() throws Exception {
        String antes = passwordGuardada();
        cambiar("mal", "OtraClave-2026", "OtraClave-2026")
                .andExpect(flash().attribute("mensajeError", containsString("actual no es correcta")));
        cambiar(CLAVE, "corta", "corta")
                .andExpect(flash().attribute("mensajeError", containsString("al menos 8")));
        cambiar(CLAVE, "OtraClave-2026", "OtraClave-2027")
                .andExpect(flash().attribute("mensajeError", containsString("no coinciden")));
        cambiar(CLAVE, CLAVE, CLAVE)
                .andExpect(flash().attribute("mensajeError", containsString("distinta")));
        assertThat(passwordGuardada()).isEqualTo(antes);
    }

    @Test
    void sinSesionNoSeCambiaNingunaContrasena() throws Exception {
        mockMvc.perform(post("/user/cuenta/password").with(csrf())
                        .param("actual", CLAVE).param("nueva", "OtraClave-2026").param("repetida", "OtraClave-2026"))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void miCuentaEnsenaElFormularioYElMedidor() throws Exception {
        mockMvc.perform(get("/user/cuenta").with(marta()))
                .andExpect(content().string(containsString("Cambiar contraseña")))
                .andExpect(content().string(containsString("data-fuerza-de=\"nueva\"")));
    }

    // --- Freno a la fuerza bruta ---

    @Test
    void trasCincoFallosSeguidosElEmailQuedaBloqueado() throws Exception {
        for (int i = 1; i < 5; i++) {
            entrar(MARTA, "no-es-esta").andExpect(redirectedUrl("/login?error"));
        }
        entrar(MARTA, "no-es-esta").andExpect(redirectedUrl("/login?bloqueado"));
        // Bloqueado, ni siquiera la contraseña buena abre la sesión
        entrar(MARTA, CLAVE).andExpect(unauthenticated()).andExpect(redirectedUrl("/login?bloqueado"));
    }

    @Test
    void elBloqueoNoRevelaSiLaCuentaExiste() throws Exception {
        for (int i = 1; i < 5; i++) {
            entrar("nadie@example.com", "x").andExpect(redirectedUrl("/login?error"));
        }
        entrar("nadie@example.com", "x").andExpect(redirectedUrl("/login?bloqueado"));
    }

    @Test
    void entrarBienBorraLosFallosAnteriores() throws Exception {
        for (int i = 1; i < 5; i++) {
            entrar(MARTA, "no-es-esta");
        }
        entrar(MARTA, CLAVE).andExpect(authenticated());
        entrar(MARTA, "no-es-esta").andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void elBloqueoTerminaPasadoElTiempo() {
        Instant inicio = Instant.parse("2026-11-06T10:00:00Z");
        IntentosDeAcceso reloj1 = new IntentosDeAcceso(3, Duration.ofMinutes(5), Clock.fixed(inicio, ZoneOffset.UTC));
        reloj1.registrarFallo("a@b.c");
        reloj1.registrarFallo("a@b.c");
        assertThat(reloj1.registrarFallo("A@B.C ")).isTrue();
        assertThat(reloj1.bloqueado("a@b.c")).isTrue();

        IntentosDeAcceso despues = new IntentosDeAcceso(3, Duration.ofMinutes(5),
                Clock.fixed(inicio.plus(Duration.ofMinutes(6)), ZoneOffset.UTC));
        assertThat(despues.bloqueado("a@b.c")).isFalse();
    }

    @Test
    void elLoginExplicaElBloqueo() throws Exception {
        mockMvc.perform(get("/login").param("bloqueado", ""))
                .andExpect(content().string(containsString("Demasiados intentos fallidos")));
    }

    // --- Recordarme ---

    @Test
    void recordarmeGuardaUnaCookieSoloSiSeMarca() throws Exception {
        mockMvc.perform(post("/login").with(csrf()).param("email", MARTA).param("password", CLAVE).param("recordarme", "true"))
                .andExpect(authenticated())
                .andExpect(cookie().exists("recordarme"))
                .andExpect(cookie().httpOnly("recordarme", true));
        entrar(MARTA, CLAVE).andExpect(cookie().doesNotExist("recordarme"));
    }

    // --- Volver a la página de origen ---

    @Test
    void trasEntrarVuelveALaPaginaDeLaQueVenia() throws Exception {
        mockMvc.perform(post("/login").with(csrf()).param("email", MARTA).param("password", CLAVE).param("volver", "/libros/3"))
                .andExpect(redirectedUrl("/libros/3"));
    }

    @Test
    void noVuelveAOtrasWebs() throws Exception {
        for (String fuera : new String[] {"//ejemplo.com", "https://ejemplo.com", "/\\ejemplo.com", "/login?error"}) {
            mockMvc.perform(post("/login").with(csrf()).param("email", MARTA).param("password", CLAVE).param("volver", fuera))
                    .andExpect(redirectedUrl("/inicio"));
        }
        assertThat(AccesoCorrecto.esRutaPropia("/libros?q=a")).isTrue();
        assertThat(AccesoCorrecto.esRutaPropia("/libros\r\nSet-Cookie: x")).isFalse();
    }

    @Test
    void elBotonDeEntrarRecuerdaLaPaginaActual() throws Exception {
        mockMvc.perform(get("/libros/3"))
                .andExpect(content().string(containsString("/login?volver=/libros/3")));
    }

    // --- Renovar la sesión ---

    @Test
    void renovarLaSesionSoloConSesion() throws Exception {
        mockMvc.perform(get("/sesion/renovar").with(marta())).andExpect(status().isNoContent());
        mockMvc.perform(get("/sesion/renovar")).andExpect(redirectedUrl("/login"));
    }
}
