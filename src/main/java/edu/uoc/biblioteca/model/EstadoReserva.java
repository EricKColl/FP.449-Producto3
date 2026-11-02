package edu.uoc.biblioteca.model;

/** Estados de una reserva. Se guardan en la base de datos y tienen una etiqueta para las vistas. */
public enum EstadoReserva {

    ACTIVA("Activa"),
    CANCELADA("Cancelada");

    private final String etiqueta;

    EstadoReserva(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
