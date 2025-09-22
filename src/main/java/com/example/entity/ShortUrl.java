package com.example.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing a shortened URL mapping.
 * This class uses Panache for simplified database operations.
 * <p>
 * Note: This entity is NOT exposed directly in REST APIs.
 * All API interactions use DTOs for clean separation of concerns.
 */
@Entity
@Table(name = "short_urls")
@NamedQueries({
        @NamedQuery(
                name = "ShortUrl.findByShortCode",
                query = "SELECT s FROM ShortUrl s WHERE s.shortCode = :shortCode"
        ),
        @NamedQuery(
                name = "ShortUrl.existsByShortCode",
                query = "SELECT COUNT(s) > 0 FROM ShortUrl s WHERE s.shortCode = :shortCode"
        )
})
public class ShortUrl extends PanacheEntity {

    /**
     * The original long URL to be shortened.
     * This field is indexed for potential future lookups and duplicate detection.
     */
    @Column(name = "long_url", nullable = false, length = 2048)
    @NotBlank(message = "Long URL cannot be blank")
    @Size(max = 2048, message = "Long URL cannot exceed 2048 characters")
    public String longUrl;

    /**
     * The unique short code that identifies this URL.
     * This is the key part of the shortened URL (e.g., 'abc123' in 'http://domain.com/abc123').
     */
    @Column(name = "short_code", nullable = false, unique = true, length = 10)
    @NotBlank(message = "Short code cannot be blank")
    @Size(max = 10, message = "Short code cannot exceed 10 characters")
    public String shortCode;

    /**
     * Timestamp when the URL was created.
     * Automatically set when the entity is persisted.
     */
    @Column(name = "created_at", nullable = false)
    @NotNull(message = "Created timestamp cannot be null")
    public LocalDateTime createdAt;

    /**
     * Number of times this short URL has been accessed.
     * Used for analytics and statistics.
     */
    @Column(name = "click_count", nullable = false)
    @NotNull(message = "Click count cannot be null")
    @PositiveOrZero(message = "Click count must be zero or positive")
    public Long clickCount;

    /**
     * Default constructor required by JPA.
     */
    public ShortUrl() {
        this.clickCount = 0L;
        this.createdAt = LocalDateTime.now();
    }

    public ShortUrl(String longUrl, String shortCode) {
        this.longUrl = longUrl;
        this.shortCode = shortCode;
        this.clickCount = 0L;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * JPA callback method to set creation timestamp before persisting.
     */
    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (clickCount == null) {
            clickCount = 0L;
        }
    }

    public Long incrementClickCount() {
        this.clickCount++;
        return this.clickCount;
    }

    // =============================================================================
    // Panache Finder Methods
    // =============================================================================

    public static ShortUrl findByShortCode(String shortCode) {
        return find("shortCode", shortCode).firstResult();
    }

    public static boolean existsByShortCode(String shortCode) {
        return count("shortCode", shortCode) > 0;
    }

    public static ShortUrl findByLongUrl(String longUrl) {
        return find("longUrl", longUrl).firstResult();
    }

    public static Long getTotalClicks() {
        return getEntityManager()
                .createQuery("SELECT COALESCE(SUM(s.clickCount), 0L) FROM ShortUrl s", Long.class)
                .getSingleResult();
    }

    public static Long getTotalUrls() {
        return count();
    }

    // =============================================================================
    // Object Methods
    // =============================================================================

    @Override
    public String toString() {
        return "ShortUrl{" +
                "id=" + id +
                ", longUrl='" + longUrl + '\'' +
                ", shortCode='" + shortCode + '\'' +
                ", createdAt=" + createdAt +
                ", clickCount=" + clickCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ShortUrl shortUrl = (ShortUrl) o;

        // Use shortCode for equality since it's unique
        return Objects.equals(shortCode, shortUrl.shortCode);
    }

    @Override
    public int hashCode() {
        // Use shortCode for hash since it's unique
        return shortCode != null ? shortCode.hashCode() : 0;
    }
}