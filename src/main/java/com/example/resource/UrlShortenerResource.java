package com.example.resource;

import com.example.dto.UrlRequest;
import com.example.dto.UrlResponse;
import com.example.dto.UrlStatsResponse;
import com.example.exception.UrlExceptionMapper;
import com.example.service.UrlShortenerService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.net.URI;

/**
 * REST resource class for URL shortening operations.
 * This class provides clean API endpoints using DTOs and proper HTTP semantics.
 * <p>
 * All endpoints use DTOs for request/response - entities are never exposed directly.
 */
@Path("/")
@Tag(name = "URL Shortener", description = "API for URL shortening and management")
public class UrlShortenerResource {

    private static final Logger LOG = Logger.getLogger(UrlShortenerResource.class);

    @Inject
    UrlShortenerService urlShortenerService;

    /**
     * Simple ping endpoint at the root context.
     * <p>
     * GET /
     *
     * @return a simple text response
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(
            summary = "Root Ping Check",
            description = "A simple endpoint at the application's root context for basic liveness check."
    )
    @APIResponse(
            responseCode = "200",
            description = "Service is alive"
    )
    public String ping() {
        LOG.debug("Ping endpoint reached.");
        // A simple, fast, and lightweight response
        return "URL Shortener Service is running.\nUse /{shortCode} for redirection or /api/urls to create a new one.";
    }

    /**
     * Creates a new shortened URL.
     * <p>
     * POST /api/urls
     * Content-Type: application/json
     *
     * @param request UrlRequest DTO containing the long URL to shorten
     * @return UrlResponse DTO with the shortened URL information
     */
    @POST
    @Path("/api/urls")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Create a shortened URL",
            description = "Creates a new shortened URL from the provided long URL"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "201",
                    description = "URL successfully shortened",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = UrlResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid URL provided",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = UrlExceptionMapper.ErrorResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "422",
                    description = "Validation error in request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = UrlExceptionMapper.ValidationErrorResponse.class)
                    )
            )
    })
    // Note: Since we are validating the request here using @Valid annotation, some of the items in the exception mapper
    // may not even be used.
    public Response createShortUrl(@Valid UrlRequest request) {
        LOG.infof("Creating short URL for: %s", request.getLongUrl());

        UrlResponse response = urlShortenerService.createShortUrl(request);

        // Return 201 Created with Location header pointing to the stats endpoint
        URI location = URI.create("/api/urls/" + response.getShortCode() + "/stats");

        return Response.created(location)
                .entity(response)
                .build();
    }

    /**
     * Redirects to the original URL using the short code.
     * <p>
     * GET /{shortCode}
     *
     * @param shortCode the short code to look up
     * @return HTTP 302 redirect to the original URL
     */
    @GET
    @Path("/{shortCode}")
    @Operation(
            summary = "Redirect to original URL",
            description = "Performs an HTTP 302 redirect to the original URL and increments the click counter"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "302",
                    description = "Redirect to original URL"
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Short code not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = UrlExceptionMapper.ErrorResponse.class)
                    )
            )
    })
    public Response redirectToLongUrl(
            @Parameter(
                    description = "The short code to redirect",
                    example = "abc123",
                    required = true
            )
            @PathParam("shortCode") String shortCode) {

        LOG.infof("Redirecting short code: %s", shortCode);

        // This method increments the click count and returns the long URL
        String longUrl = urlShortenerService.incrementClickAndGetLongUrl(shortCode);

        LOG.infof("Redirecting %s to %s", shortCode, longUrl);

        // Return HTTP 302 Found with Location header
        return Response.status(Response.Status.FOUND)
                .location(URI.create(longUrl))
                .build();
    }

    /**
     * Retrieves statistics for a shortened URL.
     * <p>
     * GET /api/urls/{shortCode}/stats
     *
     * @param shortCode the short code to get statistics for
     * @return UrlStatsResponse DTO with URL information and click statistics
     */
    @GET
    @Path("/api/urls/{shortCode}/stats")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get URL statistics",
            description = "Retrieves detailed statistics for a shortened URL including click count"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "URL statistics retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = UrlStatsResponse.class)
                    )
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Short code not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = UrlExceptionMapper.ErrorResponse.class)
                    )
            )
    })
    public UrlStatsResponse getUrlStats(
            @Parameter(
                    description = "The short code to get statistics for",
                    example = "abc123",
                    required = true
            )
            @PathParam("shortCode") String shortCode) {

        LOG.debugf("Getting statistics for short code: %s", shortCode);

        return urlShortenerService.getUrlStats(shortCode);
    }

    // =============================================================================
    // Optional System Statistics Endpoints (for monitoring/admin)
    // =============================================================================

    /**
     * Get system-wide statistics (optional endpoint for monitoring).
     * <p>
     * GET /api/stats
     *
     * @return system statistics
     */
    @GET
    @Path("/api/stats")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get system statistics",
            description = "Retrieves overall system statistics including total URLs and clicks"
    )
    @APIResponse(
            responseCode = "200",
            description = "System statistics retrieved successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = SystemStats.class)
            )
    )
    public SystemStats getSystemStats() {
        LOG.debug("Getting system statistics");

        Long totalUrls = urlShortenerService.getTotalUrlCount();
        Long totalClicks = urlShortenerService.getTotalClickCount();

        return new SystemStats(totalUrls, totalClicks);
    }

    // =============================================================================
    // Health Check Endpoint (Additional to SmallRye Health)
    // =============================================================================

    /**
     * Simple health check endpoint.
     * <p>
     * GET /api/health
     *
     * @return health status
     */
    @GET
    @Path("/api/health")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Application health check",
            description = "Simple health check endpoint for the URL shortener service"
    )
    @APIResponse(
            responseCode = "200",
            description = "Service is healthy"
    )
    public Response healthCheck() {
        // Perform a simple database check by counting URLs
        try {
            Long count = urlShortenerService.getTotalUrlCount();
            return Response.ok()
                    .entity(new HealthStatus("UP", "Database accessible, " + count + " URLs stored"))
                    .build();
        } catch (Exception e) {
            LOG.error("Health check failed", e);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new HealthStatus("DOWN", "Database not accessible: " + e.getMessage()))
                    .build();
        }
    }

    // =============================================================================
    // Response DTOs for Additional Endpoints
    // =============================================================================

    /**
     * DTO for system statistics.
     */
    @Schema(description = "System-wide statistics")
    public static class SystemStats {
        @Schema(description = "Total number of URLs in the system", examples = {"1000"})
        public Long totalUrls;

        @Schema(description = "Total number of clicks across all URLs", examples = {"5000"})
        public Long totalClicks;

        @Schema(description = "Average clicks per URL", examples = {"5.0"})
        public Double averageClicksPerUrl;

        public SystemStats() {
        }

        public SystemStats(Long totalUrls, Long totalClicks) {
            this.totalUrls = totalUrls;
            this.totalClicks = totalClicks;
            this.averageClicksPerUrl = totalUrls > 0 ? (double) totalClicks / totalUrls : 0.0;
        }
    }

    /**
     * DTO for health check responses.
     */
    @Schema(description = "Health check response")
    public static class HealthStatus {
        @Schema(description = "Health status", examples = {"UP"}, enumeration = {"UP", "DOWN"})
        public String status;

        @Schema(description = "Health check details", examples = {"Database accessible, 100 URLs stored"})
        public String details;

        public HealthStatus() {
        }

        public HealthStatus(String status, String details) {
            this.status = status;
            this.details = details;
        }
    }
}