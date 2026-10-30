package edu.uoc.biblioteca.model;

/** Rol de una cuenta. Spring Security lo trata como la autoridad ROLE_ADMIN o ROLE_USER. */
public enum Rol {
    ADMIN("Administrador"),
    USER("Usuario");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
