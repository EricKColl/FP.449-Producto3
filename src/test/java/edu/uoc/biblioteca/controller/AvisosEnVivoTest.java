package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.repository.ReservaRepository;
import edu.uoc.biblioteca.service.AvisoService;
import edu.uoc.biblioteca.service.AvisosCambiados;
import edu.uoc.biblioteca.service.AvisosEnVivo;
import edu.uoc.biblioteca.service.PrestamoService;
import edu.uoc.biblioteca.service.ReservaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

/** Avisos en tiempo real: la conexión SSE y los eventos que la alimentan. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
@RecordApplicationEvents
class AvisosEnVivoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService cuentas;

    @Autowired
    private PrestamoService prestamoService;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private AvisoService avisoService;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private ApplicationEvents eventos;

    @Test
    void sinSesionNoHayFlujoDeAvisos() throws Exception {
        mockMvc.perform(get("/avisos/flujo")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void conSesionSeAbreUnFlujoDeEventos() throws Exception {
        mockMvc.perform(get("/avisos/flujo").with(user(cuentas.loadUserByUsername("laura.martinez@example.com"))))
                .andExpect(request().asyncStarted())
                .andExpect(content().string(containsString("event:conectado")));
    }

    @Test
    void cadaOperacionPublicaUnCambioConSuLibroYSuSocio() {
        prestamoService.registrarDevolucion(9L);
        reservaService.cancelar(1L, 1L);
        assertThat(eventos.stream(AvisosCambiados.class))
                .containsExactly(new AvisosCambiados(8L, 5L), new AvisosCambiados(8L, 1L));
    }

    @Test
    void lasConexionesSeRegistranYElAvisoNoFallaSinPaginasAbiertas() {
        // Instancia propia (sin @Async) para que el envío sea inmediato
        AvisosEnVivo enVivo = new AvisosEnVivo(avisoService, reservaRepository);
        enVivo.conectar(1L, false);
        enVivo.conectar(7L, true);
        assertThat(enVivo.conexiones()).isEqualTo(2);
        enVivo.alCambiar(new AvisosCambiados(8L, 5L));
        enVivo.alCambiar(new AvisosCambiados(null, 99L));
        assertThat(enVivo.conexiones()).isEqualTo(2);
    }
}
