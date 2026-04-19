package org.ugina.ApiClient.Data;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Обёртка над java.net.http.HttpResponse.
 *
 * Зачем, если HttpResponse и так работает?
 *
 * 1. HttpResponse — generic (HttpResponse<String>), каждый раз таскать этот тип неудобно.
 *    ApiResponse — простое имя, простой тип.
 *
 * 2. HttpResponse не знает сколько длился запрос. Мы замеряем время в клиенте
 *    и сохраняем в обёртке — потом можно проверить что ответ пришёл за <500ms.
 *
 * 3. Fluent assertions — вместо трёх отдельных Assert.assertEquals / Assert.assertTrue
 *    пишем одну цепочку:
 *
 *      response.assertStatus(200)
 *              .assertBodyContains("\"email\"")
 *              .assertDurationLessThan(5000);
 *
 *    Каждый метод возвращает this — поэтому можно цеплять дальше.
 *    При первом же несовпадении — AssertionError с понятным сообщением.
 *
 * 4. Когда вынесем клиент в библиотеку — пользователи получат наш ApiResponse,
 *    а не HttpResponse из JDK. Мы сможем менять внутреннюю реализацию
 *    (например, перейти на async) без поломки API библиотеки.
 *
 * Использование:
 *
 *    ApiResponse response = apiClient.sendRequest(requestInfo);
 *
 *    // Достаём данные
 *    int status = response.statusCode();
 *    String body = response.body();
 *    String contentType = response.header("Content-Type");
 *
 *    // Fluent assertions
 *    response.assertStatus(201)
 *            .assertBodyContains("\"id\"")
 *            .assertBodyNotEmpty()
 *            .assertDurationLessThan(3000);
 */
public class ApiResponse {

    // ──── Данные ────

    // HTTP-код ответа: 200, 201, 404, 500...
    private final int statusCode;

    // Тело ответа как строка (JSON, XML, HTML, plain text...)
    private final String body;

    // Заголовки ответа.
    // Map<String, List<String>> — потому что один заголовок может иметь несколько значений.
    // Например: Set-Cookie может повторяться несколько раз.
    private final Map<String, List<String>> headers;

    // Время выполнения запроса в миллисекундах.
    // Замеряется в ApiRequestClient: start → send → end → duration = end - start.
    private final long durationMs;

    // ──── Конструктор ────

    /**
     * Создаётся внутри ApiRequestClient после получения ответа.
     * Пользователь не вызывает конструктор напрямую.
     *
     * @param httpResponse оригинальный ответ из java.net.http
     * @param durationMs   сколько миллисекунд длился запрос
     */
    public ApiResponse(HttpResponse<String> httpResponse, long durationMs) {
        this.statusCode = httpResponse.statusCode();
        this.body = httpResponse.body();
        this.headers = httpResponse.headers().map();
        this.durationMs = durationMs;
    }

    // ──── Доступ к данным ────

    /**
     * HTTP-код ответа.
     * 2xx — успех, 4xx — ошибка клиента, 5xx — ошибка сервера.
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Тело ответа как строка.
     */
    public String body() {
        return body;
    }

    /**
     * Время выполнения запроса в миллисекундах.
     */
    public long durationMs() {
        return durationMs;
    }

    /**
     * Все заголовки ответа.
     */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /**
     * Первое значение заголовка по имени.
     * Возвращает null если заголовок отсутствует.
     *
     * Пример: response.header("Content-Type") → "application/json; charset=utf-8"
     *
     * Имя регистронезависимо — HTTP-заголовки case-insensitive,
     * но java.net.http хранит их в нижнем регистре.
     */
    public String header(String name) {
        List<String> values = headers.get(name.toLowerCase());
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    /**
     * Все значения заголовка (для заголовков с несколькими значениями, например Set-Cookie).
     */
    public List<String> headerValues(String name) {
        return headers.getOrDefault(name.toLowerCase(), List.of());
    }

    // ──── Проверки без assert (boolean) ────

    /**
     * true если код 2xx (200, 201, 204...).
     */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * true если код 4xx (400, 401, 403, 404...).
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    /**
     * true если код 5xx (500, 502, 503...).
     */
    public boolean isServerError() {
        return statusCode >= 500;
    }

    // ──── Fluent Assertions ────
    //
    // Каждый метод:
    //   1. Проверяет условие
    //   2. Если НЕ выполнено — бросает AssertionError с понятным сообщением
    //   3. Если выполнено — возвращает this (себя) для цепочки вызовов
    //
    // AssertionError — тот же тип что бросает TestNG Assert.
    // Тест упадёт с тем же результатом, как если бы ты написал Assert.assertEquals.

    /**
     * Проверяет HTTP-код ответа.
     *
     * response.assertStatus(200)  — ожидаем успех
     * response.assertStatus(201)  — ожидаем создание ресурса
     * response.assertStatus(404)  — ожидаем "не найдено"
     */
    public ApiResponse assertStatus(int expected) {
        if (statusCode != expected) {
            throw new AssertionError(String.format(
                    "Expected status %d, got %d\nBody: %s",
                    expected, statusCode, truncate(body, 500)));
        }
        return this;
    }

    /**
     * Проверяет что тело содержит подстроку.
     *
     * response.assertBodyContains("\"email\"")  — есть поле email
     * response.assertBodyContains("John")       — есть имя John
     */
    public ApiResponse assertBodyContains(String substring) {
        if (body == null || !body.contains(substring)) {
            throw new AssertionError(String.format(
                    "Expected body to contain \"%s\"\nActual body: %s",
                    substring, truncate(body, 500)));
        }
        return this;
    }

    /**
     * Проверяет что тело НЕ содержит подстроку.
     *
     * response.assertBodyNotContains("error")  — нет ошибки в ответе
     */
    public ApiResponse assertBodyNotContains(String substring) {
        if (body != null && body.contains(substring)) {
            throw new AssertionError(String.format(
                    "Expected body NOT to contain \"%s\"\nActual body: %s",
                    substring, truncate(body, 500)));
        }
        return this;
    }

    /**
     * Проверяет что тело не пустое.
     */
    public ApiResponse assertBodyNotEmpty() {
        if (body == null || body.trim().isEmpty()) {
            throw new AssertionError("Expected non-empty body, got empty");
        }
        return this;
    }

    /**
     * Проверяет что запрос выполнился быстрее указанного времени.
     *
     * response.assertDurationLessThan(5000)  — быстрее 5 секунд
     * response.assertDurationLessThan(500)   — быстрее полсекунды
     */
    public ApiResponse assertDurationLessThan(long maxMs) {
        if (durationMs > maxMs) {
            throw new AssertionError(String.format(
                    "Expected response faster than %dms, took %dms", maxMs, durationMs));
        }
        return this;
    }

    /**
     * Проверяет значение заголовка.
     *
     * response.assertHeader("content-type", "application/json")
     */
    public ApiResponse assertHeader(String name, String expectedValue) {
        String actual = header(name);
        if (actual == null) {
            throw new AssertionError(String.format(
                    "Expected header \"%s\" to be \"%s\", but header is missing",
                    name, expectedValue));
        }
        if (!actual.contains(expectedValue)) {
            throw new AssertionError(String.format(
                    "Expected header \"%s\" to contain \"%s\"\nActual: \"%s\"",
                    name, expectedValue, actual));
        }
        return this;
    }

    /**
     * Проверяет что код ответа в диапазоне 2xx.
     */
    public ApiResponse assertSuccess() {
        if (!isSuccess()) {
            throw new AssertionError(String.format(
                    "Expected success (2xx), got %d\nBody: %s",
                    statusCode, truncate(body, 500)));
        }
        return this;
    }

    // ──── toString ────

    @Override
    public String toString() {
        return String.format("ApiResponse{status=%d, duration=%dms, bodyLength=%d}",
                statusCode, durationMs, body != null ? body.length() : 0);
    }

    // ──── Утилита ────

    /**
     * Обрезает строку для вывода в ошибках — чтобы 10-килобайтный JSON
     * не засорял сообщение об ошибке.
     */
    private static String truncate(String s, int maxLen) {
        if (s == null) return "null";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "... (" + s.length() + " chars total)";
    }
}