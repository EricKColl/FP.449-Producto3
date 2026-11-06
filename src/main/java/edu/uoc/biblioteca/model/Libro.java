package edu.uoc.biblioteca.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Libro del catálogo. Lado propietario de la relación N:M con {@link Autor}
 * (tabla intermedia {@code libro_autor}) y lado inverso de la relación 1:N con {@link Prestamo}.
 */
@Entity
@Table(name = "libro")
public class Libro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El ISBN es obligatorio")
    @Size(max = 17, message = "El ISBN no puede superar los 17 caracteres")
    @Column(nullable = false, unique = true, length = 17)
    private String isbn;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 200, message = "El título no puede superar los 200 caracteres")
    @Column(nullable = false, length = 200)
    private String titulo;

    @Size(max = 100, message = "La editorial no puede superar los 100 caracteres")
    @Column(length = 100)
    private String editorial;

    @Positive(message = "El año de publicación debe ser un número positivo")
    private Integer anioPublicacion;

    @Size(max = 50, message = "El género no puede superar los 50 caracteres")
    @Column(length = 50)
    private String genero;

    @NotNull(message = "El número de ejemplares es obligatorio")
    @Min(value = 1, message = "Debe haber al menos un ejemplar")
    @Column(nullable = false)
    private Integer ejemplares = 1;

    @ManyToMany
    @JoinTable(
            name = "libro_autor",
            joinColumns = @JoinColumn(name = "libro_id"),
            inverseJoinColumns = @JoinColumn(name = "autor_id"))
    @OrderBy("apellidos ASC, nombre ASC")
    private Set<Autor> autores = new LinkedHashSet<>();

    @OneToMany(mappedBy = "libro")
    @OrderBy("fechaPrestamo DESC")
    private List<Prestamo> prestamos = new ArrayList<>();

    @OneToMany(mappedBy = "libro")
    @OrderBy("fechaReserva DESC")
    private List<Reserva> reservas = new ArrayList<>();

    @ManyToMany(mappedBy = "favoritos")
    private Set<Usuario> marcadoPor = new LinkedHashSet<>();

    public Libro() {
    }

    public Libro(String isbn, String titulo) {
        this.isbn = isbn;
        this.titulo = titulo;
    }

    // Mantienen sincronizados los dos lados de la relación N:M
    public void addAutor(Autor autor) {
        autores.add(autor);
        autor.getLibros().add(this);
    }

    public void removeAutor(Autor autor) {
        autores.remove(autor);
        autor.getLibros().remove(this);
    }

    /** Nombres de los autores separados por comas, para mostrarlos en listados. */
    public String getAutoresTexto() {
        // Sin fallar si el formulario llega con un autor que no existe (queda como null hasta validar)
        return autores == null ? "" : autores.stream()
                .filter(java.util.Objects::nonNull)
                .map(Autor::getNombreCompleto)
                .collect(Collectors.joining(", "));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getEditorial() {
        return editorial;
    }

    public void setEditorial(String editorial) {
        this.editorial = editorial;
    }

    public Integer getAnioPublicacion() {
        return anioPublicacion;
    }

    public void setAnioPublicacion(Integer anioPublicacion) {
        this.anioPublicacion = anioPublicacion;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public Integer getEjemplares() {
        return ejemplares;
    }

    public void setEjemplares(Integer ejemplares) {
        this.ejemplares = ejemplares;
    }

    public Set<Autor> getAutores() {
        return autores;
    }

    public void setAutores(Set<Autor> autores) {
        this.autores = autores;
    }

    public List<Reserva> getReservas() {
        return reservas;
    }

    public Set<Usuario> getMarcadoPor() {
        return marcadoPor;
    }

    public List<Prestamo> getPrestamos() {
        return prestamos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Libro otro)) {
            return false;
        }
        return id != null && id.equals(otro.getId());
    }

    @Override
    public int hashCode() {
        return Libro.class.hashCode();
    }

    @Override
    public String toString() {
        return "Libro{id=" + id + ", isbn='" + isbn + "', titulo='" + titulo + "'}";
    }
}
