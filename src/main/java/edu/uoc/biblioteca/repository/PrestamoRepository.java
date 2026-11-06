package edu.uoc.biblioteca.repository;

import edu.uoc.biblioteca.model.Prestamo;
import edu.uoc.biblioteca.service.Cifra;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    /** Préstamos del más reciente al más antiguo, cargando usuario y libro en la misma consulta. */
    @EntityGraph(attributePaths = {"usuario", "libro"})
    List<Prestamo> findAllByOrderByFechaPrestamoDesc();

    /** Préstamos de un usuario, del más reciente al más antiguo, cargando el libro en la misma consulta. */
    @EntityGraph(attributePaths = "libro")
    List<Prestamo> findByUsuarioIdOrderByFechaPrestamoDescIdDesc(Long usuarioId);

    /** Préstamos sin devolver de un usuario, el que vence antes primero. */
    @EntityGraph(attributePaths = "libro")
    List<Prestamo> findByUsuarioIdAndFechaDevolucionIsNullOrderByFechaDevolucionPrevistaAsc(Long usuarioId);

    /** Préstamos fuera de plazo, el más atrasado primero, cargando usuario y libro. */
    @EntityGraph(attributePaths = {"usuario", "libro"})
    List<Prestamo> findByFechaDevolucionIsNullAndFechaDevolucionPrevistaBeforeOrderByFechaDevolucionPrevistaAsc(LocalDate hoy);

    /** Préstamos hechos desde una fecha, para la gráfica de actividad por meses. */
    List<Prestamo> findByFechaPrestamoGreaterThanEqual(LocalDate desde);

    /** Devoluciones registradas desde una fecha, para la misma gráfica. */
    List<Prestamo> findByFechaDevolucionGreaterThanEqual(LocalDate desde);

    /** Devoluciones hechas dentro de plazo. */
    @Query("select count(p) from Prestamo p where p.fechaDevolucion is not null "
            + "and p.fechaDevolucion <= p.fechaDevolucionPrevista")
    long contarDevueltosEnPlazo();

    long countByFechaDevolucionIsNotNull();

    long countByUsuarioId(Long usuarioId);

    /** Libros más prestados de la historia de la biblioteca. */
    @Query("select new edu.uoc.biblioteca.service.Cifra(p.libro.titulo, count(p)) from Prestamo p "
            + "group by p.libro.id, p.libro.titulo order by count(p) desc, p.libro.titulo")
    List<Cifra> contarPorLibro(Pageable pagina);

    /** Los cinco préstamos más recientes, para el panel de inicio. */
    @EntityGraph(attributePaths = {"usuario", "libro"})
    List<Prestamo> findTop5ByOrderByFechaPrestamoDescIdDesc();

    /** Préstamos sin devolver (activos + vencidos). */
    long countByFechaDevolucionIsNull();

    /** Préstamos sin devolver cuya fecha prevista es anterior a {@code fecha}. */
    long countByFechaDevolucionIsNullAndFechaDevolucionPrevistaBefore(LocalDate fecha);

    /** Ejemplares de un libro que están prestados ahora mismo. */
    long countByLibroIdAndFechaDevolucionIsNull(Long libroId);

    boolean existsByLibroId(Long libroId);

    boolean existsByUsuarioId(Long usuarioId);

    /** ¿Tiene el usuario este libro prestado ahora mismo? */
    boolean existsByUsuarioIdAndLibroIdAndFechaDevolucionIsNull(Long usuarioId, Long libroId);

    /** Préstamos sin devolver de un usuario. */
    long countByUsuarioIdAndFechaDevolucionIsNull(Long usuarioId);

    /** ¿Tiene el usuario algún préstamo sin devolver cuya fecha prevista ya pasó? */
    boolean existsByUsuarioIdAndFechaDevolucionIsNullAndFechaDevolucionPrevistaBefore(Long usuarioId, LocalDate fecha);
}
