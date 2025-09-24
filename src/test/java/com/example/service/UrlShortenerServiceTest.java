package com.example.service;

import com.example.dto.UrlRequest;
import com.example.dto.UrlResponse;
import com.example.dto.UrlStatsResponse;
import com.example.entity.ShortUrl;
import com.example.exception.InvalidUrlException;
import com.example.exception.UrlNotFoundException;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for UrlShortenerService.
 * Tests business logic, DTO conversions, and error handling.
 */
@QuarkusTest
class UrlShortenerServiceTest {

    @Inject
    UrlShortenerService urlShortenerService;

    private static final String VALID_URL = "https://www.github.com/quarkusio/quarkus";
    private static final String INVALID_URL = "not-a-valid-url";

    @BeforeEach
    @TestTransaction
    void setUp() {
        // Clean database before each test
        ShortUrl.deleteAll();
    }

    // =============================================================================
    // Create Short URL Tests
    // =============================================================================

    @Test
    @TestTransaction
    void testCreateShortUrl_ValidUrl_Success() {
        // Given
        UrlRequest request = new UrlRequest(VALID_URL);

        // When
        UrlResponse response = urlShortenerService.createShortUrl(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getLongUrl()).isEqualTo(VALID_URL);
        assertThat(response.getShortCode()).isNotNull();
        assertThat(response.getShortCode()).hasSize(6); // Default length
        assertThat(response.getShortUrl()).contains(response.getShortCode());
        assertThat(response.getCreatedAt()).isNotNull();

        // Verify entity is persisted
        ShortUrl entity = ShortUrl.findByShortCode(response.getShortCode());
        assertThat(entity).isNotNull();
        assertThat(entity.longUrl).isEqualTo(VALID_URL);
        assertThat(entity.clickCount).isEqualTo(0L);
    }

    @Test
    @TestTransaction
    void testCreateShortUrl_InvalidUrl_ThrowsException() {
        // Given
        UrlRequest request = new UrlRequest("https://");

        // When/Then
        assertThatThrownBy(() -> urlShortenerService.createShortUrl(request))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("Invalid URL syntax");
    }

    @Test
    @TestTransaction
    void testCreateShortUrl_DuplicateUrl_ReturnsSameShortCode() {
        // Given
        UrlRequest request = new UrlRequest(VALID_URL);

        // When
        UrlResponse response1 = urlShortenerService.createShortUrl(request);
        UrlResponse response2 = urlShortenerService.createShortUrl(request);

        // Then
        assertThat(response1.getShortCode()).isEqualTo(response2.getShortCode());
        assertThat(response1.getLongUrl()).isEqualTo(response2.getLongUrl());

        // Verify only one entity exists
        assertThat(ShortUrl.count()).isEqualTo(1L);
    }

    @Test
    @TestTransaction
    void testCreateShortUrl_HttpUrl_Success() {
        // Given
        String httpUrl = "http://www.example.com/test";
        UrlRequest request = new UrlRequest(httpUrl);

        // When
        UrlResponse response = urlShortenerService.createShortUrl(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getLongUrl()).isEqualTo(httpUrl);
        assertThat(response.getShortCode()).isNotNull();
    }

    // =============================================================================
    // Get Long URL Tests
    // =============================================================================

    @Test
    @TestTransaction
    void testGetLongUrl_ExistingShortCode_ReturnsUrl() {
        // Given
        UrlRequest request = new UrlRequest(VALID_URL);
        UrlResponse created = urlShortenerService.createShortUrl(request);

        // When
        String retrievedUrl = urlShortenerService.getLongUrl(created.getShortCode());

        // Then
        assertThat(retrievedUrl).isEqualTo(VALID_URL);
    }

    @Test
    void testGetLongUrl_NonExistentShortCode_ThrowsException() {
        // Given
        String nonExistentCode = "nonexistent";

        // When/Then
        assertThatThrownBy(() -> urlShortenerService.getLongUrl(nonExistentCode))
                .isInstanceOf(UrlNotFoundException.class)
                .hasMessageContaining("not found");
    }

    // =============================================================================
    // Increment Click Tests
    // =============================================================================

    @Test
    @TestTransaction
    void testIncrementClickAndGetLongUrl_ExistingUrl_IncrementsCount() {
        // Given
        UrlRequest request = new UrlRequest(VALID_URL);
        UrlResponse created = urlShortenerService.createShortUrl(request);

        // Verify initial count
        ShortUrl entity = ShortUrl.findByShortCode(created.getShortCode());
        assertThat(entity.clickCount).isEqualTo(0L);

        // When
        String retrievedUrl = urlShortenerService.incrementClickAndGetLongUrl(created.getShortCode());

        // Then
        assertThat(retrievedUrl).isEqualTo(VALID_URL);

        // Verify count is incremented
        entity = ShortUrl.findByShortCode(created.getShortCode());
        assertThat(entity.clickCount).isEqualTo(1L);
    }

    @Test
    @TestTransaction
    void testIncrementClickAndGetLongUrl_MultipleClicks_CountsCorrectly() {
        // Given
        UrlRequest request = new UrlRequest(VALID_URL);
        UrlResponse created = urlShortenerService.createShortUrl(request);

        // When - Click 3 times
        urlShortenerService.incrementClickAndGetLongUrl(created.getShortCode());
        urlShortenerService.incrementClickAndGetLongUrl(created.getShortCode());
        urlShortenerService.incrementClickAndGetLongUrl(created.getShortCode());

        // Then
        ShortUrl entity = ShortUrl.findByShortCode(created.getShortCode());
        assertThat(entity.clickCount).isEqualTo(3L);
    }

    @Test
    void testIncrementClickAndGetLongUrl_NonExistentShortCode_ThrowsException() {
        // Given
        String nonExistentCode = "nonexistent";

        // When/Then
        assertThatThrownBy(() -> urlShortenerService.incrementClickAndGetLongUrl(nonExistentCode))
                .isInstanceOf(UrlNotFoundException.class)
                .hasMessageContaining("not found");
    }

    // =============================================================================
    // Get URL Stats Tests
    // =============================================================================

    @Test
    @TestTransaction
    void testGetUrlStats_ExistingUrl_ReturnsStats() {
        // Given
        UrlRequest request = new UrlRequest(VALID_URL);
        UrlResponse created = urlShortenerService.createShortUrl(request);

        // Click the URL a few times
        urlShortenerService.incrementClickAndGetLongUrl(created.getShortCode());
        urlShortenerService.incrementClickAndGetLongUrl(created.getShortCode());

        // When
        UrlStatsResponse stats = urlShortenerService.getUrlStats(created.getShortCode());

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.getShortCode()).isEqualTo(created.getShortCode());
        assertThat(stats.getLongUrl()).isEqualTo(VALID_URL);
        assertThat(stats.getShortUrl()).isEqualTo(created.getShortUrl());
        assertThat(stats.getCreatedAt()).isEqualTo(created.getCreatedAt());
        assertThat(stats.getClickCount()).isEqualTo(2L);
    }

    @Test
    void testGetUrlStats_NonExistentShortCode_ThrowsException() {
        // Given
        String nonExistentCode = "nonexistent";

        // When/Then
        assertThatThrownBy(() -> urlShortenerService.getUrlStats(nonExistentCode))
                .isInstanceOf(UrlNotFoundException.class)
                .hasMessageContaining("not found");
    }

    // =============================================================================
    // System Statistics Tests
    // =============================================================================

    @Test
    @TestTransaction
    void testGetTotalUrlCount_EmptyDatabase_ReturnsZero() {
        // When
        Long count = urlShortenerService.getTotalUrlCount();

        // Then
        assertThat(count).isEqualTo(0L);
    }

    @Test
    @TestTransaction
    void testGetTotalUrlCount_WithUrls_ReturnsCount() {
        // Given
        urlShortenerService.createShortUrl(new UrlRequest("https://example1.com"));
        urlShortenerService.createShortUrl(new UrlRequest("https://example2.com"));
        urlShortenerService.createShortUrl(new UrlRequest("https://example3.com"));

        // When
        Long count = urlShortenerService.getTotalUrlCount();

        // Then
        assertThat(count).isEqualTo(3L);
    }

    @Test
    @TestTransaction
    void testGetTotalClickCount_EmptyDatabase_ReturnsZero() {
        // When
        Long count = urlShortenerService.getTotalClickCount();

        // Then
        assertThat(count).isEqualTo(0L);
    }

    @Test
    @TestTransaction
    void testGetTotalClickCount_WithClicks_ReturnsTotal() {
        // Given
        UrlResponse url1 = urlShortenerService.createShortUrl(new UrlRequest("https://example1.com"));
        UrlResponse url2 = urlShortenerService.createShortUrl(new UrlRequest("https://example2.com"));

        // Click first URL 3 times
        urlShortenerService.incrementClickAndGetLongUrl(url1.getShortCode());
        urlShortenerService.incrementClickAndGetLongUrl(url1.getShortCode());
        urlShortenerService.incrementClickAndGetLongUrl(url1.getShortCode());

        // Click second URL 2 times
        urlShortenerService.incrementClickAndGetLongUrl(url2.getShortCode());
        urlShortenerService.incrementClickAndGetLongUrl(url2.getShortCode());

        // When
        Long totalClicks = urlShortenerService.getTotalClickCount();

        // Then
        assertThat(totalClicks).isEqualTo(5L);
    }

    // =============================================================================
    // URL Validation Tests
    // =============================================================================

    @Test
    @TestTransaction
    void testCreateShortUrl_UrlWithoutScheme_ThrowsException() {
        // Given
        UrlRequest request = new UrlRequest("www.example.com");

        // When/Then
        assertThatThrownBy(() -> urlShortenerService.createShortUrl(request))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("scheme");
    }

    @Test
    @TestTransaction
    void testCreateShortUrl_UrlWithInvalidScheme_ThrowsException() {
        // Given
        UrlRequest request = new UrlRequest("ftp://example.com");

        // When/Then
        assertThatThrownBy(() -> urlShortenerService.createShortUrl(request))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("http");
    }

    @Test
    @TestTransaction
    void testCreateShortUrl_UrlWithoutHost_ThrowsException() {
        // Given
        UrlRequest request = new UrlRequest("https:abc");

        // When/Then
        assertThatThrownBy(() -> urlShortenerService.createShortUrl(request))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("host");
    }

    // =============================================================================
    // Short Code Generation Tests
    // =============================================================================

    @Test
    @TestTransaction
    void testCreateShortUrl_MultipleUrls_GeneratesUniqueShortCodes() {
        // Given
        UrlRequest request1 = new UrlRequest("https://example1.com");
        UrlRequest request2 = new UrlRequest("https://example2.com");
        UrlRequest request3 = new UrlRequest("https://example3.com");

        // When
        UrlResponse response1 = urlShortenerService.createShortUrl(request1);
        UrlResponse response2 = urlShortenerService.createShortUrl(request2);
        UrlResponse response3 = urlShortenerService.createShortUrl(request3);

        // Then
        assertThat(response1.getShortCode()).isNotEqualTo(response2.getShortCode());
        assertThat(response1.getShortCode()).isNotEqualTo(response3.getShortCode());
        assertThat(response2.getShortCode()).isNotEqualTo(response3.getShortCode());

        // All should be the expected length
        assertThat(response1.getShortCode()).hasSize(6);
        assertThat(response2.getShortCode()).hasSize(6);
        assertThat(response3.getShortCode()).hasSize(6);
    }

    @Test
    @TestTransaction
    void testCreateShortUrl_ShortCodeContainsOnlyValidCharacters() {
        // Given
        UrlRequest request = new UrlRequest(VALID_URL);

        // When
        UrlResponse response = urlShortenerService.createShortUrl(request);

        // Then
        String shortCode = response.getShortCode();
        assertThat(shortCode).matches("[A-Za-z0-9]+");
    }

    // =============================================================================
    // DTO Conversion Tests
    // =============================================================================

    @Test
    @TestTransaction
    void testCreateShortUrl_DtoContainsCorrectBaseUrl() {
        // Given
        UrlRequest request = new UrlRequest(VALID_URL);

        // When
        UrlResponse response = urlShortenerService.createShortUrl(request);

        // Then
        assertThat(response.getShortUrl()).startsWith("http://localhost:8081/"); // Test profile base URL
        assertThat(response.getShortUrl()).endsWith(response.getShortCode());
    }

    @Test
    @TestTransaction
    void testGetUrlStats_UrlStatsResponseContainsAllRequiredFields() {
        // Given
        UrlRequest request = new UrlRequest(VALID_URL);
        UrlResponse created = urlShortenerService.createShortUrl(request);

        // Add some clicks
        urlShortenerService.incrementClickAndGetLongUrl(created.getShortCode());

        // When
        UrlStatsResponse stats = urlShortenerService.getUrlStats(created.getShortCode());

        // Then
        assertThat(stats.getShortCode()).isEqualTo(created.getShortCode());
        assertThat(stats.getLongUrl()).isEqualTo(created.getLongUrl());
        assertThat(stats.getShortUrl()).isEqualTo(created.getShortUrl());
        assertThat(stats.getCreatedAt()).isEqualTo(created.getCreatedAt());
        assertThat(stats.getClickCount()).isNotNull();
        assertThat(stats.getClickCount()).isEqualTo(1L);
    }
}