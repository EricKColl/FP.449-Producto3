package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.security.SecurityConfig;
import edu.uoc.biblioteca.service.AutorDestacado;
import edu.uoc.biblioteca.service.Cifra;
import edu.uoc.biblioteca.service.PortadaService;
import edu.uoc.biblioteca.service.PortadaService.Portada;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(InicioController.class)
@Import(SecurityConfig.class)
class InicioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortadaService portadaService;

    @Test
    void laPortadaEsPublicaYEnsenaElCatalogo() throws Exception {
        given(portadaService.obtener()).willReturn(new Portada(14, 11, 21, 15, List.of(),
                List.of(new Cifra("Fantasía", 4)), List.of(new AutorDestacado(2L, "Gabriel", "García Márquez", "Colombiana", 2))));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("portada"))
                .andExpect(content().string(allOf(
                        containsString("títulos en el catálogo"),
                        containsString(">21<"),
                        containsString("Fantasía"),
                        containsString("Gabriel García Márquez"),
                        containsString("Iniciar sesión"),
                        containsString("Crear cuenta"))));
    }

    @Test
    void laPortadaSoloEjecutaScriptsPropios() throws Exception {
        given(portadaService.obtener()).willReturn(new Portada(0, 0, 0, 0, List.of(), List.of(), List.of()));

        mockMvc.perform(get("/"))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void elAdministradorVaASuPanel() throws Exception {
        mockMvc.perform(get("/inicio")).andExpect(redirectedUrl("/admin/panel"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void elClienteVaASuInicio() throws Exception {
        mockMvc.perform(get("/inicio")).andExpect(redirectedUrl("/user/inicio"));
    }

    @Test
    void sinSesionElInicioPideEntrar() throws Exception {
        mockMvc.perform(get("/inicio")).andExpect(redirectedUrl("/login"));
    }
}
