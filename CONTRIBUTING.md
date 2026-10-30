# Guía de contribución · Biblioteca (FP065 Producto 3)

Proyecto en equipo de la asignatura FP065 (UOC): aplicación web de gestión de una biblioteca con Spring Boot,
Thymeleaf, Spring Data JPA y MariaDB, ejecutable con Docker. Aquí están las normas comunes del equipo:
léelas antes de empezar tu parte para que todas las pantallas se vean y funcionen igual.

- Enunciado completo y rúbrica: `docs/enunciado.md` · Entrega: **23 nov 2026**
- Reparto de tareas, calendario y "definición de hecho": `docs/reparto-tareas.md`
- Modelo de datos (E/R, relacional, reglas): `docs/modelo-datos.md`
- Base de partida: Producto 2 → https://github.com/EricKColl/FP.449-Producto2
- Equipo: Erick Coll Rodríguez (coordinador), Carles Miguel Millán, Jacobo Barrera Toba, Antonio Charneco Alcalá

## Stack

Java 21 · Spring Boot 4.1.1 (`spring-boot-starter-webmvc`, `-thymeleaf`, `-data-jpa`, `-validation`, `-actuator`)
· MariaDB 11.8 · H2 (tests y perfil `h2`) · Bootstrap 5.3, Tabler Icons y las fuentes Inter y Playfair Display como WebJars · Maven Wrapper.

Spring Boot 4 tiene paquetes de test distintos a los de la versión 3:
`org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`,
`org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`,
`org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase`,
`org.springframework.test.context.bean.override.mockito.MockitoBean` (en lugar de `@MockBean`).

## Comandos

```bash
./mvnw verify                                        # compila y pasa los tests (H2, sin Docker)
docker compose up -d db adminer                      # solo la BD → ejecutar la app desde el IDE
./mvnw spring-boot:run                               # app contra el MariaDB de Docker (localhost:3307)
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2 # app sin Docker (BD en memoria)
docker compose up -d --build                         # todo en Docker → http://localhost:8080
docker compose down -v                               # borra la BD; se recrea desde los scripts
```

Windows: `.\mvnw.cmd` en lugar de `./mvnw`. Adminer: http://localhost:8081 (servidor `db`, usuario/contraseña `biblioteca`).

## Estructura

```
src/main/java/edu/uoc/biblioteca/
  model/        entidades JPA: Autor, Libro, Usuario, Prestamo, EstadoPrestamo
  repository/   interfaces Spring Data (JpaRepository)
  service/      lógica de negocio (@Service, @Transactional)
  controller/   controladores MVC (@Controller)
src/main/resources/
  db/01-esquema.sql, db/02-datos.sql   esquema y datos (los usan MariaDB en Docker y el perfil h2)
  templates/fragments/layout.html      head, navbar, alertas y footer comunes
  templates/<entidad>/*.html           vistas de cada entidad
  static/css/estilos.css               estilos propios sobre Bootstrap
```

## Convenciones (obligatorias)

**Idioma**: todo en español (clases, métodos, variables, rutas, vistas, mensajes, commits), como en el Producto 1.

**Capas**: `Controller → Service → Repository`. Los controladores no usan repositorios directamente.
Inyección por constructor (sin `@Autowired` en campos). Servicios con `@Transactional(readOnly = true)` a nivel
de clase y `@Transactional` en los métodos que escriben. No se usa Lombok.

**Rutas** (los formularios HTML solo envían GET y POST):

| | |
|---|---|
| `GET /libros` | listado → `libros/lista` |
| `GET /libros/{id}` | detalle → `libros/detalle` |
| `GET /libros/nuevo` · `GET /libros/{id}/editar` | formulario → `libros/formulario` |
| `POST /libros` · `POST /libros/{id}` | crear · actualizar → redirige al listado o al detalle |
| `POST /libros/{id}/eliminar` | eliminar → redirige al listado |

Id inexistente → `throw new ResponseStatusException(HttpStatus.NOT_FOUND, "...")` (se muestra `error.html`).

**Escritura separada**: en autores y usuarios, las rutas que crean, modifican o borran están en su propio controlador
(`AutorAdminController`, `UsuarioAdminController`) y las consultas en el de siempre. En el Producto 3 (Spring Security)
esos controladores pasarán a `/admin/...`, que es donde la rúbrica pide todas las operaciones de escritura.

**Vistas**: cada página usa los fragmentos de `fragments/layout.html` (hay una plantilla mínima en su comentario):
`head('Título')`, `navbar('libros'|'autores'|'usuarios'|'prestamos'|'inicio')`, `alertas` y `footer`.
Componentes de Bootstrap 5 (tablas `table table-hover`, `card`, `btn btn-primary`, `badge`, `form-control`,
`is-invalid` + `invalid-feedback`) e iconos `ti ti-*` (Tabler). Los colores del tema están en `css/estilos.css`. Nada de CSS ni JS por CDN: todo sale de los WebJars.
No se añade CSS propio en cada vista: solo clases de Bootstrap. Si hace falta un estilo nuevo, se pide a Erick.

- Estado de un préstamo con su color: `<span th:replace="~{fragments/layout :: estadoPrestamo(${prestamo})}"></span>`.
- Fechas siempre así: `${#temporals.format(fecha, 'dd/MM/yyyy')}` (si puede ser `null`, comprobarlo antes).
- Valor vacío: `th:text="${libro.editorial} ?: '—'"`.
- Selección de entidades en un formulario (p. ej. los autores de un libro): `<select th:field="*{autores}" multiple>`
  con `th:value="${autor.id}"`. Spring Data convierte solo los ids en entidades y marca las opciones ya elegidas.
  La lista de opciones va en el modelo con otro nombre (`todosLosAutores`) para no confundirla con el campo.

**Mensajes**: tras crear, editar o eliminar, redirigir (patrón PRG) con
`redirectAttributes.addFlashAttribute("mensajeExito" | "mensajeError", "...")`; el fragmento `alertas` los muestra.

**Validación**: las restricciones ya están en las entidades (`@NotBlank`, `@Size`, `@Email`...) con mensajes en español.
En el controlador: `@Valid @ModelAttribute("libro") Libro libro, BindingResult resultado`; si hay errores, volver al formulario.
Las reglas de negocio (ISBN repetido, borrar con préstamos, ejemplares disponibles) se comprueban en el servicio.

**Relaciones LAZY**: `spring.jpa.open-in-view=true`, así que las vistas pueden recorrer `libro.autores`,
`prestamo.usuario`, etc. En listados, usar los métodos con `@EntityGraph` del repositorio para evitar N+1 consultas.

**Modelo de datos compartido**: Hibernate está en `ddl-auto=validate` y **no crea tablas**. Cualquier cambio en una
entidad exige cambiar también `db/01-esquema.sql` (y `02-datos.sql`) y `docs/modelo-datos.md`, y avisar al equipo.
`./mvnw verify` detecta si no coinciden. No cambiar a `ddl-auto=update`.

**Tests**: cada controlador nuevo, con su clase de test en `src/test/java/edu/uoc/biblioteca/controller/`.
Patrón común: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("h2")` + `@Transactional`
(aplicación completa sobre H2 con los datos de `02-datos.sql`; cada test deshace sus cambios al terminar).
Datos de prueba: 14 libros, 11 autores, 6 usuarios, 11 préstamos (7 sin devolver, 2 de ellos vencidos).

**Ficheros compartidos** (los cambia solo Erick, o se le avisa antes): `pom.xml`, `application*.properties`,
`db/*.sql`, `model/*`, `fragments/layout.html`, `estilos.css`, `Dockerfile`, `docker-compose.yml`.
Añadir un método nuevo a un repositorio sí se puede (no cambia las tablas).

**Git**: `main` ← `develop` ← `feature/<tarea>`. Pull request a `develop` con revisión. `./mvnw verify` en verde
antes de abrir la PR (el CI de GitHub Actions lo comprueba y también construye la imagen Docker).
