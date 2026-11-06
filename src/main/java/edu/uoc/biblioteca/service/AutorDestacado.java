package edu.uoc.biblioteca.service;

/** Autor con el número de libros que tiene en el catálogo, para la portada. */
public record AutorDestacado(Long id, String nombre, String apellidos, String nacionalidad, long libros) {

    public String nombreCompleto() {
        return nombre + " " + apellidos;
    }

    public String iniciales() {
        return nombre.substring(0, 1) + apellidos.substring(0, 1);
    }
}
