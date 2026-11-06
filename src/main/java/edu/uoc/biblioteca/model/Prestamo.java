package edu.uoc.biblioteca.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Préstamo de un libro a un usuario. Hace de entidad asociativa de la relación N:M
 * entre {@link Usuario} y {@link Libro}, con atributos propios (las fechas).
 * <p>
 * Está pendiente mientras {@code fechaDevolucion} sea {@code null}.
 */
@Entity
@Table(name = "prestamo")
public class Prestamo {

    /** Duración por defecto de un préstamo, en días. */
    public static final int DIAS_PRESTAMO = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Selecciona un usuario")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @NotNull(message = "Selecciona un libro")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "libro_id", nullable = false)
    private Libro libro;

    @NotNull(message = "La fecha de préstamo es obligatoria")
    @Column(nullable = false)
    private LocalDate fechaPrestamo = LocalDate.now();

    @NotNull(message = "La fecha de devolución prevista es obligatoria")
    @Column(nullable = false)
    private LocalDate fechaDevolucionPrevista = LocalDate.now().plusDays(DIAS_PRESTAMO);

    private LocalDate fechaDevolucion;

    public Prestamo() {
    }

    public Prestamo(Usuario usuario, Libro libro) {
        this.usuario = usuario;
        this.libro = libro;
    }

    public EstadoPrestamo getEstado() {
        if (fechaDevolucion != null) {
            return EstadoPrestamo.DEVUELTO;
        }
        return LocalDate.now().isAfter(fechaDevolucionPrevista) ? EstadoPrestamo.VENCIDO : EstadoPrestamo.ACTIVO;
    }

    public boolean isDevuelto() {
        return fechaDevolucion != null;
    }

    /** Días que faltan para la fecha prevista (negativo si ya pasó); 0 si ya está devuelto. */
    public long getDiasRestantes() {
        return isDevuelto() ? 0 : ChronoUnit.DAYS.between(LocalDate.now(), fechaDevolucionPrevista);
    }

    /** Días de retraso de un préstamo sin devolver; 0 si está en plazo o devuelto. */
    public long getDiasRetraso() {
        return Math.max(0, -getDiasRestantes());
    }

    /** Parte del plazo ya consumida, de 0 a 100, para la barra de progreso. */
    public int getPorcentajePlazo() {
        long total = Math.max(1, ChronoUnit.DAYS.between(fechaPrestamo, fechaDevolucionPrevista));
        long usados = ChronoUnit.DAYS.between(fechaPrestamo, isDevuelto() ? fechaDevolucion : LocalDate.now());
        return (int) Math.min(100, Math.max(0, usados * 100 / total));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Libro getLibro() {
        return libro;
    }

    public void setLibro(Libro libro) {
        this.libro = libro;
    }

    public LocalDate getFechaPrestamo() {
        return fechaPrestamo;
    }

    public void setFechaPrestamo(LocalDate fechaPrestamo) {
        this.fechaPrestamo = fechaPrestamo;
    }

    public LocalDate getFechaDevolucionPrevista() {
        return fechaDevolucionPrevista;
    }

    public void setFechaDevolucionPrevista(LocalDate fechaDevolucionPrevista) {
        this.fechaDevolucionPrevista = fechaDevolucionPrevista;
    }

    public LocalDate getFechaDevolucion() {
        return fechaDevolucion;
    }

    public void setFechaDevolucion(LocalDate fechaDevolucion) {
        this.fechaDevolucion = fechaDevolucion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Prestamo otro)) {
            return false;
        }
        return id != null && id.equals(otro.getId());
    }

    @Override
    public int hashCode() {
        return Prestamo.class.hashCode();
    }

    @Override
    public String toString() {
        return "Prestamo{id=" + id + ", fechaPrestamo=" + fechaPrestamo + ", estado=" + getEstado() + "}";
    }
}
