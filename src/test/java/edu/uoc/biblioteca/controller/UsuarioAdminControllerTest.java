package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.model.Usuario;
import edu.uoc.biblioteca.repository.UsuarioRepository;
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
 * Alta, edición y borrado de usuarios con los datos de prueba (perfil h2).
 * @Transactional deshace los cambios al terminar cada test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
@WithMockUser(roles = "ADMIN")
class UsuarioAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void daDeAltaUnUsuarioValidoYNormalizaDniYEmail() throws Exception {
        long antes = usuarioRepository.count();

        mockMvc.perform(post("/usuarios").with(csrf())
                        .param("nombre", "Clara")
                        .param("apellidos", "Ruiz Martín")
                        .param("dni", "11223344b")
                        .param("email", "Clara.Ruiz@Example.com")
                        .param("telefono", "")
                        .param("fechaAlta", LocalDate.now().toString())
                        .param("password", "Clave1234"))
                .andExpect(redirectedUrlPattern("/usuarios/*"))
                .andExpect(flash().attributeExists("mensajeExito"));

        assertThat(usuarioRepository.count()).isEqualTo(antes + 1);
        Usuario nuevo = usuarioRepository.findByDni("11223344B").orElseThrow();
        assertThat(nuevo.getEmail()).isEqualTo("clara.ruiz@example.com");
        assertThat(nuevo.getTelefono()).isNull();
    }

    @Test
    void noDaDeAltaUnUsuarioConErrores() throws Exception {
        long antes = usuarioRepository.count();

        mockMvc.perform(post("/usuarios").with(csrf())
                        .param("nombre", "")
                        .param("apellidos", "Sin Nombre")
                        .param("dni", "123")
                        .param("email", "no-es-un-email")
                        .param("fechaAlta", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("usuarios/formulario"))
                .andExpect(model().attributeHasFieldErrors("usuario", "nombre", "dni", "email", "fechaAlta"));

        assertThat(usuarioRepository.count()).isEqualTo(antes);
    }

    @Test
    void noAdmiteDniNiEmailDeOtroUsuario() throws Exception {
        long antes = usuarioRepository.count();

        // DNI de Laura en minúsculas y email de Javier con mayúsculas: se detectan igualmente
        mockMvc.perform(post("/usuarios").with(csrf())
                        .param("nombre", "Copia")
                        .param("apellidos", "Repetida")
                        .param("dni", "12345678z")
                        .param("email", "JAVIER.LOPEZ@example.com")
                        .param("fechaAlta", LocalDate.now().toString())
                        .param("password", "Clave1234"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("usuario", "dni", "dni.repetido"))
                .andExpect(model().attributeHasFieldErrorCode("usuario", "email", "email.repetido"));

        assertThat(usuarioRepository.count()).isEqualTo(antes);
    }

    @Test
    void editaUnUsuarioConservandoSuPropioDni() throws Exception {
        mockMvc.perform(get("/usuarios/5/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuarios/formulario"));

        mockMvc.perform(post("/usuarios/5").with(csrf())
                        .param("nombre", "Lucía")
                        .param("apellidos", "Fernández Torres")
                        .param("dni", "56781234F")
                        .param("email", "lucia.fernandez@example.com")
                        .param("telefono", "699111222")
                        .param("fechaAlta", LocalDate.now().minusDays(90).toString()))
                .andExpect(redirectedUrl("/usuarios/5"))
                .andExpect(flash().attributeExists("mensajeExito"));

        assertThat(usuarioRepository.findById(5L)).get()
                .extracting(Usuario::getTelefono).isEqualTo("699111222");
    }

    @Test
    void eliminaUnUsuarioSinPrestamos() throws Exception {
        Usuario usuario = usuarioRepository.save(
                conPassword(new Usuario("99887766K", "Temporal", "De Prueba", "temporal@example.com")));

        mockMvc.perform(post("/usuarios/" + usuario.getId() + "/eliminar").with(csrf()))
                .andExpect(redirectedUrl("/usuarios"))
                .andExpect(flash().attributeExists("mensajeExito"));

        assertThat(usuarioRepository.findById(usuario.getId())).isEmpty();
    }

    @Test
    void noEliminaUnUsuarioConPrestamos() throws Exception {
        mockMvc.perform(post("/usuarios/1/eliminar").with(csrf()))
                .andExpect(redirectedUrl("/usuarios"))
                .andExpect(flash().attributeExists("mensajeError"));

        assertThat(usuarioRepository.findById(1L)).isPresent();
    }

    private static Usuario conPassword(Usuario usuario) {
        usuario.setPassword("sin-uso-en-este-test");
        return usuario;
    }
}
