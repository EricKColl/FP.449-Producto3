package edu.uoc.biblioteca.controller;
import edu.uoc.biblioteca.model.EstadoReserva;
import edu.uoc.biblioteca.model.Prestamo;
import edu.uoc.biblioteca.model.Reserva;
import edu.uoc.biblioteca.repository.PrestamoRepository;
import edu.uoc.biblioteca.repository.ReservaRepository;
import edu.uoc.biblioteca.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static
        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * Rutas /user/* con cuentas reales de los datos de prueba: Laura (1), Javier (2, con un préstamo vencido)
 * y Daniel (6). «El capitán Alatriste» (8) no tiene ejemplares; «El juego del ángel» (14) sí.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class RutasUsuarioTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserDetailsService cuentas;
    @Autowired
    private PrestamoRepository prestamoRepository;
    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    private RequestPostProcessor laura() {
        return
                SecurityMockMvcRequestPostProcessors.user(cuentas.loadUserByUsername("laura.martinez@example.com"));
    }
    private RequestPostProcessor javier() {
        return
                SecurityMockMvcRequestPostProcessors.user(cuentas.loadUserByUsername("javier.lopez@example.com"));
    }
    private RequestPostProcessor daniel() {
        return
                SecurityMockMvcRequestPostProcessors.user(cuentas.loadUserByUsername("daniel.navarro@example.com"));
    }
    @Test
    void sinSesionTodasPidenIniciarSesion() throws Exception {
        for (String ruta : new String[] {"/user/prestamos", "/user/reservas", "/user/favoritos"}) {
            mockMvc.perform(get(ruta)).andExpect(redirectedUrl("/login"));
        }
    }
    @Test
    void cadaUnoVeSoloSusPrestamos() throws Exception {
        mockMvc.perform(get("/user/prestamos").with(laura()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Cien años de soledad"),
                        containsString("Mis préstamos"),
                        not(containsString("El señor de los anillos")))));

    }
    @Test
    void unUsuarioPideUnLibroParaSiMismo() throws Exception {
// Aunque alguien añada otro usuarioId a la petición, el préstamo es para la cuenta de la sesión
        mockMvc.perform(post("/user/prestamos").with(laura()).with(csrf())
                        .param("libroId", "14").param("usuarioId", "2"))
                .andExpect(redirectedUrl("/user/prestamos"))
                .andExpect(flash().attributeExists("mensajeExito"));
        List<Prestamo> deLaura = prestamoRepository.findByUsuarioIdOrderByFechaPrestamoDescIdDesc(1L);
        assertThat(deLaura.getFirst().getLibro().getId()).isEqualTo(14L);
    }
    @Test
    void conUnPrestamoVencidoNoSePideOtro() throws Exception {
        long antes = prestamoRepository.count();
        mockMvc.perform(post("/user/prestamos").with(javier()).with(csrf()).param("libroId", "14"))
                .andExpect(redirectedUrl("/libros/14"))
                .andExpect(flash().attributeExists("mensajeError"));
        assertThat(prestamoRepository.count()).isEqualTo(antes);
    }
    @Test
    void cadaUnoVeSoloSusReservas() throws Exception {
        mockMvc.perform(get("/user/reservas").with(laura()))
                .andExpect(content().string(allOf(containsString("El capitán Alatriste"),
                        containsString("Cancelar"))));
        mockMvc.perform(get("/user/reservas").with(javier()))
                .andExpect(content().string(allOf(containsString("No tienes reservas"),
                        not(containsString("El capitán Alatriste")))));
    }
    @Test
    void reservaUnLibroSinEjemplares() throws Exception {
        mockMvc.perform(post("/user/reservas").with(daniel()).with(csrf()).param("libroId", "8"))
                .andExpect(redirectedUrl("/user/reservas"));
        assertThat(reservaRepository.existsByUsuarioIdAndLibroIdAndEstado(6L, 8L,
                EstadoReserva.ACTIVA)).isTrue();
    }
    @Test
    void noReservaUnLibroConEjemplares() throws Exception {
        long antes = reservaRepository.count();
        mockMvc.perform(post("/user/reservas").with(daniel()).with(csrf()).param("libroId", "14"))
                .andExpect(redirectedUrl("/libros/14"))
                .andExpect(flash().attributeExists("mensajeError"));
        assertThat(reservaRepository.count()).isEqualTo(antes);
    }
    @Test
    void noReservaDosVecesElMismoLibro() throws Exception {
        long antes = reservaRepository.count();
        mockMvc.perform(post("/user/reservas").with(laura()).with(csrf()).param("libroId", "8"))
                .andExpect(flash().attributeExists("mensajeError"));
        assertThat(reservaRepository.count()).isEqualTo(antes);
    }
    @Test
    void cancelaSuReserva() throws Exception {
        mockMvc.perform(post("/user/reservas/1/cancelar").with(laura()).with(csrf()))
                .andExpect(redirectedUrl("/user/reservas"))
                .andExpect(flash().attributeExists("mensajeExito"));
        Reserva reserva = reservaRepository.findById(1L).orElseThrow();
        assertThat(reserva.isActiva()).isFalse();
    }
    @Test
    void nadieCancelaLaReservaDeOtro() throws Exception {
        mockMvc.perform(post("/user/reservas/1/cancelar").with(daniel()).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(reservaRepository.findById(1L).orElseThrow().isActiva()).isTrue();
    }
    @Test
    void marcaYQuitaFavoritosSoloDeSuCuenta() throws Exception {
        mockMvc.perform(post("/user/favoritos/14").with(daniel()).with(csrf()))
                .andExpect(redirectedUrl("/libros/14"));
        assertThat(usuarioRepository.findById(6L).orElseThrow().getFavoritos()).extracting("id").containsExactly(14L);
        mockMvc.perform(get("/user/favoritos").with(daniel()))
                .andExpect(content().string(containsString("El juego del ángel")));
        mockMvc.perform(get("/user/favoritos").with(laura()))
                .andExpect(content().string(allOf(containsString("1984"), not(containsString("El juego del ángel")))));
                        mockMvc.perform(post("/user/favoritos/14/quitar").with(daniel()).with(csrf()))
                                .andExpect(redirectedUrl("/user/favoritos"));
        assertThat(usuarioRepository.findById(6L).orElseThrow().getFavoritos()).isEmpty();
    }
    @Test
    void laFichaEnsenaLosBotonesSegunElLibro() throws Exception {
        mockMvc.perform(get("/libros/14").with(laura()))
                .andExpect(content().string(allOf(containsString("Pedir prestado"),
                        not(containsString("Reservar")), containsString("Añadir a favoritos"))));
        mockMvc.perform(get("/libros/8").with(laura()))
                .andExpect(content().string(allOf(containsString("Reservar"), not(containsString("Pedir prestado")))));
                        mockMvc.perform(get("/libros/6").with(laura()))
                                .andExpect(content().string(containsString("Quitar de favoritos")));
        mockMvc.perform(get("/libros/14"))
                .andExpect(content().string(allOf(not(containsString("Pedir prestado")),
                        not(containsString("favoritos")))));
    }
    @Test
    void elMenuEnsenaLasOpcionesDelUsuario() throws Exception {
        mockMvc.perform(get("/").with(laura()))
                .andExpect(content().string(allOf(
                        containsString("Mis préstamos"),
                        containsString("Mis reservas"),
                        containsString("Mis favoritos"),
                        not(containsString("Administración")))));

    }
}