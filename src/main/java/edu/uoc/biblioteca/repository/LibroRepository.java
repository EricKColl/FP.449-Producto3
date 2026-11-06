package edu.uoc.biblioteca.repository;

import edu.uoc.biblioteca.model.Libro;
import edu.uoc.biblioteca.service.Cifra;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LibroRepository extends JpaRepository<Libro, Long> {

    /** Catálogo ordenado por título, cargando los autores en la misma consulta. */
    @EntityGraph(attributePaths = "autores")
    List<Libro> findAllByOrderByTituloAsc();

    /** Las últimas incorporaciones al catálogo, para la portada. */
    @EntityGraph(attributePaths = "autores")
    List<Libro> findTop6ByOrderByIdDesc();

    /** Ejemplares de todo el catálogo. */
    @Query("select coalesce(sum(l.ejemplares), 0) from Libro l")
    long sumarEjemplares();

    /** Títulos por género, del más numeroso al menos. */
    @Query("select new edu.uoc.biblioteca.service.Cifra(l.genero, count(l)) from Libro l "
            + "where l.genero is not null group by l.genero order by count(l) desc, l.genero")
    List<Cifra> contarPorGenero();

    boolean existsByIsbn(String isbn);

    Optional<Libro> findByIsbn(String isbn);
}
