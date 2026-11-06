package edu.uoc.biblioteca.repository;

import edu.uoc.biblioteca.model.EstadoReserva;
import edu.uoc.biblioteca.model.Reserva;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /** Reservas de un usuario, de la más reciente a la más antigua, cargando el libro. */
    @EntityGraph(attributePaths = "libro")
    List<Reserva> findByUsuarioIdOrderByFechaReservaDescIdDesc(Long usuarioId);

    /** Busca una reserva solo si pertenece al usuario indicado. */
    Optional<Reserva> findByIdAndUsuarioId(Long id, Long usuarioId);

    /** Reservas en un estado, la más antigua primero: es el orden de la cola. */
    @EntityGraph(attributePaths = {"usuario", "libro"})
    List<Reserva> findByEstadoOrderByFechaReservaAscIdAsc(EstadoReserva estado);

    long countByEstado(EstadoReserva estado);

    /** Cola de un libro: sus reservas en un estado, la más antigua primero. */
    List<Reserva> findByLibroIdAndEstadoOrderByFechaReservaAscIdAsc(Long libroId, EstadoReserva estado);

    Optional<Reserva> findFirstByUsuarioIdAndLibroIdAndEstado(Long usuarioId, Long libroId, EstadoReserva estado);

    long countByUsuarioIdAndEstado(Long usuarioId, EstadoReserva estado);

    boolean existsByUsuarioIdAndLibroIdAndEstado(Long usuarioId, Long libroId, EstadoReserva estado);
}
