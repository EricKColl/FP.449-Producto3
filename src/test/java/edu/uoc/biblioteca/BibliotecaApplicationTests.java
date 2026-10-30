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
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("resumen"))
                .andExpect(content().string(allOf(
                        containsString("Sistema de Gestión"),
                        containsString("2 vencidos"))));
    }

    @Test
    void unaRutaInexistenteMuestraLaPaginaDeError() throws Exception {
        mockMvc.perform(get("/no-existe"))
                .andExpect(status().isNotFound());
    }
}
