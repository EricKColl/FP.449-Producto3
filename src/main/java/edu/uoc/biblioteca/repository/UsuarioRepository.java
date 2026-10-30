package edu.uoc.biblioteca.repository;

import edu.uoc.biblioteca.model.Rol;
import edu.uoc.biblioteca.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    List<Usuario> findAllByOrderByApellidosAscNombreAsc();

    boolean existsByDni(String dni);

    boolean existsByEmail(String email);

    Optional<Usuario> findByDni(String dni);

    long countByRol(Rol rol);

    Optional<Usuario> findByEmail(String email);
}
