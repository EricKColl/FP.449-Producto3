-- =====================================================================
--  Biblioteca · Esquema de la base de datos (modelo relacional)
--  Derivado del diagrama E/R: docs/modelo-datos.md
--
--  Entidades: autor, libro, usuario, prestamo
--  Relaciones:
--    autor   N:M libro    -> tabla intermedia libro_autor
--    usuario 1:N prestamo -> prestamo.usuario_id
--    libro   1:N prestamo -> prestamo.libro_id
--    (usuario N:M libro a través de prestamo, con atributos propios)
--
--  MariaDB lo ejecuta automáticamente al crear el contenedor por primera
--  vez (docker-entrypoint-initdb.d). Para ejecutarlo a mano en otro
--  servidor: crear antes la base de datos "biblioteca" y seleccionarla.
--  El perfil "h2" de Spring también lo usa (tests y ejecución sin Docker).
-- =====================================================================

DROP TABLE IF EXISTS prestamo;
DROP TABLE IF EXISTS libro_autor;
DROP TABLE IF EXISTS libro;
DROP TABLE IF EXISTS autor;
DROP TABLE IF EXISTS usuario;

CREATE TABLE autor (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    nombre           VARCHAR(80)  NOT NULL,
    apellidos        VARCHAR(120) NOT NULL,
    nacionalidad     VARCHAR(60),
    fecha_nacimiento DATE,
    CONSTRAINT pk_autor PRIMARY KEY (id)
);

CREATE TABLE libro (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    isbn             VARCHAR(17)  NOT NULL,
    titulo           VARCHAR(200) NOT NULL,
    editorial        VARCHAR(100),
    anio_publicacion INT,
    genero           VARCHAR(50),
    ejemplares       INT          NOT NULL DEFAULT 1,
    CONSTRAINT pk_libro PRIMARY KEY (id),
    CONSTRAINT uk_libro_isbn UNIQUE (isbn),
    CONSTRAINT ck_libro_ejemplares CHECK (ejemplares >= 1)
);

-- Relación N:M entre libro y autor
CREATE TABLE libro_autor (
    libro_id BIGINT NOT NULL,
    autor_id BIGINT NOT NULL,
    CONSTRAINT pk_libro_autor PRIMARY KEY (libro_id, autor_id),
    CONSTRAINT fk_libro_autor_libro FOREIGN KEY (libro_id) REFERENCES libro (id) ON DELETE CASCADE,
    CONSTRAINT fk_libro_autor_autor FOREIGN KEY (autor_id) REFERENCES autor (id) ON DELETE CASCADE
);

CREATE TABLE usuario (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    dni        VARCHAR(9)   NOT NULL,
    nombre     VARCHAR(80)  NOT NULL,
    apellidos  VARCHAR(120) NOT NULL,
    email      VARCHAR(120) NOT NULL,
    telefono   VARCHAR(20),
    fecha_alta DATE         NOT NULL,
    -- Cuenta de acceso: la contraseña se guarda cifrada con BCrypt (60 caracteres)
    password   VARCHAR(60)  NOT NULL,
    rol        VARCHAR(10)  NOT NULL DEFAULT 'USER',
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_dni UNIQUE (dni),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('ADMIN', 'USER'))
);

-- Un préstamo está pendiente mientras fecha_devolucion sea NULL.
-- No se puede borrar un libro o un usuario que tenga préstamos (RESTRICT).
CREATE TABLE prestamo (
    id                        BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id                BIGINT NOT NULL,
    libro_id                  BIGINT NOT NULL,
    fecha_prestamo            DATE   NOT NULL,
    fecha_devolucion_prevista DATE   NOT NULL,
    fecha_devolucion          DATE,
    CONSTRAINT pk_prestamo PRIMARY KEY (id),
    CONSTRAINT fk_prestamo_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT fk_prestamo_libro FOREIGN KEY (libro_id) REFERENCES libro (id) ON DELETE RESTRICT,
    CONSTRAINT ck_prestamo_fechas CHECK (fecha_devolucion_prevista >= fecha_prestamo),
    CONSTRAINT ck_prestamo_devolucion CHECK (fecha_devolucion IS NULL OR fecha_devolucion >= fecha_prestamo)
);
