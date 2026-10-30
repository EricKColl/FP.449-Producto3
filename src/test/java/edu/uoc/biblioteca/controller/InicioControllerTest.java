package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.security.SecurityConfig;
import edu.uoc.biblioteca.service.PanelService;
import edu.uoc.biblioteca.service.ResumenPanel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(InicioController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class InicioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PanelService panelService;

    @Test
    void inicioMuestraElPanelConLasCifras() throws Exception {
        given(panelService.obtenerResumen()).willReturn(new ResumenPanel(14, 11, 6, 7, 2, 11));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("resumen"))
                .andExpect(content().string(allOf(
                        containsString("Libros en el catálogo"),
                        containsString(">14<"),
                        containsString("2 vencidos"))));
    }

    @Test
    void sinPrestamosVencidosNoSeMuestraElAviso() throws Exception {
        given(panelService.obtenerResumen()).willReturn(new ResumenPanel(14, 11, 6, 5, 0, 9));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("vencidos"))));
    }
}
