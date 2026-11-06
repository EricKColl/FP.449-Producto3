package edu.uoc.biblioteca.controller;

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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/** Campanita de avisos de la cabecera, calculada con los préstamos y reservas de los datos de prueba. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class AvisosTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService cuentas;

    @Autowired
    private PrestamoService prestamoService;

    private RequestPostProcessor cuenta(String email) {
        return user(cuentas.loadUserByUsername(email));
    }

    @Test
    void sinSesionNoHayCampanita() throws Exception {
        mockMvc.perform(get("/libros")).andExpect(content().string(not(containsString("data-campana"))));
    }

    @Test
    void elSocioConUnPrestamoVencidoRecibeElAviso() throws Exception {
        // Javier tiene «El señor de los anillos» fuera de plazo
        mockMvc.perform(get("/libros").with(cuenta("javier.lopez@example.com")))
                .andExpect(content().string(allOf(containsString("data-campana"),
                        containsString("«El señor de los anillos» está fuera de plazo"))));
    }

    @Test
    void sinNadaPendienteLaCampanitaDiceQueEstaTodoAlDia() throws Exception {
        mockMvc.perform(get("/libros").with(cuenta("marta.sanchez@example.com")))
                .andExpect(content().string(containsString("Todo al día")));
    }

    @Test
    void avisaCuandoUnaReservaYaSePuedePedir() throws Exception {
        prestamoService.registrarDevolucion(9L);
        mockMvc.perform(get("/libros").with(cuenta("laura.martinez@example.com")))
                .andExpect(content().string(containsString("Ya puedes pedir «El capitán Alatriste»")));
    }

    @Test
    void laAdministracionVeLosPrestamosFueraDePlazo() throws Exception {
        mockMvc.perform(get("/admin/panel").with(cuenta("admin@example.com")))
                .andExpect(content().string(allOf(containsString("2 préstamos fuera de plazo"),
                        containsString("/admin/prestamos?estado=VENCIDO"))));
    }
}
