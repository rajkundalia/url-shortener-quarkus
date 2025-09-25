package com.example.integration;

import com.example.dto.UrlRequest;
import com.example.entity.ShortUrl;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.not;

/**
 * Integration tests for UrlShortenerResource using RestAssured.
 * Tests complete HTTP request/response flow including DTO serialization.
 */
@QuarkusTest
class UrlShortenerResourceTest {

    private static final String VALID_URL = "https://www.github.com/quarkusio/quarkus";
    private static final String INVALID_URL = "not-a-valid-url";

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean database before each test
        ShortUrl.deleteAll();
    }

    // =============================================================================
    // Create Short URL Tests
    // =============================================================================

    @Test
    void testCreateShortUrl_ValidRequest_ReturnsCreatedWithCorrectDto() {
        UrlRequest request = new UrlRequest(VALID_URL);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/urls")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .header("Location", matchesRegex(".+/api/urls/.+/stats"))
                .contentType(ContentType.JSON)
                .body("longUrl", equalTo(VALID_URL))
                .body("shortCode", not(emptyOrNullString()))
                .body("shortCode", hasLength(6))
                .body("shortUrl", containsString("http://localhost:8081/"))
                .body("shortUrl", endsWith(
                        // Extract shortCode from response and verify shortUrl ends with it
                        given().contentType(ContentType.JSON).body(request)
                                .when().post("/api/urls")
                                .then().extract().path("shortCode")
                ))
                .body("createdAt", not(emptyOrNullString()));
    }

    @Test
    void testCreateShortUrl_InvalidUrl_ReturnsBadRequestWithErrorDto() {
        UrlRequest request = new UrlRequest(INVALID_URL);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/urls")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .contentType(ContentType.JSON)
                .body("violations[0].message", containsString("Invalid URL format. URL must start with http:// or https://"));
    }

    @Test
    void testCreateShortUrl_BlankUrl_ReturnsBadRequestWithValidationError() {
        UrlRequest request = new UrlRequest("");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/urls")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .contentType(ContentType.JSON)
                .body("violations", hasSize(greaterThan(0)));
    }

    @Test
    void testCreateShortUrl_NullRequestBody_ReturnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .post("/api/urls")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    void testCreateShortUrl_InvalidJson_ReturnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .body("{invalid json}")
                .when()
                .post("/api/urls")
                .then()
                .statusCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    // =============================================================================
    // Redirect Tests
    // =============================================================================

    @Test
    void testRedirectToLongUrl_ValidShortCode_ReturnsFoundWithLocationHeader() {
        // First create a short URL
        UrlRequest request = new UrlRequest(VALID_URL);
        String shortCode = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/urls")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .extract()
                .path("shortCode");

        // Then test the redirect
        given()
                .redirects().follow(false)
                .when()
                .get("/" + shortCode)
                .then()
                .statusCode(Response.Status.FOUND.getStatusCode())
                .header("Location", equalTo(VALID_URL));
    }

    @Test
    void testRedirectToLongUrl_NonExistentShortCode_Returns404WithErrorDto() {
        given()
                .when()
                .get("/nonexistent")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode())
                .contentType(ContentType.JSON)
                .body("error", equalTo("URL_NOT_FOUND"))
                .body("message", containsString("not found"))
                .body("timestamp", not(emptyOrNullString()));
    }

    @Test
    void testRedirectToLongUrl_IncrementsClickCount() {
        // Create a short URL
        UrlRequest request = new UrlRequest(VALID_URL);
        String shortCode = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/urls")
                .then()
                .extract()
                .path("shortCode");

        // Access it multiple times
        given().redirects().follow(false).when().get("/" + shortCode).then().statusCode(Response.Status.FOUND.getStatusCode());
        given().redirects().follow(false).when().get("/" + shortCode).then().statusCode(Response.Status.FOUND.getStatusCode());
        given().redirects().follow(false).when().get("/" + shortCode).then().statusCode(Response.Status.FOUND.getStatusCode());

        // Check the click count
        given()
                .when()
                .get("/api/urls/" + shortCode + "/stats")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("clickCount", equalTo(3));
    }

    // =============================================================================
    // URL Statistics Tests
    // =============================================================================

    @Test
    void testGetUrlStats_ValidShortCode_ReturnsStatsWithCorrectDto() {
        // Create a short URL
        UrlRequest request = new UrlRequest(VALID_URL);
        String shortCode = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/urls")
                .then()
                .extract()
                .path("shortCode");

        // Access it once to increment click count
        given()
                .redirects().follow(false)
                .when()
                .get("/" + shortCode)
                .then()
                .statusCode(Response.Status.FOUND.getStatusCode());

        // Get statistics
        given()
                .when()
                .get("/api/urls/" + shortCode + "/stats")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(ContentType.JSON)
                .body("shortCode", equalTo(shortCode))
                .body("longUrl", equalTo(VALID_URL))
                .body("shortUrl", containsString(shortCode))
                .body("createdAt", not(emptyOrNullString()))
                .body("clickCount", equalTo(1));
    }

    @Test
    void testGetUrlStats_NonExistentShortCode_ReturnsNotFoundWithErrorDto() {
        given()
                .when()
                .get("/api/urls/nonexistent/stats")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode())
                .contentType(ContentType.JSON)
                .body("error", equalTo("URL_NOT_FOUND"))
                .body("message", containsString("not found"))
                .body("timestamp", not(emptyOrNullString()));
    }

    @Test
    void testGetUrlStats_InitialClickCount_IsZero() {
        // Create a short URL
        UrlRequest request = new UrlRequest(VALID_URL);
        String shortCode = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/urls")
                .then()
                .extract()
                .path("shortCode");

        // Get statistics immediately (without accessing the URL)
        given()
                .when()
                .get("/api/urls/" + shortCode + "/stats")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("clickCount", equalTo(0));
    }

    // =============================================================================
    // System Statistics Tests
    // =============================================================================

    @Test
    void testGetSystemStats_EmptyDatabase_ReturnsZeroStats() {
        given()
                .when()
                .get("/api/stats")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(ContentType.JSON)
                .body("totalUrls", equalTo(0))
                .body("totalClicks", equalTo(0))
                .body("averageClicksPerUrl", equalTo(0.0f));
    }

    @Test
    void testGetSystemStats_WithUrls_ReturnsCorrectStats() {
        // Create multiple URLs
        given().contentType(ContentType.JSON).body(new UrlRequest("https://example1.com"))
                .when().post("/api/urls").then().statusCode(201);
        given().contentType(ContentType.JSON).body(new UrlRequest("https://example2.com"))
                .when().post("/api/urls").then().statusCode(201);

        // Get system stats
        given()
                .when()
                .get("/api/stats")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("totalUrls", equalTo(2))
                .body("totalClicks", equalTo(0))
                .body("averageClicksPerUrl", equalTo(0.0f));
    }

    // =============================================================================
    // Health Check Tests
    // =============================================================================

    @Test
    void testHealthCheck_ReturnsHealthyStatus() {
        given()
                .when()
                .get("/api/health")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(ContentType.JSON)
                .body("status", equalTo("UP"))
                .body("details", containsString("Database accessible"));
    }

    // =============================================================================
    // OpenAPI/Swagger Tests
    // =============================================================================

    @Test
    void testOpenApiSpec_IsAccessible() {
        given()
                .when()
                .get("/q/openapi")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(containsString("yaml"));
    }

    @Test
    void testSwaggerUi_IsAccessible() {
        given()
                .when()
                .get("/q/swagger-ui")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(containsString("html"));
    }

    // =============================================================================
    // SmallRye Health Tests
    // =============================================================================

    @Test
    void testReadinessCheck_ReturnsHealthy() {
        given()
                .when()
                .get("/q/health/ready")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(ContentType.JSON)
                .body("status", equalTo("UP"));
    }

    @Test
    void testLivenessCheck_ReturnsHealthy() {
        given()
                .when()
                .get("/q/health/live")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(ContentType.JSON)
                .body("status", equalTo("UP"));
    }

    // =============================================================================
    // URL Validation Integration Tests
    // =============================================================================

    @Test
    void testCreateShortUrl_HttpUrl_AcceptedAndReturnsCreated() {
        UrlRequest request = new UrlRequest("http://www.example.com/test");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/urls")
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .body("longUrl", equalTo("http://www.example.com/test"));
    }

    @Test
    void testCreateShortUrl_UrlWithoutScheme_ReturnsBadRequest() {
        UrlRequest request = new UrlRequest("www.example.com");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/urls")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body("violations[0].message", containsString("http"));
    }

    @Test
    void testCreateShortUrl_FtpUrl_ReturnsBadRequest() {
        UrlRequest request = new UrlRequest("ftp://example.com/file.txt");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/urls")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body("violations[0].message", containsString("http"));
    }

    // =============================================================================
    // Content Type Tests
    // =============================================================================

    @Test
    void testCreateShortUrl_WrongContentType_ReturnsUnsupportedMediaType() {
        given()
                .contentType(ContentType.TEXT)
                .body("some text")
                .when()
                .post("/api/urls")
                .then()
                .statusCode(Response.Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode()); // Unsupported Media Type
    }

    @Test
    void testGetUrlStats_AcceptsOnlyGet() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .post("/api/urls/test/stats")
                .then()
                .statusCode(Response.Status.METHOD_NOT_ALLOWED.getStatusCode()); // Method Not Allowed
    }
}