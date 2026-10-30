package edu.uoc.biblioteca.repository;

import edu.uoc.biblioteca.model.Autor;
import edu.uoc.biblioteca.model.Libro;
import edu.uoc.biblioteca.model.Prestamo;
import edu.uoc.biblioteca.model.Usuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba las relaciones del modelo contra el esquema real y los datos de prueba
 * (src/main/resources/db). Cada test se deshace al terminar.
 */
@DataJpaTest
@ActiveProfiles("h2")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoriosTest {

    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private AutorRepository autorRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void cargaLosDatosDePrueba() {
        assertThat(libroRepository.count()).isEqualTo(14);
        assertThat(autorRepository.count()).isEqualTo(11);
        assertThat(usuarioRepository.count()).isEqualTo(7);
        assertThat(prestamoRepository.count()).isEqualTo(11);
    }

    @Test
    void unLibroPuedeTenerVariosAutores() {
        Libro buenosPresagios = libroRepository.findById(5L).orElseThrow();

        assertThat(buenosPresagios.getAutores())
                .extracting(Autor::getApellidos)
                .containsExactly("Gaiman", "Pratchett");
        assertThat(buenosPresagios.getAutoresTexto()).isEqualTo("Neil Gaiman, Terry Pratchett");
    }

    @Test
    void unAutorPuedeTenerVariosLibros() {
        Autor tolkien = autorRepository.findById(10L).orElseThrow();

        assertThat(tolkien.getLibros())
                .extracting(Libro::getTitulo)
                .containsExactly("El hobbit", "El señor de los anillos");
    }

    @Test
    void guardaUnLibroNuevoConSusAutores() {
        Libro libro = new Libro("978-84-376-0494-7", "Libro de prueba");
        libro.addAutor(autorRepository.findById(1L).orElseThrow());
        libro.addAutor(autorRepository.findById(2L).orElseThrow());

        Long id = libroRepository.save(libro).getId();
        entityManager.flush();
        entityManager.clear();

        Libro guardado = libroRepository.findById(id).orElseThrow();
        assertThat(id).isGreaterThan(14L);
        assertThat(guardado.getAutores()).hasSize(2);
    }

    @Test
    void elCatalogoSeOrdenaPorTitulo() {
        assertThat(libroRepository.findAllByOrderByTituloAsc())
                .first()
                .extracting(Libro::getTitulo)
                .isEqualTo("1984");
    }

    @Test
    void cuentaLosPrestamosPendientesYVencidos() {
        assertThat(prestamoRepository.countByFechaDevolucionIsNull()).isEqualTo(7);
        assertThat(prestamoRepository.countByFechaDevolucionIsNullAndFechaDevolucionPrevistaBefore(LocalDate.now()))
                .isEqualTo(2);
        assertThat(prestamoRepository.countByLibroIdAndFechaDevolucionIsNull(1L)).isEqualTo(1);
    }

    @Test
    void unUsuarioTieneSuHistorialDePrestamos() {
        Usuario laura = usuarioRepository.findById(1L).orElseThrow();

        assertThat(laura.getPrestamos()).hasSize(3);
        assertThat(laura.getPrestamos().getFirst().getFechaPrestamo())
                .isAfter(laura.getPrestamos().getLast().getFechaPrestamo());
    }

    @Test
    void registraUnPrestamoNuevo() {
        Usuario usuario = usuarioRepository.findById(6L).orElseThrow();
        Libro libro = libroRepository.findById(7L).orElseThrow();

        Prestamo prestamo = prestamoRepository.save(new Prestamo(usuario, libro));
        entityManager.flush();

        assertThat(prestamo.getId()).isGreaterThan(11L);
        assertThat(prestamo.getFechaDevolucionPrevista())
                .isEqualTo(LocalDate.now().plusDays(Prestamo.DIAS_PRESTAMO));
        assertThat(prestamoRepository.existsByLibroId(7L)).isTrue();
    }

    @Test
    void detectaLibrosYUsuariosConPrestamos() {
        assertThat(prestamoRepository.existsByLibroId(1L)).isTrue();
        assertThat(prestamoRepository.existsByLibroId(14L)).isFalse();
        assertThat(prestamoRepository.existsByUsuarioId(1L)).isTrue();
    }
}
