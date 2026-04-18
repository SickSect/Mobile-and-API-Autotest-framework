package org.ugina.ApiClient.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Контейнер ВСЕХ данных, необходимых для формирования HTTP-запроса.
 *
 * сначала собираем ВСЁ в этот объект, потом передаём в ApiRequestClient,
 * который уже строит из него java.net.http.HttpRequest.
 *
 * Разделение ответственности:
 *   RequestInfo  — ЧТО отправить (данные)
 *   ApiRequestClient — КАК отправить (транспорт)
 *
 * HTTP-запрос состоит из:
 *
 *   ┌─────────────────────────────────────────────────────┐
 *   │  Request Line (стартовая строка)                    │
 *   │    POST /api/users?page=1&limit=10 HTTP/1.1         │
 *   │    ───── ──────────────────────────                 │
 *   │    метод  путь + query-параметры                    │
 *   ├─────────────────────────────────────────────────────┤
 *   │  Headers (заголовки)                                │
 *   │    Host: api.example.com                            │
 *   │    Content-Type: application/json                   │
 *   │    Authorization: Bearer eyJhbGciOi...              │
 *   │    Accept: application/json                         │
 *   │    User-Agent: MyFramework/1.0                      │
 *   ├─────────────────────────────────────────────────────┤
 *   │  Body (тело) — только для POST, PUT, PATCH          │
 *   │    {"name": "John", "email": "john@example.com"}    │
 *   └─────────────────────────────────────────────────────┘
 *
 */
public class RequestInfo {

    // ──── 1. МЕТОД ────
    // GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS
    // Определяет ЧТО мы хотим сделать:
    //   GET    — получить данные
    //   POST   — создать ресурс
    //   PUT    — полностью заменить ресурс
    //   PATCH  — частично обновить ресурс
    //   DELETE — удалить ресурс
    //   HEAD   — как GET, но без тела ответа (только заголовки)
    //   OPTIONS — узнать какие методы поддерживает сервер
    private String method;

    // ──── 2. ПУТЬ (path) ────
    // Относительный путь к ресурсу: "/api/users", "/posts/1", "/auth/login"
    // Базовый URL (https://api.example.com) хранится в ApiRequestClient.
    // Итоговый URL = baseUrl + path → "https://api.example.com/api/users"
    private String path;

    // ──── 3. QUERY-ПАРАМЕТРЫ ────
    // Параметры в URL после знака "?":
    //   /users?page=1&limit=10&sort=name
    //          ────── ──────── ─────────
    //          key=val key=val  key=val
    // Используются для фильтрации, пагинации, сортировки.
    // LinkedHashMap сохраняет порядок вставки — URL будет предсказуемым.
    private Map<String, String> queryParams;

    // ──── 4. ЗАГОЛОВКИ (headers) ────
    // Метаданные запроса — говорят серверу КАК обрабатывать запрос.
    // Основные:
    //   Content-Type   — формат тела ("application/json", "application/xml")
    //   Accept         — какой формат ответа хотим получить
    //   Authorization  — токен или credentials для аутентификации
    //   User-Agent     — кто отправляет запрос (имя клиента)
    //   Cookie         — куки сессии
    //   Cache-Control  — инструкции кэширования
    //
    // Content-Type выставляется автоматически из IRequestBody.contentType(),
    // но можно переопределить вручную через headers.
    private Map<String, String> headers;

    // ──── 5. ТЕЛО (body) ────
    // Данные, которые отправляем серверу. Есть только у POST, PUT, PATCH.
    // У GET и DELETE тела нет (технически можно, но это плохая практика).
    // IRequestBody абстрагирует формат — JSON, XML, form-data и т.д.
    private IRequestBody body;

    // ──── 6. ТАЙМАУТ ЗАПРОСА ────
    // Сколько секунд ждать ответа от сервера.
    // Отличие от connectTimeout в HttpClient:
    //   connectTimeout — время на УСТАНОВКУ соединения (TCP handshake)
    //   requestTimeout — время на ПОЛУЧЕНИЕ ответа (сервер думает)
    // Если null — используется дефолт из клиента.
    private Integer requestTimeoutSeconds;

    // ──── Конструктор ────

    public RequestInfo() {
        this.headers = new LinkedHashMap<>();
        this.queryParams = new LinkedHashMap<>();
    }

    // ──── Геттеры и сеттеры ────

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public IRequestBody getBody() {
        return body;
    }

    public void setBody(IRequestBody body) {
        this.body = body;
    }

    public Integer getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(Integer requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    // ──── Удобные методы для добавления данных по одному ────

    public void addHeader(String name, String value) {
        this.headers.put(name, value);
    }

    public void addQueryParam(String key, String value) {
        this.queryParams.put(key, value);
    }
}