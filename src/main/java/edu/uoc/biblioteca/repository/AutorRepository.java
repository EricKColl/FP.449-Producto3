package edu.uoc.biblioteca.repository;

import edu.uoc.biblioteca.model.Autor;
import edu.uoc.biblioteca.service.AutorDestacado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AutorRepository extends JpaRepository<Autor, Long> {

    List<Autor> findAllByOrderByApellidosAscNombreAsc();

    /** Autores con su número de libros, de los que más tienen a los que menos. */
    @Query("select new edu.uoc.biblioteca.service.AutorDestacado(a.id, a.nombre, a.apellidos, a.nacionalidad, count(l)) "
            + "from Autor a left join a.libros l group by a.id, a.nombre, a.apellidos, a.nacionalidad "
            + "order by count(l) desc, a.apellidos, a.nombre")
    List<AutorDestacado> listarConLibros();
}
