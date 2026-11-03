package edu.uoc.biblioteca.security;

import edu.uoc.biblioteca.repository.LibroRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rutas administrativas y vistas con la cadena de seguridad real, sin modificar datos persistentes. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class RutasAdminTest {

    private static final String[] RUTAS = {
            "/admin/libros/nuevo", "/admin/libros/1/editar",
            "/admin/autores/nuevo", "/admin/autores/1/editar",
            "/admin/usuarios", "/admin/usuarios/1", "/admin/usuarios/nuevo", "/admin/usuarios/1/editar"};

    private static final String[] ESCRITURAS = {
            "/admin/libros", "/admin/libros/14", "/admin/libros/14/eliminar",
            "/admin/autores", "/admin/autores/1", "/admin/autores/1/eliminar",
            "/admin/usuarios", "/admin/usuarios/1", "/admin/usuarios/1/eliminar"};

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LibroRepository libroRepository;

    @Test
    void sinSesionPidenIniciarSesion() throws Exception {
        for (String ruta : RUTAS) {
            mockMvc.perform(get(ruta)).andExpect(redirectedUrl("/login"));
        }
        // Con CSRF válido comprobamos la autenticación, sin confundirla con la protección CSRF.
        for (String ruta : ESCRITURAS) {
            mockMvc.perform(post(ruta).with(csrf())).andExpect(redirectedUrl("/login"));
        }
    }

    @Test
    @WithMockUser(roles = "USER")
    void unUsuarioNoEntra() throws Exception {
        for (String ruta : RUTAS) {
            mockMvc.perform(get(ruta)).andExpect(status().isForbidden());
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unAdministradorEntra() throws Exception {
        for (String ruta : RUTAS) {
            mockMvc.perform(get(ruta)).andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser(roles = "USER")
    void unUsuarioNoBorraNada() throws Exception {
        // También cubre altas y actualizaciones directas, aunque el botón no aparezca.
        for (String ruta : ESCRITURAS) {
            mockMvc.perform(post(ruta).with(csrf())).andExpect(status().isForbidden());
        }
        assertThat(libroRepository.existsById(14L)).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void lasRutasAntiguasYaNoModificanNada() throws Exception {
        for (String ruta : new String[] {"/libros/14/eliminar", "/autores/1/eliminar", "/usuarios/1/eliminar"}) {
            mockMvc.perform(post(ruta).with(csrf())).andExpect(status().isNotFound());
        }
        assertThat(libroRepository.existsById(14L)).isTrue();
    }

    @Test
    void sinSesionNoSeVenLosBotonesDeAdministracion() throws Exception {
        mockMvc.perform(get("/libros"))
                .andExpect(content().string(allOf(containsString("Don Quijote"),
                        not(containsString("Nuevo libro")), not(containsString("Acciones")), not(containsString("/admin/")))));
        mockMvc.perform(get("/autores"))
                .andExpect(content().string(allOf(containsString("Cervantes"),
                        not(containsString("Nuevo autor")), not(containsString("Acciones")), not(containsString("/admin/")))));
        mockMvc.perform(get("/libros/1")).andExpect(content().string(not(containsString("/admin/"))));
        mockMvc.perform(get("/autores/1")).andExpect(content().string(not(containsString("/admin/"))));
    }

    @Test
    @WithMockUser(roles = "USER")
    void unUsuarioTampocoVeLosBotones() throws Exception {
        for (String ruta : new String[] {"/libros", "/autores", "/libros/1", "/autores/1"}) {
            mockMvc.perform(get(ruta)).andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("/admin/"))));
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unAdministradorVeLosBotones() throws Exception {
        mockMvc.perform(get("/libros"))
                .andExpect(content().string(allOf(containsString("Nuevo libro"),
                        containsString("/admin/libros/1/editar"), containsString("Acciones"))));
        mockMvc.perform(get("/autores"))
                .andExpect(content().string(allOf(containsString("Nuevo autor"),
                        containsString("/admin/autores/1/editar"), containsString("Acciones"))));
    }
}
