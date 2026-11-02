package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.model.Rol;
import edu.uoc.biblioteca.model.Usuario;
import edu.uoc.biblioteca.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Registro con las vistas y los filtros de seguridad reales. Cada test deshace sus cambios en H2. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class RegistroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void elFormularioYLosEnlacesDeRegistroSonPublicos() throws Exception {
        mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().attributeExists("usuario"))
                .andExpect(content().string(containsString("action=\"/registro\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString("name=\"id\""))))
                .andExpect(content().string(not(containsString("name=\"rol\""))))
                .andExpect(content().string(not(containsString("name=\"fechaAlta\""))));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/registro\"")));
        mockMvc.perform(get("/login?registrado"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/registro\"")))
                .andExpect(content().string(containsString("Cuenta creada correctamente. Ya puedes iniciar sesión.")));
    }

    @Test
    void registraUnSocioNormalizaSusDatosYCifraLaContrasena() throws Exception {
        long antes = usuarioRepository.count();
        LocalDate fechaAntes = LocalDate.now();

        mockMvc.perform(registro("Clave1234", "Clave1234").with(csrf()))
                .andExpect(redirectedUrl("/login?registrado"))
                .andExpect(unauthenticated());

        usuarioRepository.flush();
        assertThat(usuarioRepository.count()).isEqualTo(antes + 1);
        Usuario nuevo = usuarioRepository.findByDni("11223344B").orElseThrow();
        assertThat(nuevo.getEmail()).isEqualTo("clara.ruiz@example.com");
        assertThat(nuevo.getTelefono()).isNull();
        assertThat(nuevo.getRol()).isEqualTo(Rol.USER);
        assertThat(nuevo.getFechaAlta()).isBetween(fechaAntes, LocalDate.now());
        assertThat(nuevo.getPassword()).hasSize(60).isNotEqualTo("Clave1234");
        assertThat(passwordEncoder.matches("Clave1234", nuevo.getPassword())).isTrue();

        mockMvc.perform(formLogin("/login").userParameter("email")
                        .user("clara.ruiz@example.com").password("Clave1234"))
                .andExpect(authenticated().withUsername("clara.ruiz@example.com").withRoles("USER"));
    }

    @Test
    void noRegistraDatosPersonalesInvalidos() throws Exception {
        long antes = usuarioRepository.count();

        mockMvc.perform(post("/registro").with(csrf())
                        .param("nombre", "")
                        .param("apellidos", "")
                        .param("dni", "123")
                        .param("email", "no-es-un-email")
                        .param("telefono", "1".repeat(21))
                        .param("password", "Clave1234")
                        .param("confirmarPassword", "Clave1234"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().attributeHasFieldErrors("usuario", "nombre", "apellidos", "dni", "email", "telefono"))
                .andExpect(content().string(containsString("El nombre es obligatorio")));

        // Un DNI vacío solo muestra un aviso, no también el del formato
        mockMvc.perform(registro("", "clara.ruiz@example.com", "Clave1234", "Clave1234").with(csrf()))
                .andExpect(model().attributeErrorCount("usuario", 1))
                .andExpect(content().string(containsString("El DNI es obligatorio")));

        assertThat(usuarioRepository.count()).isEqualTo(antes);
    }

    @Test
    void noAdmiteDniNiEmailRepetidosAunqueCambienLasMayusculas() throws Exception {
        long antes = usuarioRepository.count();

        mockMvc.perform(registro("12345678z", "clara.ruiz@example.com", "Clave1234", "Clave1234").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().attributeHasFieldErrorCode("usuario", "dni", "dni.repetido"))
                .andExpect(model().attributeErrorCount("usuario", 1));
        mockMvc.perform(registro("11223344B", "LAURA.MARTINEZ@example.com", "Clave1234", "Clave1234").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().attributeHasFieldErrorCode("usuario", "email", "email.repetido"))
                .andExpect(model().attributeErrorCount("usuario", 1));

        assertThat(usuarioRepository.count()).isEqualTo(antes);
    }

    @Test
    void exigeContrasenaYRespetaElLimiteDeBytesDeBcrypt() throws Exception {
        long antes = usuarioRepository.count();
        for (String password : new String[]{null, "", "        "}) {
            mockMvc.perform(registro(password, password).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("registro"))
                    .andExpect(model().attributeHasFieldErrorCode("usuario", "password", "password.vacia"));
        }
        // 37 letras acentuadas ocupan 74 bytes en UTF-8, aunque sean menos de 72 caracteres.
        for (String password : new String[]{"corta", "a".repeat(73), "á".repeat(37)}) {
            mockMvc.perform(registro(password, password).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrorCode("usuario", "password", "password.longitud"));
        }
        assertThat(usuarioRepository.count()).isEqualTo(antes);

        // La frontera de 72 bytes sí se admite, también con caracteres de varios bytes.
        String limite = "á".repeat(36);
        mockMvc.perform(registro(limite, limite).with(csrf()))
                .andExpect(redirectedUrl("/login?registrado"));
        Usuario nuevo = usuarioRepository.findByDni("11223344B").orElseThrow();
        assertThat(passwordEncoder.matches(limite, nuevo.getPassword())).isTrue();
    }

    @Test
    void exigeConfirmarLaContrasenaSinDevolverlaAlFormulario() throws Exception {
        long antes = usuarioRepository.count();
        String password = "MiClaveSegura1234";
        for (String confirmacion : new String[]{null, "", "OtraClave1234"}) {
            mockMvc.perform(registro(password, confirmacion).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("registro"))
                    .andExpect(model().attributeHasFieldErrorCode("usuario", "password", "password.confirmacion"))
                    .andExpect(content().string(containsString("Las contraseñas no coinciden")))
                    .andExpect(content().string(not(containsString(password))))
                    .andExpect(content().string(not(containsString("OtraClave1234"))));
        }
        assertThat(usuarioRepository.count()).isEqualTo(antes);
    }

    @Test
    void elFormularioNoPermiteElegirRolIdentificadorNiFechaDeAlta() throws Exception {
        long antes = usuarioRepository.count();
        Usuario admin = usuarioRepository.findById(7L).orElseThrow();
        String passwordAdmin = admin.getPassword();
        LocalDate fechaAntes = LocalDate.now();

        mockMvc.perform(registro("Clave1234", "Clave1234").with(csrf())
                        .param("id", "7")
                        .param("rol", "ADMIN")
                        .param("fechaAlta", "no-es-una-fecha"))
                .andExpect(redirectedUrl("/login?registrado"));

        usuarioRepository.flush();
        Usuario nuevo = usuarioRepository.findByDni("11223344B").orElseThrow();
        assertThat(usuarioRepository.count()).isEqualTo(antes + 1);
        assertThat(nuevo.getId()).isNotEqualTo(7L);
        assertThat(nuevo.getRol()).isEqualTo(Rol.USER);
        assertThat(nuevo.getFechaAlta()).isBetween(fechaAntes, LocalDate.now());
        assertThat(admin.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(admin.getEmail()).isEqualTo("admin@example.com");
        assertThat(admin.getPassword()).isEqualTo(passwordAdmin);

        mockMvc.perform(formLogin("/login").userParameter("email")
                        .user("clara.ruiz@example.com").password("Clave1234"))
                .andExpect(authenticated().withRoles("USER"));
    }

    @Test
    void sinTokenCsrfValidoNoSePuedeRegistrarNadie() throws Exception {
        long antes = usuarioRepository.count();
        mockMvc.perform(registro("Clave1234", "Clave1234"))
                .andExpect(status().isForbidden());
        mockMvc.perform(registro("Clave1234", "Clave1234").with(csrf().useInvalidToken()))
                .andExpect(status().isForbidden());
        assertThat(usuarioRepository.count()).isEqualTo(antes);
    }

    @Test
    void quienYaTieneSesionVuelveAlInicioSinCrearOtraCuenta() throws Exception {
        long antes = usuarioRepository.count();
        for (String rol : new String[]{"USER", "ADMIN"}) {
            mockMvc.perform(get("/registro").with(user("sesion@example.com").roles(rol)))
                    .andExpect(redirectedUrl("/"));
            mockMvc.perform(registro("Clave1234", "Clave1234").with(csrf())
                            .with(user("sesion@example.com").roles(rol)))
                    .andExpect(redirectedUrl("/"));
        }
        assertThat(usuarioRepository.count()).isEqualTo(antes);
    }

    private MockHttpServletRequestBuilder registro(String password, String confirmarPassword) {
        return registro("11223344b", "Clara.Ruiz@Example.com", password, confirmarPassword);
    }

    private MockHttpServletRequestBuilder registro(String dni, String email, String password, String confirmarPassword) {
        MockHttpServletRequestBuilder formulario = post("/registro")
                .param("nombre", "Clara")
                .param("apellidos", "Ruiz Martín")
                .param("dni", dni)
                .param("email", email)
                .param("telefono", "");
        if (password != null) {
            formulario.param("password", password);
        }
        if (confirmarPassword != null) {
            formulario.param("confirmarPassword", confirmarPassword);
        }
        return formulario;
    }
}
