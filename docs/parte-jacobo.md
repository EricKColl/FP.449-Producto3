# Parte de Jacobo · Producto 3

**Autor:** Jacobo Barrera Toba

**Proyecto:** Biblioteca · FP.449 / FP065 · Equipo BackLord

**Rama:** `feature/jacobo-er-registro`

**Fecha de la revisión:** 2 de noviembre de 2026

## Resumen de la aportación

Mi parte amplía el modelo de Biblioteca con reservas y favoritos, incorpora el registro público de socios y actualiza los diagramas y la documentación del modelo. Incluye las pruebas de estas funciones y su comprobación sobre MariaDB con la aplicación ejecutada en Docker.

El trabajo parte del MVC del Producto 2 y de la base de seguridad del Producto 3 preparada por Erick: login, roles, BCrypt y reglas de acceso. Se han reutilizado esos componentes y el diseño común de la aplicación.

## 1. Reservas y favoritos en el modelo de datos

### Reservas

Se han añadido la tabla `reserva`, la entidad `Reserva`, el enumerado `EstadoReserva` y `ReservaRepository`.

Cada reserva tiene un identificador propio, un usuario, un libro, una fecha y un estado (`ACTIVA` o `CANCELADA`). Al crearla se asignan la fecha actual y el estado activo. El método `cancelar()` cambia su estado a cancelada.

La entidad se relaciona con `Usuario` y `Libro` mediante `@ManyToOne`, con carga diferida y relaciones obligatorias. Ambos modelos incluyen su colección de reservas mediante `@OneToMany(mappedBy = ...)`.

El repositorio permite consultar las reservas de un usuario ordenadas de más reciente a más antigua, buscar una reserva por su identificador y propietario, y comprobar si existe una reserva de un usuario para un libro en un estado determinado. La consulta del listado carga también el libro mediante `@EntityGraph`.

El estado se almacena como `VARCHAR(10)`, tanto en SQL como en el mapeo de Hibernate. Esto mantiene la compatibilidad con MariaDB durante la validación del esquema.

### Favoritos

Se ha añadido la tabla intermedia `usuario_favorito` y la relación N:M entre usuarios y libros mediante `@ManyToMany`.

`Usuario.favoritos` es el lado propietario y declara `@JoinTable`; `Libro.marcadoPor` es el lado inverso. La colección de favoritos utiliza `Set<Libro>` y se ordena por título. La clave primaria compuesta `(usuario_id, libro_id)` impide repetir una misma pareja en la base de datos.

### SQL y datos de ejemplo

Se han actualizado [el esquema SQL](../src/main/resources/db/01-esquema.sql) y [los datos de prueba](../src/main/resources/db/02-datos.sql). Incluyen dos reservas de ejemplo —una activa y otra cancelada— y tres asociaciones de favoritos.

Las claves ajenas de las nuevas tablas utilizan `ON DELETE CASCADE`: al eliminar un usuario o libro también se eliminan sus reservas y asociaciones de favoritos. Los préstamos conservan sus restricciones anteriores de borrado.

## 2. Registro público de usuarios

Se han creado `RegistroController` y la vista `registro.html`, accesibles mediante `/registro`.

El formulario solicita nombre, apellidos, DNI/NIE, email, teléfono opcional, contraseña y confirmación. Aplica las restricciones del modelo y comprueba que el DNI y el email no estén repetidos, reutilizando su normalización para comparar correctamente mayúsculas y minúsculas.

La contraseña es obligatoria, debe tener al menos ocho caracteres y no puede superar 72 bytes en UTF-8, límite utilizado por BCrypt. La confirmación debe coincidir y las contraseñas no se devuelven rellenadas al mostrar errores. La regla de longitud se ha centralizado en `UsuarioService.passwordValida()` y también se utiliza en el controlador administrativo de usuarios.

El método `UsuarioService.registrar()` fija siempre el rol `USER` y la fecha actual. Reutiliza la creación de usuarios, que normaliza los datos, descarta cualquier identificador previo y guarda el hash BCrypt.

El controlador limita los campos admitidos con `@InitBinder`, de modo que el formulario no puede elegir identificador, rol ni fecha de alta. Si el usuario ya tiene sesión iniciada, las peticiones de registro vuelven al inicio sin crear otra cuenta.

Tras un registro correcto se redirige a `/login?registrado`, donde aparece un mensaje de confirmación. Se ha autorizado `/registro` en `SecurityConfig`, manteniendo CSRF activo, y se han añadido enlaces «Crear cuenta» en el login y en el menú sin sesión.

## 3. Diagramas y documentación

Se han actualizado estos entregables:

- [Diagrama E/R](diagramas/er.png): cinco entidades, con `password` y `rol` en Usuario, la nueva entidad Reserva y la relación de favoritos entre Usuario y Libro.
- [Diagrama relacional](diagramas/relacional.png): las siete tablas del Producto 3 y sus claves y relaciones.
- [Modelo editable de MySQL Workbench](diagramas/relacional.mwb), obtenido por ingeniería inversa de MariaDB.
- [Modelo de datos](modelo-datos.md): cardinalidades, mapeos JPA, restricciones y reglas de las nuevas relaciones.
- [Reparto de tareas](reparto-tareas.md): tareas implementadas del turno de Jacobo marcadas como completadas.
- [README](../README.md): acceso público a `/registro` y enlace a este informe.

La imagen relacional se ha ordenado manualmente para facilitar la lectura.

## 4. Pruebas y resultados

### Pruebas automatizadas

Se han incorporado **nueve tests de registro** y **dos tests de repositorios**, que amplían la suite de 89 a **100 tests**.

Las pruebas del registro cubren acceso público y enlaces, registro correcto con normalización y BCrypt, datos inválidos, DNI y email duplicados, límites de contraseña, confirmación, protección de identificador/rol/fecha, CSRF y peticiones con sesión ya iniciada.

Las pruebas de repositorios verifican la persistencia y cancelación de reservas, su consulta por usuario y propietario, y la persistencia, eliminación y ausencia de duplicados en favoritos.

**Resultado de `./mvnw verify`: 100 tests, 0 fallos, 0 errores y 0 omitidos**, con H2 y los scripts SQL del proyecto.

### Comprobación con Docker y MariaDB

Se ha construido la imagen de la aplicación y arrancado con Docker Compose, conservando el volumen existente de MariaDB. Durante la comprobación se utilizó el puerto 18080 porque IntelliJ ocupaba el 8080; el puerto por defecto del proyecto sigue siendo el 8080.

La aplicación y MariaDB alcanzaron el estado `healthy`, el endpoint de salud respondió `UP` y Hibernate validó el esquema. Se comprobaron las páginas públicas, la validación del registro y la protección CSRF.

También se creó una cuenta temporal mediante el registro público: se verificaron en MariaDB su rol `USER`, la fecha de alta y el hash BCrypt, y se comprobó que podía iniciar sesión y acceder a su cuenta. Las rutas administrativas devolvieron 403 para USER y el acceso administrativo funcionó con ADMIN. Al terminar se eliminó la cuenta temporal.

Se conservaron los recuentos originales: 7 usuarios, 14 libros, 11 autores, 11 préstamos, 2 reservas y 3 favoritos. No se detectaron errores en los logs de la aplicación durante estas comprobaciones.

## 5. Continuación del equipo

La aportación deja preparado el modelo que utilizarán los servicios y pantallas de reservas y favoritos. Antonio implementará sus operaciones bajo `/user/*`, incluyendo la comprobación de disponibilidad, la prevención de reservas activas repetidas y el control de pertenencia de cada operación.

Carles completará la migración de las operaciones de administración a `/admin/*`. La base de seguridad y la coordinación de la integración corresponden a Erick, según el [reparto de tareas](reparto-tareas.md).

Para cerrar la entrega de esta rama quedan la subida de los commits, la pull request hacia `develop` y su revisión e integración. El vídeo y la memoria conjunta se prepararán según el calendario del equipo.

## 6. Commits de implementación

| Commit | Contenido |
|---|---|
| `e9c4ab2` | Relaciones Reserva y favoritos, SQL y pruebas de repositorios. |
| `a4137ee` | Registro público, integración con seguridad, enlaces y pruebas. |
| `bfc703a` | Diagramas E/R y relacional y documentación del modelo. |
