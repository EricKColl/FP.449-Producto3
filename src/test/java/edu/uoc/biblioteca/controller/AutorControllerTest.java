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
class AutorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listaLosAutores() throws Exception {
        mockMvc.perform(get("/autores"))
                .andExpect(status().isOk())
                .andExpect(view().name("autores/lista"))
                .andExpect(model().attribute("autores", hasSize(11)))
                .andExpect(content().string(containsString("J. R. R. Tolkien")));
    }

    @Test
    void elDetalleMuestraSusLibros() throws Exception {
        mockMvc.perform(get("/autores/10"))
                .andExpect(status().isOk())
                .andExpect(view().name("autores/detalle"))
                .andExpect(content().string(containsString("J. R. R. Tolkien")))
                .andExpect(content().string(containsString("El hobbit")))
                .andExpect(content().string(containsString("El señor de los anillos")))
                .andExpect(content().string(containsString("/libros/9")))
                .andExpect(content().string(containsString("/libros/10")));
    }

    @Test
    void unAutorQueNoExisteDevuelve404() throws Exception {
        mockMvc.perform(get("/autores/999"))
                .andExpect(status().isNotFound());
    }
}
