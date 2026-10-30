package edu.uoc.biblioteca.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
@WithMockUser(roles = "ADMIN")
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listaLosUsuarios() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuarios/lista"))
                .andExpect(model().attribute("usuarios", hasSize(7)))
                .andExpect(content().string(containsString("Laura Martínez Soler")));
    }

    @Test
    void elDetalleMuestraSuHistorialDePrestamos() throws Exception {
        mockMvc.perform(get("/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuarios/detalle"))
                .andExpect(model().attribute("usuario", hasProperty("prestamos", hasSize(3))))
                .andExpect(content().string(containsString("Cien años de soledad")))
                .andExpect(content().string(containsString("El hobbit")))
                .andExpect(content().string(containsString("Activo")))
                .andExpect(content().string(containsString("Devuelto")));
    }

    @Test
    void unUsuarioQueNoExisteDevuelve404() throws Exception {
        mockMvc.perform(get("/usuarios/999"))
                .andExpect(status().isNotFound());
    }
}
