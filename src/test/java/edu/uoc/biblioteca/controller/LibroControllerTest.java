package edu.uoc.biblioteca.controller;

import edu.uoc.biblioteca.repository.LibroRepository;
import edu.uoc.biblioteca.model.Autor;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Prueba el CRUD de libros con la aplicación completa y los datos de prueba (perfil h2).
 * @Transactional deshace los cambios al terminar cada test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
@WithMockUser(roles = "ADMIN")
class LibroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private EntityManager entityManager;

    private void recargarDatos() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void listaLosLibros() throws Exception {
        mockMvc.perform(get("/libros"))
                .andExpect(status().isOk())
                .andExpect(view().name("libros/lista"))
                .andExpect(content().string(containsString("Don Quijote de la Mancha")));
    }

    @Test
    void buscaPorAutor() throws Exception {
        mockMvc.perform(get("/libros").param("q", "tolkien"))
                .andExpect(content().string(containsString("El hobbit")))
                .andExpect(content().string(not(containsString("Rebelión en la granja"))));
    }

    @Test
    void muestraElDetalleConSusAutores() throws Exception {
        mockMvc.perform(get("/libros/5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Neil Gaiman, Terry Pratchett")));
    }

    @Test
    void unLibroQueNoExisteDevuelve404() throws Exception {
        mockMvc.perform(get("/libros/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void creaUnLibroValido() throws Exception {
        mockMvc.perform(post("/libros").with(csrf())
                        .param("isbn", "978-84-376-0494-7")
                        .param("titulo", "Libro de prueba")
                        .param("ejemplares", "2")
                        .param("autores", "1", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("mensajeExito"));

        recargarDatos();
        assertThat(libroRepository.findByIsbn("978-84-376-0494-7")).isPresent()
                .get().satisfies(libro -> assertThat(libro.getAutores()).hasSize(2));
    }

    @Test
    void noCreaUnLibroConErrores() throws Exception {
        mockMvc.perform(post("/libros").with(csrf())
                        .param("isbn", "")
                        .param("titulo", "")
                        .param("ejemplares", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("libros/formulario"))
                .andExpect(model().attributeHasFieldErrors("libro", "isbn", "titulo", "autores"));
    }

    @Test
    void noAdmiteUnIsbnRepetido() throws Exception {
        mockMvc.perform(post("/libros").with(csrf())
                        .param("isbn", "978-84-376-2405-1")
                        .param("titulo", "Otro Quijote")
                        .param("ejemplares", "1")
                        .param("autores", "1"))
                .andExpect(view().name("libros/formulario"))
                .andExpect(model().attributeHasFieldErrorCode("libro", "isbn", "isbn.repetido"));
    }

    @Test
    void editaUnLibro() throws Exception {
        mockMvc.perform(post("/libros/14").with(csrf())
                        .param("isbn", "978-84-08-08236-1")
                        .param("titulo", "El juego del ángel (edición revisada)")
                        .param("ejemplares", "3")
                        .param("autores", "1", "2"))
                .andExpect(redirectedUrl("/libros/14"));

        recargarDatos();
        assertThat(libroRepository.findById(14L)).get()
                .satisfies(libro -> {
                    assertThat(libro.getTitulo()).isEqualTo("El juego del ángel (edición revisada)");
                    assertThat(libro.getEjemplares()).isEqualTo(3);
                    assertThat(libro.getAutores()).extracting(Autor::getId).containsExactlyInAnyOrder(1L, 2L);
                });
    }

    @Test
    void eliminaUnLibroSinPrestamos() throws Exception {
        mockMvc.perform(post("/libros/14/eliminar").with(csrf()))
                .andExpect(redirectedUrl("/libros"))
                .andExpect(flash().attributeExists("mensajeExito"));

        recargarDatos();
        assertThat(libroRepository.existsById(14L)).isFalse();
    }

    @Test
    void noEliminaUnLibroConPrestamos() throws Exception {
        mockMvc.perform(post("/libros/1/eliminar").with(csrf()))
                .andExpect(redirectedUrl("/libros"))
                .andExpect(flash().attributeExists("mensajeError"));

        assertThat(libroRepository.existsById(1L)).isTrue();
    }

    @Test
    void muestraFormularioDeAlta() throws Exception {
        mockMvc.perform(get("/libros/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("libros/formulario"))
                .andExpect(model().attributeExists("todosLosAutores"))
                .andExpect(content().string(containsString("multiple")));
    }

    @Test
    void edicionPreseleccionaLosDosAutores() throws Exception {
        mockMvc.perform(get("/libros/8/editar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"8\" selected=\"selected\"")))
                .andExpect(content().string(containsString("value=\"9\" selected=\"selected\"")));
    }

    @Test
    void buscaPorTituloEIsbnYAdmiteBusquedaVacia() throws Exception {
        mockMvc.perform(get("/libros").param("q", "  HOBBIT  "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El hobbit")))
                .andExpect(content().string(not(containsString("Buenos presagios"))));
        mockMvc.perform(get("/libros").param("q", "978-84-450-7180-9"))
                .andExpect(content().string(containsString("Buenos presagios")))
                .andExpect(content().string(not(containsString("El hobbit"))));
        mockMvc.perform(get("/libros").param("q", "  "))
                .andExpect(content().string(containsString("Don Quijote de la Mancha")));
        mockMvc.perform(get("/libros").param("q", "sin coincidencias"))
                .andExpect(content().string(containsString("No hay libros que coincidan")));
    }

    @Test
    void detalleMuestraDisponibilidadEHistorial() throws Exception {
        mockMvc.perform(get("/libros/5"))
                .andExpect(model().attribute("disponibles", 1L))
                .andExpect(content().string(containsString("Devuelto")))
                .andExpect(content().string(containsString("Historial de préstamos")));
        mockMvc.perform(get("/libros/1"))
                .andExpect(model().attribute("disponibles", 2L));
        mockMvc.perform(get("/libros/14"))
                .andExpect(content().string(containsString("Este libro todavía no se ha prestado")));
    }

    @Test
    void rechazaEdicionConIsbnDeOtroLibroSinCambiarDatos() throws Exception {
        mockMvc.perform(post("/libros/14").with(csrf())
                        .param("isbn", "978-84-376-2405-1")
                        .param("titulo", "Cambio inválido")
                        .param("ejemplares", "1").param("autores", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("libros/formulario"))
                .andExpect(model().attributeHasFieldErrorCode("libro", "isbn", "isbn.repetido"))
                .andExpect(model().attributeExists("todosLosAutores"));
        recargarDatos();
        assertThat(libroRepository.findById(14L).orElseThrow().getTitulo()).isEqualTo("El juego del ángel");
    }

    @Test
    void rechazaCamposInvalidos() throws Exception {
        mockMvc.perform(post("/libros").with(csrf())
                        .param("isbn", "x".repeat(18)).param("titulo", "x".repeat(201))
                        .param("editorial", "x".repeat(101)).param("genero", "x".repeat(51))
                        .param("anioPublicacion", "-1").param("ejemplares", "0")
                        .param("autores", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("libro", "isbn", "titulo", "editorial",
                        "genero", "anioPublicacion", "ejemplares"));
        assertThat(libroRepository.count()).isEqualTo(14);
    }

    @Test
    void rechazaNumerosYAutoresNoValidos() throws Exception {
        mockMvc.perform(post("/libros").with(csrf())
                        .param("isbn", "nuevo").param("titulo", "Libro")
                        .param("ejemplares", "texto").param("autores", "texto"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("libro", "ejemplares", "autores"));
        mockMvc.perform(post("/libros").with(csrf())
                        .param("isbn", "nuevo").param("titulo", "Libro")
                        .param("ejemplares", "1").param("autores", "999"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("libro", "autores"));
        assertThat(libroRepository.count()).isEqualTo(14);
    }

    @Test
    void noEliminaUnLibroConPrestamosYaDevueltos() throws Exception {
        mockMvc.perform(post("/libros/5/eliminar").with(csrf()))
                .andExpect(redirectedUrl("/libros"))
                .andExpect(flash().attributeExists("mensajeError"));
        recargarDatos();
        assertThat(libroRepository.existsById(5L)).isTrue();
    }

    @Test
    void rutasDeEdicionYBorradoInexistentesDevuelven404() throws Exception {
        mockMvc.perform(get("/libros/999/editar")).andExpect(status().isNotFound());
        mockMvc.perform(post("/libros/999").with(csrf())
                        .param("isbn", "nuevo").param("titulo", "Libro")
                        .param("ejemplares", "1").param("autores", "1"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/libros/999/eliminar").with(csrf())).andExpect(status().isNotFound());
    }
}
