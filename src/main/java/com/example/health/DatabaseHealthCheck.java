package com.example.health;

import com.example.entity.ShortUrl;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.jboss.logging.Logger;

/**
 * Custom health check to verify database connectivity and basic functionality.
 * This health check is automatically included in the SmallRye Health endpoints.
 */
@ApplicationScoped
@Readiness
public class DatabaseHealthCheck implements HealthCheck {

    private static final Logger LOG = Logger.getLogger(DatabaseHealthCheck.class);

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder responseBuilder = HealthCheckResponse.named("database-connectivity");

        try {
            // Perform a simple database operation to verify connectivity
            long urlCount = ShortUrl.count();

            // Check if we can perform a basic query
            boolean canQuery = (urlCount >= 0);

            if (canQuery) {
                LOG.debug("Database health check passed");
                return responseBuilder
                        .up()
                        .withData("total_urls", urlCount)
                        .withData("database_type", "H2")
                        .withData("last_check", System.currentTimeMillis())
                        .build();
            } else {
                LOG.warn("Database health check failed: Unable to query database");
                return responseBuilder
                        .down()
                        .withData("error", "Unable to query database")
                        .withData("last_check", System.currentTimeMillis())
                        .build();
            }

        } catch (Exception e) {
            LOG.errorf(e, "Database health check failed with exception: %s", e.getMessage());
            return responseBuilder
                    .down()
                    .withData("error", e.getMessage())
                    .withData("exception_type", e.getClass().getSimpleName())
                    .withData("last_check", System.currentTimeMillis())
                    .build();
        }
    }
}