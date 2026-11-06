package edu.uoc.biblioteca;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Arranca la aplicación completa sobre H2 con los scripts SQL de MariaDB.
 * Como Hibernate está en modo "validate", si una entidad no coincide con las tablas este test falla.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@WithMockUser(roles = "ADMIN")
class BibliotecaApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void elPanelMuestraLosDatosDeLaBaseDeDatos() throws Exception {
        mockMvc.perform(get("/admin/panel"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("panel"))
                .andExpect(content().string(allOf(
                        containsString("Panel de administración"), containsString("Nuevo préstamo"),
                        containsString("2 préstamos fuera de plazo"),
                        containsString("Javier López Ferrer"))));
    }

    @Test
    void unaRutaInexistenteMuestraLaPaginaDeError() throws Exception {
        mockMvc.perform(get("/no-existe"))
                .andExpect(status().isNotFound());
    }
}
