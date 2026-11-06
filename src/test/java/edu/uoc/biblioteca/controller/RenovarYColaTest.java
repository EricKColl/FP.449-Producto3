package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.model.EstadoReserva;
import edu.uoc.biblioteca.model.Prestamo;
import edu.uoc.biblioteca.repository.PrestamoRepository;
import edu.uoc.biblioteca.repository.ReservaRepository;
import edu.uoc.biblioteca.service.PrestamoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Renovar un préstamo y la cola de reservas: los ejemplares que vuelven van primero a quien reservó antes.
 * Datos de prueba: Laura tiene el préstamo 7 en plazo y es la primera (y única) de la cola de «El capitán
 * Alatriste» (libro 8, un ejemplar), que tiene Lucía en el préstamo 9. Javier tiene el préstamo 6 vencido.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class RenovarYColaTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService cuentas;

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private PrestamoService prestamoService;

    private RequestPostProcessor cuenta(String email) {
        return user(cuentas.loadUserByUsername(email));
    }

    private RequestPostProcessor laura() {
        return cuenta("laura.martinez@example.com");
    }

    private RequestPostProcessor daniel() {
        return cuenta("daniel.navarro@example.com");
    }

    // --- Renovar ---

    @Test
    void renuevaUnaVezSuPrestamoEnPlazo() throws Exception {
        LocalDate antes = prestamoRepository.findById(7L).orElseThrow().getFechaDevolucionPrevista();

        mockMvc.perform(post("/user/prestamos/7/renovar").with(laura()).with(csrf()))
                .andExpect(redirectedUrl("/user/prestamos"))
                .andExpect(flash().attribute("mensajeExito", containsString("Renovado")));
        Prestamo renovado = prestamoRepository.findById(7L).orElseThrow();
        assertThat(renovado.getFechaDevolucionPrevista()).isEqualTo(antes.plusDays(Prestamo.DIAS_PRESTAMO));
        assertThat(renovado.isRenovado()).isTrue();

        // La segunda vez ya no
        mockMvc.perform(post("/user/prestamos/7/renovar").with(laura()).with(csrf()))
                .andExpect(flash().attribute("mensajeError", containsString("una vez")));
        assertThat(prestamoRepository.findById(7L).orElseThrow().getFechaDevolucionPrevista())
                .isEqualTo(antes.plusDays(Prestamo.DIAS_PRESTAMO));
    }

    @Test
    void noSeRenuevaUnPrestamoVencidoNiUnoQueOtrosEsperan() throws Exception {
        mockMvc.perform(post("/user/prestamos/6/renovar").with(cuenta("javier.lopez@example.com")).with(csrf()))
                .andExpect(flash().attribute("mensajeError", containsString("fuera de plazo")));
        mockMvc.perform(post("/user/prestamos/9/renovar").with(cuenta("lucia.fernandez@example.com")).with(csrf()))
                .andExpect(flash().attribute("mensajeError", containsString("esperan")));
    }

    @Test
    void nadieRenuevaElPrestamoDeOtro() throws Exception {
        LocalDate antes = prestamoRepository.findById(7L).orElseThrow().getFechaDevolucionPrevista();
        mockMvc.perform(post("/user/prestamos/7/renovar").with(daniel()).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(prestamoRepository.findById(7L).orElseThrow().getFechaDevolucionPrevista()).isEqualTo(antes);
    }

    @Test
    void misPrestamosOfreceRenovarYExplicaCuandoNo() throws Exception {
        mockMvc.perform(get("/user/prestamos").with(laura()))
                .andExpect(content().string(containsString("/user/prestamos/7/renovar")));
        mockMvc.perform(get("/user/prestamos").with(cuenta("lucia.fernandez@example.com")))
                .andExpect(content().string(allOf(not(containsString("/user/prestamos/9/renovar")),
                        containsString("Otros socios esperan este libro"))));
    }

    // --- Cola de reservas ---

    @Test
    void misReservasEnsenaElPuestoEnLaCola() throws Exception {
        mockMvc.perform(get("/user/reservas").with(laura()))
                .andExpect(content().string(containsString("Eres el primero de la cola")));
    }

    @Test
    void elEjemplarQueVuelveEsPrimeroParaQuienLoReservo() throws Exception {
        // Lucía devuelve «El capitán Alatriste»: queda un ejemplar libre, apartado para Laura
        prestamoService.registrarDevolucion(9L);

        mockMvc.perform(get("/libros/8").with(daniel()))
                .andExpect(content().string(allOf(containsString("Reservar"), not(containsString("Pedir prestado")),
                        containsString("apartados para quien lo reservó antes"))));
        mockMvc.perform(post("/user/prestamos").with(daniel()).with(csrf()).param("libroId", "8"))
                .andExpect(redirectedUrl("/libros/8"))
                .andExpect(flash().attribute("mensajeError", containsString("apartados")));

        mockMvc.perform(get("/user/reservas").with(laura()))
                .andExpect(content().string(containsString("¡Ya hay ejemplar!")));
        mockMvc.perform(post("/user/prestamos").with(laura()).with(csrf()).param("libroId", "8"))
                .andExpect(redirectedUrl("/user/prestamos"));

        // Su reserva queda atendida y sale de la cola
        assertThat(reservaRepository.existsByUsuarioIdAndLibroIdAndEstado(1L, 8L, EstadoReserva.ACTIVA)).isFalse();
        mockMvc.perform(get("/user/reservas").with(laura()))
                .andExpect(content().string(containsString("Atendida")));
    }

    @Test
    void conLosLibresApartadosSePuedeReservar() throws Exception {
        prestamoService.registrarDevolucion(9L);
        mockMvc.perform(post("/user/reservas").with(daniel()).with(csrf()).param("libroId", "8"))
                .andExpect(redirectedUrl("/user/reservas"));
        mockMvc.perform(get("/user/reservas").with(daniel()))
                .andExpect(content().string(containsString("Hay 1 persona antes que tú")));
    }
}
