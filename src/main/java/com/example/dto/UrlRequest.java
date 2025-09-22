package com.example.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Objects;

/**
 * Request DTO for creating short URLs.
 * This class defines the contract for incoming URL shortening requests.
 */
@Schema(description = "Request payload for creating a short URL")
public class UrlRequest {

    @JsonbProperty("longUrl")
    @NotBlank(message = "Long URL cannot be blank")
    @Size(max = 2048, message = "URL length cannot exceed 2048 characters")
    @Pattern(
            regexp = "^https?://(?:[-\\w.])+(?:[:\\d]+)?(?:/(?:[\\w/_.])*(?:\\?(?:[\\w&=%.]*))?(?:#(?:[\\w.]*))?)?$",
            message = "Invalid URL format. URL must start with http:// or https://"
    )
    @Schema(
            description = "The original long URL to be shortened",
            examples = {"https://www.github.com/quarkusio/quarkus"},
            required = true,
            maxLength = 2048
    )
    private String longUrl;

    /**
     * Default constructor for JSON deserialization.
     */
    public UrlRequest() {
    }

    public UrlRequest(String longUrl) {
        this.longUrl = longUrl;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    @Override
    public String toString() {
        return "UrlRequest{" +
                "longUrl='" + longUrl + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UrlRequest that = (UrlRequest) o;
        return Objects.equals(longUrl, that.longUrl);
    }

    @Override
    public int hashCode() {
        return longUrl != null ? longUrl.hashCode() : 0;
    }
}