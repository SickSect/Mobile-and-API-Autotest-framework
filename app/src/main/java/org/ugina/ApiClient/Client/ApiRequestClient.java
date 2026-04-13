package org.ugina.ApiClient.Client;

import org.ugina.ApiClient.Config.ApiClientConfigReader;
import org.ugina.ApiClient.Data.IRequestBody;
import org.ugina.ApiClient.utils.Log;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ApiRequestClient  {

    private static final Log log = Log.forClass(ApiRequestClient.class);

    private HttpClient httpClient;
    private String baseUrl;

    public ApiRequestClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Integer.parseInt(ApiClientConfigReader.get("timeout.connection"))))
                .followRedirects(HttpClient.Redirect.NORMAL) // CONFIG IN NEXT STEP
                .build();
    }

    private void logRequest(Object request) {
        if (request == null) {
            log.info("=== HTTP REQUEST [NULL] ===");
            return;
        }

        log.info("=== HTTP REQUEST START ===");

        try {
            log.info("Request class: {}", request.getClass().getName());
            log.info("Hint: Specify your HTTP library to get exact logging code");

        } catch (Exception e) {
            log.warn("Failed to log request: {}", e.getMessage());
            log.debug("Details:", e);
        }

        log.info("=== HTTP REQUEST END ===\n");
    }


    /**
     * GET-запрос. Без тела — тут ничего не изменилось.
     */
    public HttpResponse<String> get(String path) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .build();
        logRequest(request);
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * POST-запрос с абстрактным телом.
     *
     * Раньше:  post("/users", jsonString)           — только JSON, хардкод
     * Теперь:  post("/users", new JsonBody(json))    — JSON
     *          post("/users", new XmlBody(xml))      — XML
     *          post("/users", new MyCustomBody(...))  — что угодно
     *
     * Content-Type выставляется автоматически из body.contentType().
     * Клиенту не нужно знать формат — он просто берёт строку и тип.
     */
    public HttpResponse<String> post(String path, IRequestBody body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", body.contentType())
                .POST(HttpRequest.BodyPublishers.ofString(body.content()))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
