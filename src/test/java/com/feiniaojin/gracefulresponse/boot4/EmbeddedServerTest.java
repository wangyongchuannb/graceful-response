package com.feiniaojin.gracefulresponse.boot4;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Boot4CompatibilityTest.Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmbeddedServerTest {
    @Value("${local.server.port}") int port;
    private final HttpClient client = HttpClient.newHttpClient();

    @Test void servesWrappedJsonOverRealHttp() throws Exception {
        HttpResponse<String> response = get("/boot4/object");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"code\":\"0\"", "\"data\":{\"name\":\"boot4\"}");
    }
    @Test void servesBusinessErrorOverRealHttp() throws Exception {
        HttpResponse<String> response = get("/boot4/business-error");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"code\":\"BUSINESS\"", "business failed");
    }
    @Test void servesUnwrappedPlainTextOverRealHttp() throws Exception {
        HttpResponse<String> response = get("/boot4/text");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("plain");
    }
    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
