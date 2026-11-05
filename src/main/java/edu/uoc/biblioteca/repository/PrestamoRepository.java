package edu.uoc.biblioteca.repository;

import edu.uoc.biblioteca.model.Prestamo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    /** Préstamos del más reciente al más antiguo, cargando usuario y libro en la misma consulta. */
    @EntityGraph(attributePaths = {"usuario", "libro"})
    List<Prestamo> findAllByOrderByFechaPrestamoDesc();

    /** Préstamos de un usuario, del más reciente al más antiguo, cargando el libro en la misma consulta.*/
    @EntityGraph(attributePaths = "libro")
    List<Prestamo> findByUsuarioIdOrderByFechaPrestamoDescIdDesc(Long usuarioId);

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

    /** Préstamos sin devolver de un usuario. */
    long countByUsuarioIdAndFechaDevolucionIsNull(Long usuarioId);

    /** ¿Tiene el usuario algún préstamo sin devolver cuya fecha prevista ya pasó? */
    boolean existsByUsuarioIdAndFechaDevolucionIsNullAndFechaDevolucionPrevistaBefore(Long usuarioId, LocalDate fecha);
}
