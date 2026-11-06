package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.model.Prestamo;
import edu.uoc.biblioteca.repository.PrestamoRepository;
import edu.uoc.biblioteca.service.AnioLectorService;
import edu.uoc.biblioteca.service.AnioLectorService.AnioLector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * «Tu año lector». Los datos de prueba tienen fechas relativas a hoy, así que las cifras esperadas se calculan
 * aquí con los préstamos del año en curso: el test vale cualquier día del año.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class AnioLectorTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService cuentas;

    @Autowired
    private AnioLectorService anioLectorService;

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Test
    void elResumenCuentaLosPrestamosDelAnioEnCurso() {
        LocalDate inicio = LocalDate.of(LocalDate.now().getYear(), 1, 1);
        long esperados = prestamoRepository.findByUsuarioIdOrderByFechaPrestamoDescIdDesc(1L).stream()
                .map(Prestamo::getFechaPrestamo).filter(f -> !f.isBefore(inicio)).count();

        AnioLector anio = anioLectorService.resumen(1L);

        assertThat(anio.anio()).isEqualTo(LocalDate.now().getYear());
        assertThat(anio.prestamos()).isEqualTo(esperados);
        assertThat(anio.meses()).hasSize(12);
        assertThat(anio.meses().stream().mapToLong(m -> m.valor()).sum()).isEqualTo(esperados);
        assertThat(anio.superaA()).isBetween(0, 100);
        assertThat(anio.dias()).isGreaterThanOrEqualTo(esperados);
    }

    @Test
    void elSocioVeSuHistoria() throws Exception {
        mockMvc.perform(get("/user/anio-lector").with(user(cuentas.loadUserByUsername("laura.martinez@example.com"))))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("data-historia"),
                        containsString("Hola, Laura"), containsString("Tu nivel lector"))));
    }

    @Test
    void esSoloParaSocios() throws Exception {
        mockMvc.perform(get("/user/anio-lector").with(user(cuentas.loadUserByUsername("admin@example.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void elInicioDelSocioLlevaASuAnioLector() throws Exception {
        mockMvc.perform(get("/user/inicio").with(user(cuentas.loadUserByUsername("laura.martinez@example.com"))))
                .andExpect(content().string(containsString("/user/anio-lector")));
    }
}
