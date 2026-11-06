package edu.uoc.biblioteca.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Páginas pedidas a un servidor de verdad, como lo hace un navegador que llega por primera vez.
 * MockMvc no envía la respuesta por partes, así que no detecta el fallo de crear la sesión del token CSRF
 * cuando la página ya se está enviando: aquí sí se vería, porque la página llegaría cortada.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class PaginasServidorRealTest {

    @LocalServerPort
    private int puerto;

    private final HttpClient cliente = HttpClient.newHttpClient();

    private HttpResponse<String> pedir(String ruta) throws Exception {
        return cliente.send(HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void elLoginLlegaEnteroAUnaVisitaNueva() throws Exception {
        HttpResponse<String> respuesta = pedir("/login");
        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("name=\"_csrf\"").contains("</html>");
        assertThat(respuesta.headers().firstValue("Set-Cookie")).hasValueSatisfying(c -> assertThat(c).contains("JSESSIONID"));
    }

    @Test
    void elRegistroLlegaEnteroAUnaVisitaNueva() throws Exception {
        HttpResponse<String> respuesta = pedir("/registro");
        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("name=\"_csrf\"").contains("</html>");
    }

    /**
     * Las conexiones de avisos en tiempo real duran minutos. Si cada una retuviera una conexión de la base de datos,
     * con unas pocas páginas abiertas el pool se agotaría y la web dejaría de responder. Aquí se abren más
     * conexiones de avisos que conexiones tiene el pool (10) y la web tiene que seguir respondiendo al momento.
     */
    @Test
    void muchasConexionesDeAvisosNoBloqueanLaWeb() throws Exception {
        HttpClient navegador = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .cookieHandler(new java.net.CookieManager()).build();
        String base = "http://localhost:" + puerto;
        String login = navegador.send(HttpRequest.newBuilder(URI.create(base + "/login")).build(),
                HttpResponse.BodyHandlers.ofString()).body();
        java.util.regex.Matcher token = java.util.regex.Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(login);
        assertThat(token.find()).isTrue();
        String formulario = "email=laura.martinez%40example.com&password=Usuario1234&_csrf="
                + java.net.URLEncoder.encode(token.group(1), java.nio.charset.StandardCharsets.UTF_8);
        navegador.send(HttpRequest.newBuilder(URI.create(base + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formulario)).build(), HttpResponse.BodyHandlers.discarding());

        java.util.List<java.io.InputStream> flujos = new java.util.ArrayList<>();
        try {
            for (int i = 0; i < 15; i++) {
                HttpResponse<java.io.InputStream> flujo = navegador.send(HttpRequest.newBuilder(URI.create(base + "/avisos/flujo"))
                        .header("Accept", "text/event-stream").build(), HttpResponse.BodyHandlers.ofInputStream());
                assertThat(flujo.statusCode()).isEqualTo(200);
                flujos.add(flujo.body());
            }
            HttpResponse<String> pagina = navegador.send(HttpRequest.newBuilder(URI.create(base + "/libros"))
                    .timeout(java.time.Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(pagina.statusCode()).isEqualTo(200);
            assertThat(pagina.body()).contains("data-campana");
        } finally {
            for (java.io.InputStream flujo : flujos) {
                flujo.close();
            }
        }
    }

    @Test
    void laPortadaLlegaEnteraConSuPoliticaDeSeguridad() throws Exception {
        HttpResponse<String> respuesta = pedir("/");
        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("</html>");
        assertThat(respuesta.headers().firstValue("Content-Security-Policy")).hasValueSatisfying(
                politica -> assertThat(politica).contains("script-src 'self'"));
    }
}
