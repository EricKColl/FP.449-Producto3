# Reparto de tareas · Producto 3

Entrega: **lunes 23 de noviembre de 2026**. Fecha interna de entrega: **domingo 22 de noviembre** (un día de margen).

El reparto sigue la rúbrica (3 criterios, 25 puntos): cada persona es responsable de **una parte vertical de la aplicación**
(modelo o servicio + controlador + vistas + tests).

**El código se hace por turnos**, uno detrás de otro, como en el Producto 2: cada persona programa su parte cuando le toca,
partiendo del trabajo del anterior ya unido en `develop`. Así nadie tiene que resolver conflictos al juntar el código.

| Turno | Persona | Rama | Parte | Criterio de la rúbrica |
|---|---|---|---|---|
| **1** | Erick Coll | `feature/erick-base-seguridad` | Base de seguridad: Spring Security, login, roles y Docker | Spring Security · Gestión por roles |
| **2** | Jacobo Barrera | `feature/jacobo-er-registro` | Diagramas E/R y relacional, nueva entidad y formulario de registro | E/R y modelos · Spring Security |
| **3** | Carles Miguel | `feature/carles-rutas-admin` | Rutas `/admin/*`: todo lo que crea, modifica o borra filas | Gestión por roles |
| **4** | Antonio Charneco | `feature/antonio-rutas-user` | Rutas `/user/*`: lo que hace el usuario con su cuenta | Gestión por roles |

Erick revisa e integra cada pull request, monta el vídeo y la memoria y prepara la entrega.

## Decisión de modelo

- **Usuario** deja de ser solo un socio y pasa a ser también la cuenta de acceso: se le añaden `password` (cifrada con BCrypt) y `rol` (`ADMIN` o `USER`). Ya tiene más de 5 atributos.
- **Nueva relación: Usuario reserva Libro.** La rúbrica pide una relación *nueva* con las entidades anteriores, y Usuario–Préstamo ya existía en el Producto 2. La entidad `Reserva` (usuario, libro, fecha y estado) permite reservar los libros sin ejemplares disponibles, una de las propuestas de mejora de la memoria del Producto 2.
- **Nueva relación: Usuario marca como favorito Libro (N:M).** Tabla intermedia `usuario_favorito`, sin atributos propios y con `@ManyToMany`. Completa a `Reserva`, que es una relación con atributos.

## Turno 1 · Base de seguridad (Erick)

Rama `feature/erick-base-seguridad` · **Del 30 de octubre al martes 3 de noviembre**

- [x] Dependencias: Spring Security e integración con Thymeleaf.
- [x] `usuario` con `password` y `rol` en `01-esquema.sql`, `02-datos.sql` y la entidad. Cuentas de prueba en el README.
- [x] `SecurityConfig`, `UserDetailsService` y BCrypt.
- [x] Formulario de login propio con el diseño BackLord, cierre de sesión y página de acceso denegado (403).
- [x] Reglas de acceso: `/admin/**` solo ADMIN, `/user/**` con sesión iniciada y el resto reservado a ADMIN.
- [x] Menú lateral, inicio y ficha de libro según el rol. Primera ruta de usuario: `/user/cuenta`.
- [x] Alta y edición de usuarios con contraseña y rol. Nadie elimina su propia cuenta ni deja la aplicación sin administradores.
- [x] Protección CSRF activa en todos los formularios.
- [x] Tests existentes adaptados y 23 nuevos de login, cuentas y acceso por rol: `./mvnw verify` da **89 tests**.
- [x] Docker: nombres de contenedores propios del Producto 3, para que no choquen con los del Producto 2.

## Turno 2 · Diagramas y registro (Jacobo)

Rama `feature/jacobo-er-registro` · **Del miércoles 4 al domingo 8 de noviembre**

- [ ] Diagrama E/R con los atributos nuevos de Usuario y las relaciones Usuario–Reserva–Libro y Usuario–Favorito–Libro.
- [ ] Modelo relacional actualizado y `docs/modelo-datos.md`.
- [ ] Tabla `reserva`, entidad `Reserva` y repositorio, con las relaciones en `Usuario` y `Libro`.
- [ ] Tabla `usuario_favorito` y `@ManyToMany` entre `Usuario` y `Libro`.
- [ ] Formulario de registro público (`/registro`): validación, DNI y email únicos, contraseña cifrada y rol `USER` siempre.
- [ ] Abrir `/registro` en `SecurityConfig` y añadir el enlace «Crear cuenta» en el login y en el menú sin sesión.
- [ ] Tests del registro y de los repositorios.

## Turno 3 · Rutas de administración (Carles)

Rama `feature/carles-rutas-admin` · **Del lunes 9 al viernes 13 de noviembre**

- [ ] Mover a `/admin/*` todas las rutas de libros, autores y usuarios que crean, modifican o borran filas.
- [ ] Separar la escritura de libros en `LibroAdminController`, como ya están `AutorAdminController` y `UsuarioAdminController`.
- [ ] Actualizar enlaces y formularios: los botones de crear, editar y eliminar solo se ven con rol ADMIN.
- [ ] Quitar de `SecurityConfig` las reglas temporales de `/libros/nuevo`, `/libros/*/editar`, `/autores/nuevo` y `/autores/*/editar`.
- [ ] Tests: un USER o una visita sin sesión no pueden crear, editar ni borrar.

## Turno 4 · Rutas de usuario (Antonio)

Rama `feature/antonio-rutas-user` · **Del sábado 14 al miércoles 18 de noviembre**

- [ ] `/user/prestamos`: los préstamos del usuario con sesión iniciada y pedir un libro. El usuario sale de la sesión, nunca del formulario.
- [ ] `/user/reservas`: reservar un libro sin ejemplares, ver las reservas propias y cancelarlas.
- [ ] `/user/favoritos`: marcar y desmarcar libros desde su ficha y ver la lista propia.
- [ ] `/admin/prestamos`: la gestión de todos los préstamos y el registro de devoluciones.
- [ ] Añadir «Mis préstamos», «Mis reservas» y «Mis favoritos» al menú para quien tenga la sesión iniciada.
- [ ] Tests: un usuario no puede ver ni tocar los préstamos, las reservas o los favoritos de otro.

## Calendario por turnos

| Fechas | Qué | Quién |
|---|---|---|
| 30 oct – 3 nov | Base de seguridad | Erick |
| 4 – 8 nov | Diagramas, `Reserva`, favoritos en el modelo y registro | Jacobo |
| 9 – 13 nov | Rutas `/admin/*` | Carles |
| 14 – 18 nov | Rutas `/user/*` | Antonio |
| 19 nov | Congelación del código | Todos |
| 20 – 21 nov | Vídeo: cada uno graba su parte y Erick los une | Todos |
| 22 nov | Memoria, PDF y ZIP | Erick |
| **23 nov** | **Entrega** | |

Cada turno empieza con `git switch` a su rama y `git merge develop`, para partir de lo último. Erick avisa en el grupo cuando une una pull request: es la señal para que empiece el turno siguiente.

## Memoria

Cada persona envía a Erick, al acabar su turno, qué ha hecho y qué dificultades ha tenido.

## Definición de hecho

- `./mvnw verify` en verde y `docker compose up -d --build` funcionando.
- Probado en el navegador con los dos roles y sin sesión.
- Sin errores de la consola y con el diseño común (ver `CONTRIBUTING.md`).
- Pull request hacia `develop` con la integración continua en verde.
