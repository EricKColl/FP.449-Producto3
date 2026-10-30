# Producto 3 · Seguridad y usuarios (FP065)

> Copia del enunciado y de la rúbrica del aula virtual, para tenerlos siempre junto al código.
> Fecha de entrega: **lunes 23 de noviembre de 2026**.

## Descripción

En cualquier aplicación son esenciales los usuarios, tanto para la securización de la aplicación como para la administración y gestión de las acciones que se pueden realizar.

Mediante Spring.io podemos recubrir nuestro primer desarrollo con esa gestión de usuarios, clientes y roles.

## Objetivos

- Modificar nuestra base de datos para la gestión con usuarios.
- Utilizar, configurar e implementar Spring Security para poder gestionar los diferentes usuarios.

## Pasos a seguir

1. Leer detenidamente estas instrucciones e identificar los requerimientos de la actividad.
2. Revisar detenidamente la rúbrica de evaluación.
3. Consultar los recursos necesarios facilitados en el aula.
4. Modificar nuestro diagrama E/R para la incorporación de usuarios a nuestra aplicación tanto desde el punto de vista de la securización como desde el punto de vista de la lógica de la misma.
5. Incorporar Spring Security dentro de la aplicación. Para ello:
   - Se modificarán las configuraciones del proyecto para incorporar Spring Security.
   - Incorporar el formulario de login y registro de usuario.
6. Se modifican las relaciones en al menos una de las entidades propuestas, ya que los usuarios interactúan con el resto de entidades. En el caso de la aplicación de Alquiler de vehículos, los usuarios no sólo sirven para el login, sino que pueden realizar Alquiler de vehículos. Por lo tanto se realizarán los siguientes cambios en la aplicación:
   - Se añadirán los modelos necesarios nuevos.
   - Se modificarán los modelos con las anotaciones necesarias.
   - Se modificarán las vistas y controladores.
7. Se añade una gestión por roles de las diferentes partes de la aplicación. Se añaden las siguientes rutas:
   - Una ruta `/admin/*` en la que únicamente tendrá acceso el rol ADMIN. Aquí se moverán todas las rutas que creen, modifiquen o borren filas de los diferentes modelos.
   - Una ruta `/user/*`, donde el usuario con acceso podrá interactuar con partes de la aplicación.
8. Realizad las modificaciones oportunas del contenedor Docker.

## Requisitos

- Partir del diseño MVC del Producto 2.
- Disponer del framework de programación en Spring o Java EE.

## Entrega

- Por el canal indicado en el aula virtual.
- Formato: ZIP o RAR.
- Memoria del trabajo: trabajo realizado, dificultades, puntos no realizados, propuestas de mejora, autoevaluación, webgrafía.
- Nombre del archivo: `producto3(FP065)_Apellido1Apellido2_Nombre`.
- Vídeo o enlace de YouTube mostrando el funcionamiento de la aplicación.

## Rúbrica (máximo 25 puntos, aprobado con 12,5)

### Modificación del diagrama E/R y modelos dentro de la aplicación (8 puntos)

| Puntos | Nivel |
|---|---|
| **8** | Se han realizado las modificaciones en el diagrama E/R para incluir a los usuarios con al menos 5 atributos, y al menos una nueva relación con entidades anteriores. Se han reflejado estas modificaciones dentro de los modelos de Spring. |
| 6 | Igual, con al menos 4 atributos y al menos una nueva relación con entidades anteriores. |
| 4 | Igual, con al menos 3 atributos y al menos una nueva relación con entidades anteriores. |
| 2 | Usuarios con al menos 5 atributos, reflejados en los modelos de Spring. |

### Configuración e implementación de Spring Security (9 puntos)

| Puntos | Nivel |
|---|---|
| **9** | Se ha configurado y añadido a la aplicación Spring Security con un formulario de login y otro de registro, modificando o añadiendo los controllers necesarios. |
| 6,75 | Spring Security con un formulario de login, modificando o añadiendo los controllers necesarios. |
| 2,25 | Spring Security con un formulario de login. |

### Gestión por roles (8 puntos)

| Puntos | Nivel |
|---|---|
| **8** | Se añaden al menos 2 grupos de rutas, admin y user, con las que se implementa el control de acceso por roles a determinadas partes de la app. Se modifica el menú y las vistas de forma dinámica de acuerdo al rol del usuario. |
| 6 | Al menos 2 grupos de rutas, admin y user, con control de acceso por roles. |
| 2 | 1 grupo de rutas, user, con control de acceso por roles. |
