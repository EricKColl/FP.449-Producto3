package edu.uoc.biblioteca.repository;

import edu.uoc.biblioteca.model.Autor;
import edu.uoc.biblioteca.model.EstadoReserva;
import edu.uoc.biblioteca.model.Libro;
import edu.uoc.biblioteca.model.Prestamo;
import edu.uoc.biblioteca.model.Reserva;
import edu.uoc.biblioteca.model.Usuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

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
    private ReservaRepository reservaRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void lasReservasRelacionanUsuariosYLibros() {
        assertThat(reservaRepository.count()).isEqualTo(2);
        List<Reserva> deLaura = reservaRepository.findByUsuarioIdOrderByFechaReservaDescIdDesc(1L);
        assertThat(deLaura).hasSize(1);
        assertThat(deLaura.getFirst().getLibro().getTitulo()).isEqualTo("El capitán Alatriste");
        assertThat(deLaura.getFirst().isActiva()).isTrue();
        assertThat(usuarioRepository.findById(1L).orElseThrow().getReservas())
                .extracting(Reserva::getId).containsExactly(1L);
        assertThat(libroRepository.findById(8L).orElseThrow().getReservas())
                .extracting(Reserva::getId).containsExactly(1L, 2L);
        // La búsqueda comprueba la pertenencia: Daniel no encuentra la reserva de Laura.
        assertThat(reservaRepository.findByIdAndUsuarioId(1L, 6L)).isEmpty();
        assertThat(reservaRepository.existsByUsuarioIdAndLibroIdAndEstado(6L, 8L, EstadoReserva.ACTIVA))
                .isFalse();

        Usuario daniel = usuarioRepository.findById(6L).orElseThrow();
        Libro alatriste = libroRepository.findById(8L).orElseThrow();
        Long nuevaId = reservaRepository.saveAndFlush(new Reserva(daniel, alatriste)).getId();
        entityManager.clear();

        List<Reserva> deDaniel = reservaRepository.findByUsuarioIdOrderByFechaReservaDescIdDesc(6L);
        assertThat(deDaniel).extracting(Reserva::getId).containsExactly(nuevaId, 2L);
        assertThat(reservaRepository.existsByUsuarioIdAndLibroIdAndEstado(6L, 8L, EstadoReserva.ACTIVA))
                .isTrue();
        Reserva guardada = reservaRepository.findByIdAndUsuarioId(nuevaId, 6L).orElseThrow();
        assertThat(guardada.getUsuario().getId()).isEqualTo(6L);
        assertThat(guardada.getLibro().getId()).isEqualTo(8L);
        assertThat(guardada.getFechaReserva()).isEqualTo(LocalDate.now());
        guardada.cancelar();
        entityManager.flush();
        entityManager.clear();

        assertThat(reservaRepository.findById(nuevaId).orElseThrow().getEstado())
                .isEqualTo(EstadoReserva.CANCELADA);
        assertThat(reservaRepository.existsByUsuarioIdAndLibroIdAndEstado(6L, 8L, EstadoReserva.ACTIVA))
                .isFalse();
    }

    @Test
    void losFavoritosSonUnaRelacionNaMEntreUsuariosYLibros() {
        Usuario laura = usuarioRepository.findById(1L).orElseThrow();
        assertThat(laura.getFavoritos()).extracting(Libro::getTitulo).containsExactly("1984", "El hobbit");
        assertThat(libroRepository.findById(2L).orElseThrow().getMarcadoPor())
                .extracting(Usuario::getId).containsExactly(4L);

        Libro juegoDelAngel = libroRepository.findById(14L).orElseThrow();
        laura.getFavoritos().add(juegoDelAngel);
        laura.getFavoritos().add(juegoDelAngel);
        entityManager.flush();
        entityManager.clear();

        // Volvemos a consultar para comprobar lo guardado, sin duplicar el favorito.
        Usuario recargada = usuarioRepository.findById(1L).orElseThrow();
        assertThat(recargada.getFavoritos()).extracting(Libro::getId).containsExactly(6L, 9L, 14L);
        assertThat(libroRepository.findById(14L).orElseThrow().getMarcadoPor())
                .extracting(Usuario::getId).containsExactly(1L);
        recargada.getFavoritos().remove(libroRepository.findById(14L).orElseThrow());
        entityManager.flush();
        entityManager.clear();

        assertThat(usuarioRepository.findById(1L).orElseThrow().getFavoritos())
                .extracting(Libro::getId).containsExactly(6L, 9L);
        assertThat(libroRepository.findById(14L).orElseThrow().getMarcadoPor()).isEmpty();
    }

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
