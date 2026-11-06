package edu.uoc.biblioteca.service;

/**
 * Evento: algo ha cambiado en los préstamos o reservas de un libro (préstamo, devolución, renovación, reserva
 * o cancelación). Lo publican PrestamoService y ReservaService y lo escucha {@link AvisosEnVivo}, que manda
 * los avisos nuevos a quien tenga la página abierta.
 *
 * @param libroId   libro afectado: también cambia la cola de quienes lo tienen reservado
 * @param usuarioId socio que hizo o recibió la operación
 */
public record AvisosCambiados(Long libroId, Long usuarioId) {
}
