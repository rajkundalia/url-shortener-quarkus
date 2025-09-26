# Quarkus URL Shortener Microservice

A modern, cloud-native Java microservice built with Quarkus that demonstrates enterprise patterns and best practices for URL shortening functionality. This project showcases clean architecture, DTO-first API design, reactive programming, and production-ready patterns in a lightweight, fast-starting microservice.

## Read the learnings at the end!

## Features

- **URL Shortening** with cryptographically secure custom short codes
- **HTTP 302 Redirection** for seamless user experience
- **Click Tracking and Statistics** with real-time analytics
- **Zero-setup H2 Database** - instant startup with no configuration required
- **Data Persistence** using Hibernate ORM with Panache for simplified data access
- **H2 Console** for real-time database inspection during development
- **Health Checks and Metrics** for production monitoring
- **OpenAPI Documentation** with interactive Swagger UI
- **Production-ready Error Handling** with consistent DTO-based responses
- **Clean Architecture** with proper separation of concerns and DTO patterns

## Prerequisites

- **JDK 17+** - [Download OpenJDK](https://openjdk.org/install/) or [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
- **Maven 3.8+** - [Installation Guide](https://maven.apache.org/install.html)

## Getting Started

### Instant Startup (Zero Configuration Required!)

1. **Clone or download the project**
   ```bash
   # If using git
   git clone https://github.com/rajkundalia/url-shortener-quarkus.git
   cd url-shortener-quarkus
   
   # Or simply extract the project files to a directory
   ```

2. **Start the application immediately** - no database setup needed!
   ```bash
   mvn quarkus:dev
   ```

   The application will start in seconds and be available at: `http://localhost:8080`

3. **Access Development Tools:**
    - **H2 Database Console**: `http://localhost:8080/h2-console`
        - JDBC URL: `jdbc:h2:mem:testdb`
        - Username: `sa`
        - Password: (leave empty)
    - **Quarkus DevUI**: `http://localhost:8080/q/dev`
    - **OpenAPI/Swagger UI**: `http://localhost:8080/q/swagger-ui`
    - **Health Checks**: `http://localhost:8080/q/health`

## API Documentation

### Create Short URL
**POST** `/api/urls`

Request body:
```json
{
  "longUrl": "https://www.example.com/very/long/url/path"
}
```

Response (201 Created):
```json
{
  "shortCode": "abc123",
  "longUrl": "https://www.example.com/very/long/url/path",
  "shortUrl": "http://localhost:8080/abc123",
  "createdAt": "2025-09-20T10:30:00"
}
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{"longUrl": "https://www.github.com/quarkusio/quarkus"}'
```

### Redirect to Original URL
**GET** `/{shortCode}`

Performs HTTP 302 redirect to the original URL and increments click counter.

**Example:**
```bash
curl -L http://localhost:8080/abc123
# Redirects to the original URL
```

### Get URL Statistics
**GET** `/api/urls/{shortCode}/stats`

Response (200 OK):
```json
{
  "shortCode": "abc123",
  "longUrl": "https://www.example.com/very/long/url/path",
  "shortUrl": "http://localhost:8080/abc123",
  "createdAt": "2025-09-20T10:30:00",
  "clickCount": 42
}
```

**Example:**
```bash
curl http://localhost:8080/api/urls/abc123/stats
```

## Building and Deployment

### JVM Mode (Standard Deployment)
```bash
# Build the application
mvn package

# Run the JAR file
java -jar target/quarkus-app/quarkus-run.jar
```

## Testing

Run the comprehensive test suite including unit and integration tests:

```bash
# Run all tests
mvn test

# Run tests in continuous mode during development
mvn quarkus:test
```

The test suite includes:
- **Unit Tests** for service layer with DTO conversions
- **Integration Tests** using RestAssured for REST endpoints
- **Validation Tests** for input constraints
- **Error Handling Tests** for all exception scenarios

## Technology Stack

- **Quarkus 3.x** - Supersonic Subatomic Java Framework
- **Java 17+** - Modern Java with latest language features
- **Maven** - Build and dependency management
- **Hibernate ORM with Panache** - Simplified JPA and database operations
- **H2 Database** - In-memory database for zero-setup development
- **JAX-RS (RESTEasy Reactive)** - Reactive REST endpoint implementation
- **JSON-B** - JSON binding for DTO serialization
- **Bean Validation** - Input validation with annotations
- **SmallRye OpenAPI** - OpenAPI 3.0 specification and Swagger UI
- **SmallRye Health** - Health check and metrics endpoints
- **RestAssured** - HTTP endpoint testing framework

### Quarkus Extensions Used:
- `quarkus-resteasy-reactive` - Reactive REST endpoints
- `quarkus-resteasy-reactive-jsonb` - JSON handling
- `quarkus-hibernate-orm-panache` - Simplified ORM
- `quarkus-jdbc-h2` - H2 database driver
- `quarkus-hibernate-validator` - Bean validation
- `quarkus-smallrye-openapi` - API documentation
- `quarkus-smallrye-health` - Health checks
- `quarkus-arc` - CDI dependency injection

## Architecture

The project follows clean architecture principles with clear separation of concerns:

### **Resource Layer** (REST API)
- REST endpoints using DTOs exclusively
- No direct entity exposure in API contracts
- Proper HTTP status codes and error handling
- OpenAPI documentation with DTO schemas

### **Service Layer** (Business Logic)
- Business logic implementation
- DTO ↔ Entity conversion patterns
- URL validation and short code generation
- Exception handling for business rules

### **Entity Layer** (Data Persistence)
- Panache entities for simplified database operations
- Database constraints and relationships
- Audit fields (timestamps, counters)

### **DTO Layer** (API Contracts)
- Clean separation between API and persistence models
- Input validation using Bean Validation
- Consistent response formats across all endpoints

## Configuration

Key configuration options in `application.properties`:

### H2 Database (Default - Zero Setup)
```properties
# H2 in-memory database (data resets on restart)
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:mem:testdb
quarkus.hibernate-orm.database.generation=drop-and-create

# H2 Console (development only)
quarkus.h2.console.enabled=true
```

### Application Settings
```properties
# Short code configuration
app.shortcode.length=6
app.base-url=http://localhost:8080

# OpenAPI Documentation
quarkus.swagger-ui.always-include=true
mp.openapi.extensions.smallrye.info.title=URL Shortener API
```

## Database Options

### Development Mode (Default)
- **H2 In-Memory Database** - Zero configuration required
- **Data Persistence** - Data resets on application restart
- **H2 Console Access** - Available at `/h2-console` for real-time inspection
- **JDBC URL**: `jdbc:h2:mem:testdb`
- **Username**: `sa` (Password: empty)

### Production Configuration
For production deployments, simply change the database configuration to PostgreSQL, MySQL, or other supported databases:

```properties
# Example: PostgreSQL
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/urlshortener
quarkus.datasource.username=user
quarkus.datasource.password=password
```

## Some learnings:
1. The build time would be double for this app compared to a SpringBoot.
2. The **hot reload** is **so good and so fast**! 
3. The size of a fat jar in spring boot and a jar + lib folders are almost similar.
4. I have not tried native or docker, and it looked promising based on what I read.
5. The startup time using ```java -jar java -jar target/quarkus-app/quarkus-run.jar```
   is **3 seconds** roughly.

**Ready to start developing?** Just run `mvn quarkus:dev` and begin exploring the API at `http://localhost:8080/q/swagger-ui`!