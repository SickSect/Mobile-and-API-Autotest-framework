# Framework Usage Guide

This guide is written for those who are new to test automation. Every example is working code that you can copy and run.

## Table of Contents

1. [How to create a test class](#1-how-to-create-a-test-class)
2. [How to send a request](#2-how-to-send-a-request)
3. [How to check the status code](#3-how-to-check-the-status-code)
4. [How to check response content](#4-how-to-check-response-content)
5. [How to send a request with body (POST)](#5-how-to-send-a-request-with-body-post)
6. [How to work with a database](#6-how-to-work-with-a-database)
7. [How to send requests to different hosts](#7-how-to-send-requests-to-different-hosts)
8. [How to work with multiple databases](#8-how-to-work-with-multiple-databases)
9. [How to work with tokens (authentication)](#9-how-to-work-with-tokens-authentication)
10. [How to name tests for Allure reports](#10-how-to-name-tests-for-allure-reports)
11. [How to run tests](#11-how-to-run-tests)
12. [Cheat sheet](#12-cheat-sheet)

---

## 1. How to create a test class

Each test file is a Java class with methods marked `@Test`. The TestNG framework finds these methods and runs them.

```java
package org.ugina.apiTests;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.ugina.ApiClient.Client.ApiClientProvider;
import org.ugina.ApiClient.Client.ApiRequestClient;
import org.ugina.ApiClient.Data.RequestInfo;

public class MyFirstTest {

    private ApiRequestClient client;

    // @BeforeClass — runs ONCE before all tests in this class.
    // Here we set up the client: specify which server to send requests to.
    @BeforeClass
    public void setUp() {
        // has() checks: does a client with this name already exist?
        // If yes — don't recreate, use the existing one.
        // Without this check, every test class would recreate the client.
        if (!ApiClientProvider.has("myapi")) {
            ApiClientProvider.register("myapi", "https://jsonplaceholder.typicode.com");
        }
        client = ApiClientProvider.get("myapi");
    }

    // @Test — this is one test. Each @Test method is a separate check.
    @Test
    public void myFirstTest() throws Exception {
        // Test code goes here
    }
}
```

**What's happening here:**
- `@BeforeClass` — setup. Runs once before all tests.
- `ApiClientProvider.register("myapi", "...")` — registers a host under the name "myapi".
- `ApiClientProvider.get("myapi")` — gets the client for this host.
- `@Test` — a test method. Write as many `@Test` methods as you need.

---

## 2. How to send a request

Sending a request takes three steps: create a `RequestInfo`, fill it in, send it via `client`.

```java
@Test
public void testGetUsers() throws Exception {
    // Step 1: Create a request object
    RequestInfo request = new RequestInfo();

    // Step 2: Fill in — WHAT we're sending
    request.setMethod("GET");          // HTTP method: GET, POST, PUT, DELETE, PATCH
    request.setPath("/users");         // Path (appended to the base URL)

    // Step 3: Send
    client.sendRequest(request);
}
```

**What are HTTP methods:**
- `GET` — retrieve data (list of users, product info)
- `POST` — create something new (a new user, an order)
- `PUT` — fully replace an existing resource (update all user fields)
- `PATCH` — partially update (change only the email)
- `DELETE` — delete

---

## 3. How to check the status code

Every HTTP response contains a numeric code. The main ones:
- `200` — success
- `201` — created successfully
- `400` — bad request (invalid data)
- `401` — unauthorized (no token or token is invalid)
- `404` — not found
- `500` — server error

```java
@Test
public void testStatusCode() throws Exception {
    RequestInfo request = new RequestInfo();
    request.setMethod("GET");
    request.setPath("/users");

    // .assertStatus(200) — checks that the server returned code 200.
    // If the code is different — the test fails with a clear message:
    // "Expected status 200, got 404"
    client.sendRequest(request)
            .assertStatus(200);
}
```

---

## 4. How to check response content

The server returns data in the response body (usually JSON). You can check that it contains the right words or values.

```java
@Test
public void testResponseContent() throws Exception {
    RequestInfo request = new RequestInfo();
    request.setMethod("GET");
    request.setPath("/users/1");

    client.sendRequest(request)
            .assertStatus(200)                          // status 200
            .assertBodyContains("Leanne Graham")        // response contains this name
            .assertBodyContains("\"email\"")            // response has an email field
            .assertBodyNotContains("error")             // response does NOT contain "error"
            .assertBodyNotEmpty()                       // response is not empty
            .assertDurationLessThan(5000);              // response arrived in less than 5 seconds
}
```

**All checks can be chained.** Each check returns the same response object, so you can write `.assertStatus(200).assertBodyContains("...").assertDurationLessThan(5000)` in one chain.

**If you need to save a response and use it later:**

```java
@Test
public void testSaveAndCompare() throws Exception {
    // Send the first request and save the response
    RequestInfo request1 = new RequestInfo();
    request1.setMethod("GET");
    request1.setPath("/users/1");
    ApiResponse response1 = client.sendRequest(request1);

    // ... send 3 more requests, do something ...

    // Send the fifth request
    RequestInfo request5 = new RequestInfo();
    request5.setMethod("GET");
    request5.setPath("/users/1");
    ApiResponse response5 = client.sendRequest(request5);

    // Compare data from the first and fifth requests
    Assert.assertEquals(response1.body(), response5.body(),
            "Data should not change between requests");
}
```

**Saving data between requests is the framework's key advantage.** You can save the response of the first request and compare it with the fifth. Everything lives in one method — no passing state between classes.

---

## 5. How to send a request with body (POST)

When creating a resource (POST, PUT, PATCH), you need to send data — the request body.

```java
import org.ugina.ApiClient.Data.JsonRequestBody;

@Test
public void testCreateUser() throws Exception {
    RequestInfo request = new RequestInfo();
    request.setMethod("POST");
    request.setPath("/users");

    // Request body — a JSON string with the new user's data
    request.setBody(new JsonRequestBody("""
            {
                "name": "John",
                "email": "john@example.com",
                "age": 30
            }
            """));

    client.sendRequest(request)
            .assertStatus(201)              // 201 = created
            .assertBodyContains("\"id\"");  // server returned the new resource's ID
}
```

**With headers and query parameters:**

```java
@Test
public void testFullRequest() throws Exception {
    RequestInfo request = new RequestInfo();
    request.setMethod("POST");
    request.setPath("/users");

    // Headers — metadata about the request
    request.addHeader("Accept", "application/json");
    request.addHeader("X-Request-Id", "test-123");

    // Query parameters — appended to the URL: /users?source=test&version=2
    request.addQueryParam("source", "test");
    request.addQueryParam("version", "2");

    // Body
    request.setBody(new JsonRequestBody("""
            {"name": "John", "email": "john@example.com"}
            """));

    client.sendRequest(request)
            .assertStatus(201);
}
```

**If the body is XML instead of JSON:**

```java
import org.ugina.ApiClient.Data.XmlRequestBody;

request.setBody(new XmlRequestBody("""
        <?xml version="1.0" encoding="UTF-8"?>
        <user>
            <n>John</n>
            <email>john@example.com</email>
        </user>
        """));
```

---

## 6. How to work with a database

### Connection

```java
import org.ugina.ApiClient.Db.JdbcDbClient;
import org.ugina.ApiClient.Db.IDbClient;

// PostgreSQL
IDbClient db = new JdbcDbClient(
        "jdbc:postgresql://localhost:5432/mydb", "user", "password");
db.connect();

// MySQL
IDbClient db = new JdbcDbClient(
        "jdbc:mysql://localhost:3306/mydb", "root", "password");
db.connect();
```

### SELECT — check what's in the database

```java
// Get all active users older than 25
List<Map<String, Object>> users = db.select("users")
        .columns("id", "name", "email")       // which columns
        .where("status", "active")             // WHERE status = 'active'
        .where("age", ">", 25)                 // AND age > 25
        .orderBy("name")                       // sorting
        .limit(10)                             // no more than 10 rows
        .execute();

// Result is a list. Each element is one row from the database:
// users.get(0).get("name") → "Alice"

// Get a single row
Map<String, Object> user = db.select("users")
        .where("id", 42)
        .executeOne();

// Get a single value (COUNT, MAX, etc.)
Object count = db.select("users")
        .columns("COUNT(*)")
        .where("status", "active")
        .executeScalar();
```

### INSERT — add a record

```java
db.insert("users")
        .set("name", "John")
        .set("email", "john@example.com")
        .set("age", 30)
        .execute();
```

### UPDATE — update a record

```java
db.update("users")
        .set("status", "inactive")
        .where("last_login", "<", "2025-01-01")
        .execute();
```

### DELETE — delete a record

```java
db.delete("users")
        .where("status", "deleted")
        .execute();
```

### Full example: API request + database verification

```java
@Test
public void testCreateUserAndCheckDb() throws Exception {
    // 1. Create a user via API
    RequestInfo request = new RequestInfo();
    request.setMethod("POST");
    request.setPath("/users");
    request.setBody(new JsonRequestBody("""
            {"name": "John", "email": "john@example.com"}
            """));

    ApiResponse response = client.sendRequest(request);
    response.assertStatus(201);

    // 2. Verify the user appeared in the database
    Map<String, Object> user = db.select("users")
            .where("email", "john@example.com")
            .executeOne();

    Assert.assertNotNull(user, "User should be in the database");
    Assert.assertEquals(user.get("name"), "John");

    // 3. Clean up after ourselves
    db.delete("users").where("email", "john@example.com").execute();
}
```

---

## 7. How to send requests to different hosts

You can work with multiple servers in a single test.

```java
@BeforeClass
public void setUp() {
    // has() prevents client recreation.
    // If 10 test classes use "main-api" — the client is created once,
    // the other 9 get the same instance. One HttpClient, one connection pool.
    if (!ApiClientProvider.has("main-api")) {
        ApiClientProvider.register("main-api", "https://api.example.com");
    }
    if (!ApiClientProvider.has("auth")) {
        ApiClientProvider.register("auth", "https://auth.example.com");
    }
    if (!ApiClientProvider.has("payment")) {
        ApiClientProvider.register("payment", "https://pay.example.com");
    }
}

@Test
public void testCrossServiceFlow() throws Exception {
    // Request to auth service
    RequestInfo login = new RequestInfo();
    login.setMethod("POST");
    login.setPath("/login");
    login.setBody(new JsonRequestBody("""
            {"username": "admin", "password": "secret"}
            """));
    ApiResponse loginResponse = ApiClientProvider.get("auth").sendRequest(login);

    // Request to main API
    RequestInfo order = new RequestInfo();
    order.setMethod("POST");
    order.setPath("/orders");
    order.setBody(new JsonRequestBody("""
            {"product": "laptop", "quantity": 1}
            """));
    ApiResponse orderResponse = ApiClientProvider.get("main-api").sendRequest(order);

    // Check on payment service
    RequestInfo payment = new RequestInfo();
    payment.setMethod("GET");
    payment.setPath("/payments/latest");
    ApiClientProvider.get("payment").sendRequest(payment)
            .assertStatus(200);
}
```

> **Important:** always use `has()` before `register()`. Without the check, every test class will recreate the client — not a bug, but unnecessary work. With the check — one client for the entire test run, regardless of how many test classes use it.

---

## 8. How to work with multiple databases

Same approach as with hosts — create multiple clients.

```java
private IDbClient mainDb;
private IDbClient analyticsDb;

@BeforeClass
public void setUp() {
    mainDb = new JdbcDbClient(
            "jdbc:postgresql://localhost:5432/main", "user", "pass");
    mainDb.connect();

    analyticsDb = new JdbcDbClient(
            "jdbc:postgresql://localhost:5432/analytics", "user", "pass");
    analyticsDb.connect();
}

@Test
public void testDataSync() throws Exception {
    // Data from the main database
    Map<String, Object> user = mainDb.select("users")
            .where("id", 1)
            .executeOne();

    // Same data in the analytics database
    Map<String, Object> event = analyticsDb.select("user_events")
            .where("user_id", 1)
            .executeOne();

    Assert.assertNotNull(event, "Data should reach analytics");
}

@AfterClass
public void tearDown() {
    mainDb.close();
    analyticsDb.close();
}
```

---

## 9. How to work with tokens (authentication)

If the API requires authentication:

```java
import org.ugina.ApiClient.Token.ApiTokenManager;

private ApiTokenManager tokenManager;

@BeforeClass
public void setUp() {
    ApiClientProvider.register("api", "https://api.example.com");
    tokenManager = new ApiTokenManager("/auth/login", "/auth/refresh");
    tokenManager.authenticate("api", "admin", "password123");
}

@Test
public void testProtectedEndpoint() {
    RequestInfo request = new RequestInfo();
    request.setMethod("GET");
    request.setPath("/users/me");

    // sendWithAuth — automatically:
    //   1. Adds the token to the header
    //   2. If the token expired — refreshes it
    //   3. If 401 — refreshes and retries
    tokenManager.sendWithAuth("api", request)
            .assertStatus(200)
            .assertBodyContains("\"email\"");
}
```

---

## 10. How to name tests for Allure reports

Allure annotations group tests into a structured report:

```java
import io.qameta.allure.*;

@Epic("User Management")                     // top level — module
public class UserApiTest {

    @Test
    @Feature("User CRUD")                    // feature within the module
    @Story("Create user")                    // specific scenario
    @Description("POST /users creates a new user and returns 201")  // description
    @Severity(SeverityLevel.CRITICAL)        // severity
    public void testCreateUser() throws Exception {
        // ...
    }
}
```

**How it looks in the report:**

```
📁 User Management                          ← @Epic
  📁 User CRUD                              ← @Feature
    ✅ Create user                           ← @Story
       POST /users creates a new user...     ← @Description
       Severity: CRITICAL                    ← @Severity
```

**Minimum recommended — at least `@Description`:**

```java
@Test
@Description("Verify that GET /users returns a list of users")
public void testGetUsers() throws Exception {
    // ...
}
```

---

## 11. How to run tests

```bash
./gradlew apiTest       # API tests (parallel)
./gradlew uiTest        # UI tests (Appium)
./gradlew sslTest       # SSL tests
./gradlew e2eTest       # DB + End-to-End tests
./gradlew test          # All tests
```

---

## 12. Cheat sheet

### Request

```java
RequestInfo r = new RequestInfo();
r.setMethod("GET");                              // method
r.setPath("/users");                             // path
r.addHeader("Accept", "application/json");       // header
r.addQueryParam("page", "1");                    // ?page=1
r.setBody(new JsonRequestBody("{...}"));         // body
```

### Assertions

```java
response.assertStatus(200);                      // status code
response.assertBodyContains("email");            // contains
response.assertBodyNotContains("error");         // does not contain
response.assertBodyNotEmpty();                   // not empty
response.assertDurationLessThan(5000);           // faster than 5s
response.assertHeader("content-type", "json");   // header
response.assertSuccess();                        // 2xx code
```

### Response data

```java
response.statusCode();    // 200
response.body();          // {"name": "John"}
response.durationMs();    // 145
response.header("...");   // header value
response.isSuccess();     // true/false
```

### Database

```java
db.select("t").where("c", v).execute();                    // SELECT
db.insert("t").set("c", v).execute();                      // INSERT
db.update("t").set("c", v).where("id", 1).execute();      // UPDATE
db.delete("t").where("id", 1).execute();                   // DELETE
```

### Allure

```java
@Epic("Module")           @Feature("Feature")
@Story("Scenario")        @Description("Description")
@Severity(SeverityLevel.CRITICAL)
```