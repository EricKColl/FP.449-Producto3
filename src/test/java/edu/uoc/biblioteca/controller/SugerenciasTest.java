package edu.uoc.biblioteca.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sugerencias del buscador: JSON público, sin distinguir tildes y con un mínimo de letras. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class SugerenciasTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sugiereLibrosYAutoresSinSesionYSinTildes() throws Exception {
        mockMvc.perform(get("/libros/sugerencias").param("q", "garcia"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.autores[0].nombre", is("Gabriel García Márquez")))
                .andExpect(jsonPath("$.libros[0].autores", is("Gabriel García Márquez")));
    }

    @Test
    void primeroLosTitulosQueEmpiezanPorLoEscrito() throws Exception {
        mockMvc.perform(get("/libros/sugerencias").param("q", "el"))
                .andExpect(jsonPath("$.libros[0].titulo", is("El capitán Alatriste")))
                .andExpect(jsonPath("$.libros", hasSize(6)));
    }

    @Test
    void conMenosDeDosLetrasNoSugiereNada() throws Exception {
        mockMvc.perform(get("/libros/sugerencias").param("q", "e"))
                .andExpect(jsonPath("$.libros", hasSize(0)))
                .andExpect(jsonPath("$.autores", hasSize(0)));
    }

    @Test
    void elCatalogoTambienBuscaSinTildes() throws Exception {
        mockMvc.perform(get("/libros").param("q", "angel"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("El juego del ángel")));
    }
}
