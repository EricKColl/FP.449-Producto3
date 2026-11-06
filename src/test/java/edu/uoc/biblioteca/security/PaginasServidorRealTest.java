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

    @Test
    void laPortadaLlegaEnteraConSuPoliticaDeSeguridad() throws Exception {
        HttpResponse<String> respuesta = pedir("/");
        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("</html>");
        assertThat(respuesta.headers().firstValue("Content-Security-Policy")).hasValueSatisfying(
                politica -> assertThat(politica).contains("script-src 'self'"));
    }
}
