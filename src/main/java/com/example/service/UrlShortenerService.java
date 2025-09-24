package com.example.service;

import com.example.dto.UrlRequest;
import com.example.dto.UrlResponse;
import com.example.dto.UrlStatsResponse;
import com.example.entity.ShortUrl;
import com.example.exception.InvalidUrlException;
import com.example.exception.UrlNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.SecureRandom;

/**
 * Service class containing business logic for URL shortening operations.
 * This service handles DTO-Entity conversions and core business rules.
 */
@ApplicationScoped
public class UrlShortenerService {

    private static final Logger LOG = Logger.getLogger(UrlShortenerService.class);

    @ConfigProperty(name = "app.shortcode.length", defaultValue = "6")
    int shortCodeLength;

    @ConfigProperty(name = "app.shortcode.alphabet", defaultValue = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789")
    String shortCodeAlphabet;

    @ConfigProperty(name = "app.base-url", defaultValue = "http://localhost:8080")
    String baseUrl;

    @ConfigProperty(name = "app.url.validation.enabled", defaultValue = "true")
    boolean urlValidationEnabled;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public UrlResponse createShortUrl(UrlRequest request) {
        LOG.debugf("Creating short URL for: %s", request.getLongUrl());

        // Validate URL format and accessibility
        validateUrl(request.getLongUrl());

        // Check if URL already exists (optional - prevents duplicates)
        ShortUrl existingUrl = ShortUrl.findByLongUrl(request.getLongUrl());
        if (existingUrl != null) {
            LOG.debugf("URL already exists, returning existing short code: %s", existingUrl.shortCode);
            return convertToUrlResponse(existingUrl);
        }

        // Generate unique short code
        String shortCode = generateUniqueShortCode();

        // Create and persist new entity
        ShortUrl shortUrl = new ShortUrl(request.getLongUrl(), shortCode);
        shortUrl.persist();

        LOG.infof("Created new short URL: %s -> %s", shortCode, request.getLongUrl());
        return convertToUrlResponse(shortUrl);
    }

    /*
     * Retrieves the original URL for a given short code.
     */
    public String getLongUrl(String shortCode) {
        LOG.debugf("Looking up long URL for short code: %s", shortCode);

        ShortUrl shortUrl = ShortUrl.findByShortCode(shortCode);
        if (shortUrl == null) {
            throw new UrlNotFoundException(shortCode);
        }

        return shortUrl.longUrl;
    }

    /*
     * Increments the click count for a short URL and returns the long URL.
     * This method is called when someone accesses a short URL.
     */
    @Transactional
    public String incrementClickAndGetLongUrl(String shortCode) {
        LOG.debugf("Incrementing click count for short code: %s", shortCode);

        ShortUrl shortUrl = ShortUrl.findByShortCode(shortCode);
        if (shortUrl == null) {
            throw new UrlNotFoundException(shortCode);
        }

        // Increment click count
        shortUrl.incrementClickCount();
        shortUrl.persist();

        LOG.debugf("Incremented click count to %d for short code: %s", shortUrl.clickCount, shortCode);
        return shortUrl.longUrl;
    }

    public UrlStatsResponse getUrlStats(String shortCode) {
        LOG.debugf("Getting statistics for short code: %s", shortCode);

        ShortUrl shortUrl = ShortUrl.findByShortCode(shortCode);
        if (shortUrl == null) {
            throw new UrlNotFoundException(shortCode);
        }

        return convertToUrlStatsResponse(shortUrl);
    }

    // =============================================================================
    // Private Helper Methods
    // =============================================================================

    /*
     * Validates that a URL is properly formatted and potentially accessible.
     */
    private void validateUrl(String urlString) {
        if (!urlValidationEnabled) {
            return;
        }

        try {
            // Parse as URI first for RFC compliance
            URI uri = new URI(urlString);

            // Ensure it has a scheme (http/https)
            if (uri.getScheme() == null) {
                throw new InvalidUrlException(urlString, "URL must include a scheme (http:// or https://)");
            }

            // Ensure scheme is http or https
            String scheme = uri.getScheme().toLowerCase();
            if (!scheme.equals("http") && !scheme.equals("https")) {
                throw new InvalidUrlException(urlString, "URL scheme must be http or https");
            }

            // Ensure it has a host
            if (uri.getHost() == null || uri.getHost().trim().isEmpty()) {
                throw new InvalidUrlException(urlString, "URL must include a valid host");
            }

            // Convert to URL for additional validation
            URL url = uri.toURL();

            // Additional checks could be added here:
            // - Blacklist certain domains
            // - Check if URL is reachable (with timeout)
            // - Validate against known malicious URLs

        } catch (URISyntaxException e) {
            throw new InvalidUrlException(urlString, "Invalid URL syntax: " + e.getMessage());
        } catch (MalformedURLException e) {
            throw new InvalidUrlException(urlString, "Malformed URL: " + e.getMessage());
        }
    }

    /*
     * Generates a cryptographically secure unique short code.
     */
    private String generateUniqueShortCode() {
        String shortCode;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            shortCode = generateRandomShortCode();
            attempts++;

            if (attempts > maxAttempts) {
                LOG.warnf("Failed to generate unique short code after %d attempts", maxAttempts);
                throw new RuntimeException("Unable to generate unique short code. Please try again.");
            }

        } while (ShortUrl.existsByShortCode(shortCode));

        LOG.debugf("Generated unique short code '%s' in %d attempts", shortCode, attempts);
        return shortCode;
    }

    /*
     * Generates a random short code using the configured alphabet.
     */
    private String generateRandomShortCode() {
        StringBuilder shortCode = new StringBuilder(shortCodeLength);

        for (int i = 0; i < shortCodeLength; i++) {
            int randomIndex = secureRandom.nextInt(shortCodeAlphabet.length());
            shortCode.append(shortCodeAlphabet.charAt(randomIndex));
        }

        return shortCode.toString();
    }

    private UrlResponse convertToUrlResponse(ShortUrl shortUrl) {
        String fullShortUrl = baseUrl + "/" + shortUrl.shortCode;
        return new UrlResponse(
                shortUrl.shortCode,
                shortUrl.longUrl,
                fullShortUrl,
                shortUrl.createdAt
        );
    }

    private UrlStatsResponse convertToUrlStatsResponse(ShortUrl shortUrl) {
        String fullShortUrl = baseUrl + "/" + shortUrl.shortCode;
        return new UrlStatsResponse(
                shortUrl.shortCode,
                shortUrl.longUrl,
                fullShortUrl,
                shortUrl.createdAt,
                shortUrl.clickCount
        );
    }

    // =============================================================================
    // System Statistics Methods (Optional - for admin/monitoring)
    // =============================================================================

    public Long getTotalUrlCount() {
        return ShortUrl.getTotalUrls();
    }

    public Long getTotalClickCount() {
        return ShortUrl.getTotalClicks();
    }
}