package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.model.Autor;
import edu.uoc.biblioteca.repository.AutorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Alta, edición y borrado de autores con los datos de prueba (perfil h2).
 * @Transactional deshace los cambios al terminar cada test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
@WithMockUser(roles = "ADMIN")
class AutorAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AutorRepository autorRepository;

    @Test
    void creaUnAutorValido() throws Exception {
        long antes = autorRepository.count();

        mockMvc.perform(post("/autores").with(csrf())
                        .param("nombre", "Ursula K.")
                        .param("apellidos", "Le Guin")
                        .param("nacionalidad", "Estadounidense")
                        .param("fechaNacimiento", "1929-10-21"))
                .andExpect(redirectedUrlPattern("/autores/*"))
                .andExpect(flash().attributeExists("mensajeExito"));

        assertThat(autorRepository.count()).isEqualTo(antes + 1);
        assertThat(autorRepository.findAll()).anySatisfy(autor -> {
            assertThat(autor.getApellidos()).isEqualTo("Le Guin");
            assertThat(autor.getFechaNacimiento()).isEqualTo(LocalDate.of(1929, 10, 21));
        });
    }

    @Test
    void noCreaUnAutorConErrores() throws Exception {
        long antes = autorRepository.count();

        mockMvc.perform(post("/autores").with(csrf())
                        .param("nombre", "")
                        .param("apellidos", "")
                        .param("fechaNacimiento", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("autores/formulario"))
                .andExpect(model().attributeHasFieldErrors("autor", "nombre", "apellidos", "fechaNacimiento"));

        assertThat(autorRepository.count()).isEqualTo(antes);
    }

    @Test
    void editaUnAutorSinCambiarSuId() throws Exception {
        mockMvc.perform(get("/autores/10/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("autores/formulario"));

        mockMvc.perform(post("/autores/10").with(csrf())
                        .param("id", "999")
                        .param("nombre", "John Ronald Reuel")
                        .param("apellidos", "Tolkien")
                        .param("nacionalidad", "Británica")
                        .param("fechaNacimiento", "1892-01-03"))
                .andExpect(redirectedUrl("/autores/10"))
                .andExpect(flash().attributeExists("mensajeExito"));

        assertThat(autorRepository.findById(10L)).get()
                .extracting(Autor::getNombre).isEqualTo("John Ronald Reuel");
        assertThat(autorRepository.findById(999L)).isEmpty();
    }

    @Test
    void eliminaUnAutorSinLibros() throws Exception {
        Autor autor = autorRepository.save(new Autor("Mary", "Shelley"));

        mockMvc.perform(post("/autores/" + autor.getId() + "/eliminar").with(csrf()))
                .andExpect(redirectedUrl("/autores"))
                .andExpect(flash().attributeExists("mensajeExito"));

        assertThat(autorRepository.findById(autor.getId())).isEmpty();
    }

    @Test
    void noEliminaUnAutorConLibros() throws Exception {
        mockMvc.perform(post("/autores/10/eliminar").with(csrf()))
                .andExpect(redirectedUrl("/autores"))
                .andExpect(flash().attributeExists("mensajeError"));

        assertThat(autorRepository.findById(10L)).isPresent();
    }

    @Test
    void rutasDeEdicionYBorradoInexistentesDevuelven404() throws Exception {
        mockMvc.perform(get("/autores/999/editar")).andExpect(status().isNotFound());
        mockMvc.perform(post("/autores/999").with(csrf()).param("nombre", "A").param("apellidos", "B"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/autores/999/eliminar").with(csrf())).andExpect(status().isNotFound());
    }
}
