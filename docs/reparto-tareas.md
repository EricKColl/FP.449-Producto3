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

Rama `feature/jacobo-er-registro` · **Del miércoles 4 al domingo 8 de noviembre** · Terminado el 2 de noviembre

- [x] Diagrama E/R con los atributos nuevos de Usuario y las relaciones Usuario–Reserva–Libro y Usuario–Favorito–Libro.
- [x] Modelo relacional actualizado y `docs/modelo-datos.md`.
- [x] Tabla `reserva`, entidad `Reserva` y repositorio, con las relaciones en `Usuario` y `Libro`.
- [x] Tabla `usuario_favorito` y `@ManyToMany` entre `Usuario` y `Libro`.
- [x] Formulario de registro público (`/registro`): validación, DNI y email únicos, contraseña cifrada y rol `USER` siempre.
- [x] Abrir `/registro` en `SecurityConfig` y añadir el enlace «Crear cuenta» en el login y en el menú sin sesión.
- [x] Tests del registro y de los repositorios: `./mvnw verify` da **100 tests**.

## Turno 3 · Rutas de administración (Carles)

Rama `feature/carles-rutas-admin` · **Del lunes 9 al viernes 13 de noviembre** · Terminado el 3 de noviembre

- [x] Mover a `/admin/*` todas las rutas de libros, autores y usuarios que crean, modifican o borran filas.
- [x] Separar la escritura de libros en `LibroAdminController`, como ya están `AutorAdminController` y `UsuarioAdminController`.
- [x] Actualizar enlaces y formularios: los botones de crear, editar y eliminar solo se ven con rol ADMIN.
- [x] Quitar de `SecurityConfig` las reglas temporales de `/libros/nuevo`, `/libros/*/editar`, `/autores/nuevo` y `/autores/*/editar`.
- [x] Tests: un USER o una visita sin sesión no pueden crear, editar ni borrar: `./mvnw verify` da **108 tests**.

## Turno 4 · Rutas de usuario (Antonio)

Rama `feature/antonio-rutas-user` · **Del sábado 14 al miércoles 18 de noviembre** · Terminado el 5 de noviembre

- [x] `/user/prestamos`: los préstamos del usuario con sesión iniciada y pedir un libro. El usuario sale de la sesión, nunca del formulario.
- [x] `/user/reservas`: reservar un libro sin ejemplares, ver las reservas propias y cancelarlas.
- [x] `/user/favoritos`: marcar y desmarcar libros desde su ficha y ver la lista propia.
- [x] `/admin/prestamos`: la gestión de todos los préstamos y el registro de devoluciones.
- [x] Añadir «Mis préstamos», «Mis reservas» y «Mis favoritos» al menú para quien tenga la sesión iniciada.
- [x] Tests: un usuario no puede ver ni tocar los préstamos, las reservas o los favoritos de otro: `./mvnw verify` da **124 tests**.

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

## Vídeo (9 min): presentación y cuatro partes en el orden de los turnos

El vídeo enseña **toda la seguridad funcionando**: cómo entra cada perfil y qué puede hacer. Sigue el orden de los turnos,
de la visita sin sesión al administrador. Cada persona explica **unos 2 minutos** con su voz, empezando por lo que ha hecho.
Erick abre el vídeo justo después de la portada animada (2:30) y lo cierra (0:30), y une todos los cortes.

| Orden | Quién | Duración | Pantallas | Qué enseña |
|---|---|---|---|---|
| 1 | Erick | 2:30 | Cartelas, IntelliJ, Docker, Adminer, login | Presentación del equipo y del producto. Tecnologías nuevas (Spring Security 7, BCrypt, Thymeleaf Extras Security) y el recorrido de una petición por la cadena de seguridad. En IntelliJ IDEA: las dependencias del `pom.xml` y `SecurityConfig` con las reglas por rol. `docker compose up -d --build` y `docker compose ps` con los contenedores `biblioteca-p3-*`. Adminer: la tabla `usuario` con las contraseñas cifradas y el rol. En la aplicación: el catálogo sin sesión, un login con la contraseña mal (sale el error), el login de `admin@example.com` con su menú, el cierre de sesión y la página 403 al abrir `/admin/usuarios` con `laura.martinez@example.com`. |
| 2 | Jacobo | 2:00 | Diagramas, Adminer, registro | Diagrama E/R y modelo relacional: la relación con atributos `Reserva` y la N:M `usuario_favorito`. Las dos tablas en Adminer. Registro en «Crear cuenta»: primero con errores (DNI repetido `12345678Z`, email repetido `laura.martinez@example.com` y una contraseña corta) y después bien, con el medidor de fuerza: *Sara Ortega Gil*, DNI `11223344B`, `sara.ortega@example.com`, contraseña `Lectora-2026!`. Iniciar sesión con la cuenta nueva: menú de usuaria y rol `USER` en Adminer, aunque nadie lo eligió en el formulario. |
| 3 | Carles | 2:00 | Libros, autores, rutas | Con `admin@example.com`: el catálogo con «Nuevo libro» y, en la vista de lista, las acciones de cada fila. Crear en `/admin/libros/nuevo` primero con errores y después bien: *Eva Luna*, ISBN `978-84-9759-243-7`, de Isabel Allende, con la vista previa de la portada. Editarlo y eliminarlo; *Don Quijote de la Mancha* no se deja borrar porque tiene préstamos. Alta y baja de un autor. Las mismas pantallas con Laura, sin botones. La ruta antigua `/libros/nuevo` da 404 y `/admin/libros/nuevo` con Laura da 403. |
| 4 | Antonio | 2:00 | Mis préstamos, reservas, favoritos, préstamos | Con `laura.martinez@example.com`: «Mis préstamos» y renovar *Cien años de soledad*; pedir *1984*. Reservar *El señor de los anillos*, que no tiene ejemplares libres, ver su puesto en la cola y cancelar la reserva. Marcar y quitar un favorito. `/admin/prestamos` con Laura da 403. Con `admin@example.com`: la gestión de todos los préstamos por estado y registrar una devolución. |
| 5 | Erick | 0:30 | Avisos, móvil, tests, GitHub | Cierre: un aviso en tiempo real, la aplicación en el móvil con el menú plegado, `./mvnw verify` con los tests en verde y las comprobaciones automáticas de GitHub. |

Cada parte empieza en la pantalla donde acaba la anterior, así los cortes se unen sin saltos. Si alguien se pasa de
2:15, que lo regrabe: el total no debe superar los 9 minutos.

**Cómo grabar tu parte**

1. Después de la congelación (jueves 19), ponte al día: `git switch develop` y `git pull`.
2. Si tienes arrancado el Producto 2, páralo (`docker compose stop` en su carpeta), y para la aplicación de IntelliJ (■).
3. Arranca todo como en la entrega, con los datos de prueba limpios: `docker compose down -v` y `docker compose up -d --build`.
   Espera a que `docker compose ps` diga *healthy* en `biblioteca-p3-app`. Repítelo antes de cada toma si has cambiado datos.
4. Prepara Chrome: ventana maximizada, zoom al 100 % (`Ctrl`+`0`), barra de marcadores oculta (`Ctrl`+`Mayús`+`B`) y
   notificaciones del sistema desactivadas («No molestar» o «Concentración»). Que no se vea nada personal.
5. Graba la **pantalla completa con micrófono** en formato mp4, a 1920×1080. Recomendado: **OBS Studio** (Windows y Mac).
   Sin instalar nada: `Cmd`+`Mayús`+`5` en Mac; en Windows, `Win`+`Alt`+`R` graba solo la ventana activa (sirve si solo enseñas el navegador).
6. Habla como si se lo explicaras al profesor: qué haces y por qué funciona así. Mueve el ratón despacio y ensáyalo una vez antes de grabar.
7. Llama al fichero `video-<orden>-<nombre>.mp4` (por ejemplo `video-3-carles.mp4`) y envíaselo a Erick
   (Google Drive o WeTransfer) **el sábado 21**.

**Detalles de la aplicación que conviene saber antes de grabar**

- Cuentas: `admin@example.com` / `Admin1234` y `laura.martinez@example.com` / `Usuario1234`. Adminer en
  http://localhost:8081 (servidor `db`, usuario `biblioteca`, contraseña `biblioteca`, base de datos `biblioteca`).
- Cinco contraseñas mal seguidas con el mismo email bloquean ese email cinco minutos: si pasa, espera o usa otra cuenta.
- En «Mis préstamos», «Mis reservas» y «Mis favoritos» se abre un consejo abajo a la derecha: ciérralo con «Entendido»
  o la X antes de explicar.
- Borrar o cancelar abre una ventana propia de confirmación: hay que pulsar «Sí, eliminar» o «Sí, cancelarla».
- Si Docker Desktop no arranca en Windows con un error de `engine.sock` o `dockerInference`, **no** pulses
  «Reset to factory defaults»: avisa a Erick.

**Montaje (Erick)**: une los cortes en orden con la presentación y el cierre, sube el vídeo a YouTube como **Oculto** y
pasa el enlace al grupo el **domingo 22**: va en la memoria y en el ZIP de la entrega.

## Definición de hecho

- `./mvnw verify` en verde y `docker compose up -d --build` funcionando.
- Probado en el navegador con los dos roles y sin sesión.
- Sin errores de la consola y con el diseño común (ver `CONTRIBUTING.md`).
- Pull request hacia `develop` con la integración continua en verde.
