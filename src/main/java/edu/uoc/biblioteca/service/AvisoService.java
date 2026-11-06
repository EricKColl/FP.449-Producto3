package edu.uoc.biblioteca.service;

import edu.uoc.biblioteca.model.EstadoReserva;
import edu.uoc.biblioteca.model.Libro;
import edu.uoc.biblioteca.model.Prestamo;
import edu.uoc.biblioteca.model.Reserva;
import edu.uoc.biblioteca.repository.PrestamoRepository;
import edu.uoc.biblioteca.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Avisos de la campanita de la cabecera. No se guardan en la base de datos: se calculan en cada página con el
 * estado real de préstamos y reservas, así nunca quedan avisos viejos. Cada aviso tiene una clave estable para
 * que el navegador recuerde cuáles ya ha visto (js/app.js).
 */
@Service
@Transactional(readOnly = true)
public class AvisoService {

    /** Días antes del vencimiento en los que se avisa al socio. */
    static final int DIAS_AVISO_VENCIMIENTO = 2;

    private final PrestamoRepository prestamoRepository;
    private final ReservaRepository reservaRepository;
    private final PrestamoService prestamoService;

    public AvisoService(PrestamoRepository prestamoRepository, ReservaRepository reservaRepository,
                        PrestamoService prestamoService) {
        this.prestamoRepository = prestamoRepository;
        this.reservaRepository = reservaRepository;
        this.prestamoService = prestamoService;
    }

    /**
     * @param tono peligro, aviso o exito: el color del aviso
     * @param icono nombre del icono de Tabler Icons, sin el prefijo ti-
     */
    public record Aviso(String clave, String icono, String tono, String titulo, String texto, String enlace) {
    }

    /** Socio: préstamos fuera de plazo, préstamos que vencen pronto y reservas que ya puede pedir. */
    public List<Aviso> deSocio(Long usuarioId) {
        List<Aviso> avisos = new ArrayList<>();
        for (Prestamo p : prestamoRepository.findByUsuarioIdAndFechaDevolucionIsNullOrderByFechaDevolucionPrevistaAsc(usuarioId)) {
            String titulo = p.getLibro().getTitulo();
            long retraso = p.getDiasRetraso();
            long quedan = p.getDiasRestantes();
            if (retraso > 0) {
                avisos.add(new Aviso("vencido-" + p.getId(), "alarm", "peligro", "«" + titulo + "» está fuera de plazo",
                        "Venció hace " + dias(retraso) + ". Devuélvelo para poder pedir otro libro.", "/user/prestamos"));
            } else if (quedan <= DIAS_AVISO_VENCIMIENTO) {
                String cuando = quedan == 0 ? "hoy" : quedan == 1 ? "mañana" : "en " + dias(quedan);
                String renovar = prestamoService.motivoNoRenovable(p) == null ? " Aún puedes renovarlo." : "";
                avisos.add(new Aviso("vence-" + p.getId() + "-" + p.getFechaDevolucionPrevista(), "calendar-due", "aviso",
                        "«" + titulo + "» vence " + cuando, "Devuélvelo a tiempo." + renovar, "/user/prestamos"));
            }
        }
        for (Reserva r : reservaRepository.findByUsuarioIdOrderByFechaReservaDescIdDesc(usuarioId)) {
            if (r.isActiva() && prestamoService.ejemplaresParaUsuario(r.getLibro(), usuarioId) > 0) {
                avisos.add(new Aviso("reserva-" + r.getId(), "confetti", "exito", "Ya puedes pedir «" + r.getLibro().getTitulo() + "»",
                        "Ha vuelto un ejemplar del libro que reservaste.", "/user/reservas"));
            }
        }
        return avisos;
    }

    /** Administración: préstamos fuera de plazo y reservas con un ejemplar listo para prestar. */
    public List<Aviso> deAdministracion() {
        List<Aviso> avisos = new ArrayList<>();
        long vencidos = prestamoRepository.countByFechaDevolucionIsNullAndFechaDevolucionPrevistaBefore(LocalDate.now());
        if (vencidos > 0) {
            avisos.add(new Aviso("admin-vencidos-" + vencidos + "-" + LocalDate.now(), "alarm", "peligro",
                    vencidos == 1 ? "1 préstamo fuera de plazo" : vencidos + " préstamos fuera de plazo",
                    "Revisa quién los tiene y reclama la devolución.", "/admin/prestamos?estado=VENCIDO"));
        }
        // Puesto de cada reserva en la cola de su libro: está lista si hay ejemplares libres para llegar a ella
        Map<Long, Integer> puestos = new HashMap<>();
        long listas = 0;
        for (Reserva r : reservaRepository.findByEstadoOrderByFechaReservaAscIdAsc(EstadoReserva.ACTIVA)) {
            Libro libro = r.getLibro();
            int puesto = puestos.merge(libro.getId(), 1, Integer::sum);
            if (prestamoService.ejemplaresDisponibles(libro) >= puesto) {
                listas++;
            }
        }
        if (listas > 0) {
            avisos.add(new Aviso("admin-reservas-" + listas + "-" + LocalDate.now(), "bookmark", "exito",
                    listas == 1 ? "1 reserva con ejemplar listo" : listas + " reservas con ejemplar listo",
                    "Esos socios ya pueden llevarse el libro que esperaban.", "/admin/panel"));
        }
        return avisos;
    }

    private static String dias(long n) {
        return n == 1 ? "1 día" : n + " días";
    }
}
