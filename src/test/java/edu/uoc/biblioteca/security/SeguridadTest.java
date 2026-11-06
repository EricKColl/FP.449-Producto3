package edu.uoc.biblioteca.security;

import edu.uoc.biblioteca.model.Rol;
import edu.uoc.biblioteca.model.Usuario;
import edu.uoc.biblioteca.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Login, cuentas y control de acceso por rol, con la cadena de filtros real y las cuentas de los datos de prueba.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class SeguridadTest {

    private static final String ADMIN = "admin@example.com";
    private static final String SOCIA = "laura.martinez@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserDetailsService cuentas;

    // --- Login y cierre de sesión ---

    @Test
    void elFormularioDeLoginEsPublico() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Iniciar sesión")));
    }

    @Test
    void entraConEmailYContrasenaCorrectos() throws Exception {
        mockMvc.perform(formLogin("/login").userParameter("email").user(ADMIN).password("Admin1234"))
                .andExpect(authenticated().withRoles("ADMIN"))
                .andExpect(redirectedUrl("/inicio"));
        mockMvc.perform(formLogin("/login").userParameter("email").user(SOCIA).password("Usuario1234"))
                .andExpect(authenticated().withRoles("USER"));
    }

    @Test
    void elEmailDelLoginNoDistingueMayusculas() throws Exception {
        mockMvc.perform(formLogin("/login").userParameter("email").user("Admin@Example.COM").password("Admin1234"))
                .andExpect(authenticated().withRoles("ADMIN"));
    }

    @Test
    void noEntraConContrasenaIncorrectaNiCuentaInexistente() throws Exception {
        mockMvc.perform(formLogin("/login").userParameter("email").user(ADMIN).password("otra-clave"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
        mockMvc.perform(formLogin("/login").userParameter("email").user("nadie@example.com").password("Admin1234"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void cerrarSesionVuelveAlLogin() throws Exception {
        mockMvc.perform(logout())
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?salida"));
    }

    @Test
    @WithMockUser
    void conSesionIniciadaElLoginRedirigeAlInicio() throws Exception {
        mockMvc.perform(get("/login")).andExpect(redirectedUrl("/inicio"));
    }

    // --- Sin sesión ---

    @Test
    void sinSesionSeConsultaElCatalogo() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/libros")).andExpect(status().isOk());
        mockMvc.perform(get("/libros/1")).andExpect(status().isOk());
        mockMvc.perform(get("/autores")).andExpect(status().isOk());
        mockMvc.perform(get("/autores/1")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void sinSesionLasDemasRutasPidenIniciarSesion() throws Exception {
        for (String ruta : new String[] {"/admin/libros", "/user/cuenta", "/admin/usuarios", "/admin/prestamos", "/admin/libros/nuevo", "/admin/libros/1/editar"}) {
            mockMvc.perform(get(ruta)).andExpect(redirectedUrl("/login"));
        }
    }

    @Test
    void sinSesionNoSeVenDatosDeOtrosUsuarios() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(model().attributeDoesNotExist("ultimos"))
                .andExpect(content().string(allOf(not(containsString("Últimos préstamos")), not(containsString("Administración")))));
        mockMvc.perform(get("/libros/1"))
                .andExpect(content().string(not(containsString("Historial de préstamos"))));
    }

    // --- Rol USER ---

    @Test
    @WithMockUser(roles = "USER")
    void unUsuarioNoEntraEnLasRutasDeAdministracion() throws Exception {
        for (String ruta : new String[] {"/admin/libros", "/admin/usuarios", "/admin/usuarios/1", "/admin/prestamos", "/admin/libros/nuevo", "/admin/autores/1/editar"}) {
            mockMvc.perform(get(ruta)).andExpect(status().isForbidden());
        }
    }

    @Test
    @WithMockUser(roles = "USER")
    void unUsuarioNoModificaDatos() throws Exception {
        long antes = usuarioRepository.count();
        mockMvc.perform(post("/admin/libros/1/eliminar").with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/usuarios/1/eliminar").with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/prestamos").with(csrf()).param("usuarioId", "1").param("libroId", "1"))
                .andExpect(status().isForbidden());
        assertThat(usuarioRepository.count()).isEqualTo(antes);
    }

    @Test
    void unUsuarioVeSuCuentaYElMenuDeSuRol() throws Exception {
        mockMvc.perform(get("/user/cuenta").with(user(cuentas.loadUserByUsername(SOCIA))))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Laura Martínez Soler"),
                        containsString("Mi cuenta"),
                        containsString("Cerrar sesión"),
                        not(containsString("Administración")))));
    }

    // --- Rol ADMIN ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void unAdministradorPasaElControlDeAcceso() throws Exception {
        mockMvc.perform(get("/admin/usuarios")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/prestamos")).andExpect(status().isOk());
        // Pasa el filtro: la ruta todavía no existe, de ahí el 404 y no un 403
        mockMvc.perform(get("/admin/no-existe")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unAdministradorVeElMenuYElPanelCompletos() throws Exception {
        mockMvc.perform(get("/admin/panel"))
                .andExpect(model().attributeExists("panel"))
                .andExpect(content().string(allOf(
                        containsString("Administración"),
                        containsString("Últimos préstamos"),
                        containsString("Cerrar sesión"))));
    }

    @Test
    void elAdministradorNoTienePrestamosReservasNiFavoritosPropios() throws Exception {
        var admin = user(cuentas.loadUserByUsername(ADMIN));
        // Mi cuenta es de cualquier sesión; el resto de /user/* es del cliente
        mockMvc.perform(get("/user/cuenta").with(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Permisos del administrador")));
        for (String ruta : new String[] {"/user/inicio", "/user/prestamos", "/user/reservas", "/user/favoritos"}) {
            mockMvc.perform(get(ruta).with(admin)).andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/libros/14").with(admin))
                .andExpect(content().string(allOf(not(containsString("Pedir prestado")),
                        not(containsString("/user/favoritos")), not(containsString("Mis préstamos")))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sinTokenCsrfNoSeModificaNada() throws Exception {
        mockMvc.perform(post("/admin/libros/1/eliminar")).andExpect(status().isForbidden());
    }

    // --- Cuentas ---

    @Test
    void lasContrasenasDeLosDatosDePruebaEstanCifradas() {
        Usuario admin = usuarioRepository.findByEmail(ADMIN).orElseThrow();
        assertThat(admin.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(admin.getPassword()).startsWith("$2a$").doesNotContain("Admin1234");
        assertThat(passwordEncoder.matches("Admin1234", admin.getPassword())).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void elAltaCifraLaContrasenaYAsignaElRol() throws Exception {
        altaDeClara("Clave1234", "USER").andExpect(redirectedUrlPattern("/admin/usuarios/*"));

        Usuario clara = usuarioRepository.findByEmail("clara@example.com").orElseThrow();
        assertThat(clara.getRol()).isEqualTo(Rol.USER);
        assertThat(passwordEncoder.matches("Clave1234", clara.getPassword())).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void elAltaExigeUnaContrasenaValida() throws Exception {
        long antes = usuarioRepository.count();
        altaDeClara("", "USER").andExpect(model().attributeHasFieldErrorCode("usuario", "password", "password.vacia"));
        altaDeClara("corta", "USER").andExpect(model().attributeHasFieldErrorCode("usuario", "password", "password.longitud"));
        assertThat(usuarioRepository.count()).isEqualTo(antes);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void elFormularioNuncaMuestraLaContrasena() throws Exception {
        Usuario laura = usuarioRepository.findByEmail(SOCIA).orElseThrow();
        mockMvc.perform(get("/admin/usuarios/" + laura.getId() + "/editar"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(laura.getPassword()))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editarSinContrasenaConservaLaActual() throws Exception {
        Usuario laura = usuarioRepository.findByEmail(SOCIA).orElseThrow();
        String anterior = laura.getPassword();

        editar(laura, "", "USER").andExpect(redirectedUrl("/admin/usuarios/" + laura.getId()));

        assertThat(usuarioRepository.findByEmail(SOCIA).orElseThrow().getPassword()).isEqualTo(anterior);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editarConContrasenaLaCambiaCifrada() throws Exception {
        Usuario laura = usuarioRepository.findByEmail(SOCIA).orElseThrow();

        editar(laura, "NuevaClave99", "USER").andExpect(redirectedUrl("/admin/usuarios/" + laura.getId()));

        assertThat(passwordEncoder.matches("NuevaClave99", usuarioRepository.findByEmail(SOCIA).orElseThrow().getPassword())).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void elFormularioNoPermiteQuitarElRolAlUltimoAdministrador() throws Exception {
        Usuario admin = usuarioRepository.findByEmail(ADMIN).orElseThrow();

        editar(admin, "", "USER").andExpect(model().attributeHasFieldErrorCode("usuario", "rol", "rol.ultimoAdmin"));

        assertThat(usuarioRepository.findByEmail(ADMIN).orElseThrow().getRol()).isEqualTo(Rol.ADMIN);
    }

    @Test
    @WithMockUser(username = ADMIN, roles = "ADMIN")
    void nadieEliminaLaCuentaConLaQueHaEntrado() throws Exception {
        Usuario admin = usuarioRepository.findByEmail(ADMIN).orElseThrow();

        mockMvc.perform(post("/admin/usuarios/" + admin.getId() + "/eliminar").with(csrf()))
                .andExpect(flash().attributeExists("mensajeError"));

        assertThat(usuarioRepository.findByEmail(ADMIN)).isPresent();
    }

    private ResultActions altaDeClara(String password, String rol) throws Exception {
        return mockMvc.perform(post("/admin/usuarios").with(csrf())
                .param("nombre", "Clara")
                .param("apellidos", "Ruiz Martín")
                .param("dni", "11223344B")
                .param("email", "clara@example.com")
                .param("fechaAlta", LocalDate.now().toString())
                .param("password", password)
                .param("rol", rol));
    }

    private ResultActions editar(Usuario usuario, String password, String rol) throws Exception {
        return mockMvc.perform(post("/admin/usuarios/" + usuario.getId()).with(csrf())
                .param("nombre", usuario.getNombre())
                .param("apellidos", usuario.getApellidos())
                .param("dni", usuario.getDni())
                .param("email", usuario.getEmail())
                .param("fechaAlta", usuario.getFechaAlta().toString())
                .param("password", password)
                .param("rol", rol));
    }
}
