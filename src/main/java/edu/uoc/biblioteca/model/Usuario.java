package edu.uoc.biblioteca.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Socio de la biblioteca que puede tomar libros en préstamo.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "[0-9XYZxyz][0-9]{7}[A-Za-z]", message = "El DNI/NIE debe tener 8 caracteres y una letra")
    @Column(nullable = false, unique = true, length = 9)
    private String dni;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
    @Column(nullable = false, length = 80)
    private String nombre;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 120, message = "Los apellidos no pueden superar los 120 caracteres")
    @Column(nullable = false, length = 120)
    private String apellidos;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Size(max = 120, message = "El email no puede superar los 120 caracteres")
    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    @Column(length = 20)
    private String telefono;

    // Formato de <input type="date"> en los formularios
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @NotNull(message = "La fecha de alta es obligatoria")
    @PastOrPresent(message = "La fecha de alta no puede ser futura")
    @Column(nullable = false)
    private LocalDate fechaAlta = LocalDate.now();

    /** Contraseña cifrada con BCrypt. La cifra UsuarioService: nunca se guarda en claro. */
    @Column(nullable = false, length = 60)
    private String password;

    // Sin @JdbcTypeCode, Hibernate esperaría una columna ENUM en MariaDB y fallaría al validar el esquema
    @NotNull(message = "El rol es obligatorio")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private Rol rol = Rol.USER;

    @OneToMany(mappedBy = "usuario")
    @OrderBy("fechaPrestamo DESC")
    private List<Prestamo> prestamos = new ArrayList<>();

    public Usuario() {
    }

    public Usuario(String dni, String nombre, String apellidos, String email) {
        this.dni = dni;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.email = email;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellidos;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public LocalDate getFechaAlta() {
        return fechaAlta;
    }

    public void setFechaAlta(LocalDate fechaAlta) {
        this.fechaAlta = fechaAlta;
    }

    public List<Prestamo> getPrestamos() {
        return prestamos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Usuario otro)) {
            return false;
        }
        return id != null && id.equals(otro.getId());
    }

    @Override
    public int hashCode() {
        return Usuario.class.hashCode();
    }

    @Override
    public String toString() {
        return "Usuario{id=" + id + ", dni='" + dni + "', nombre='" + getNombreCompleto() + "'}";
    }
}
