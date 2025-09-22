package com.example.dto;

import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.json.bind.annotation.JsonbProperty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Response DTO for URL statistics operations.
 * This class extends UrlResponse to include click tracking information.
 */
@Schema(description = "Response payload containing shortened URL information with statistics")
public class UrlStatsResponse {

    @JsonbProperty("shortCode")
    @Schema(description = "The unique short code for the URL", examples = {"abc123"}, required = true)
    private String shortCode;

    @JsonbProperty("longUrl")
    @Schema(description = "The original long URL", examples = {"https://www.github.com/quarkusio/quarkus"}, required = true)
    private String longUrl;

    @JsonbProperty("shortUrl")
    @Schema(description = "The complete shortened URL", examples = {"http://localhost:8080/abc123"}, required = true)
    private String shortUrl;

    @JsonbProperty("createdAt")
    @JsonbDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Timestamp when the URL was created", examples = {"2025-09-20T10:30:00"}, required = true)
    private LocalDateTime createdAt;

    @JsonbProperty("clickCount")
    @Schema(description = "Number of times the short URL has been accessed", examples = {"42"}, required = true)
    private Long clickCount;

    /**
     * Default constructor for JSON serialization.
     */
    public UrlStatsResponse() {
    }

    public UrlStatsResponse(String shortCode, String longUrl, String shortUrl,
                            LocalDateTime createdAt, Long clickCount) {
        this.shortCode = shortCode;
        this.longUrl = longUrl;
        this.shortUrl = shortUrl;
        this.createdAt = createdAt;
        this.clickCount = clickCount;
    }

    public UrlStatsResponse(UrlResponse urlResponse, Long clickCount) {
        this.shortCode = urlResponse.getShortCode();
        this.longUrl = urlResponse.getLongUrl();
        this.shortUrl = urlResponse.getShortUrl();
        this.createdAt = urlResponse.getCreatedAt();
        this.clickCount = clickCount;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getClickCount() {
        return clickCount;
    }

    public void setClickCount(Long clickCount) {
        this.clickCount = clickCount;
    }

    @Override
    public String toString() {
        return "UrlStatsResponse{" +
                "shortCode='" + shortCode + '\'' +
                ", longUrl='" + longUrl + '\'' +
                ", shortUrl='" + shortUrl + '\'' +
                ", createdAt=" + createdAt +
                ", clickCount=" + clickCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UrlStatsResponse that = (UrlStatsResponse) o;

        if (!Objects.equals(shortCode, that.shortCode)) return false;
        if (!Objects.equals(longUrl, that.longUrl)) return false;
        if (!Objects.equals(shortUrl, that.shortUrl)) return false;
        if (!Objects.equals(createdAt, that.createdAt)) return false;
        return Objects.equals(clickCount, that.clickCount);
    }

    @Override
    public int hashCode() {
        int result = shortCode != null ? shortCode.hashCode() : 0;
        result = 31 * result + (longUrl != null ? longUrl.hashCode() : 0);
        result = 31 * result + (shortUrl != null ? shortUrl.hashCode() : 0);
        result = 31 * result + (createdAt != null ? createdAt.hashCode() : 0);
        result = 31 * result + (clickCount != null ? clickCount.hashCode() : 0);
        return result;
    }
}