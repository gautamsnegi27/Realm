# API Gateway

**Intelligent Request Routing & Centralized Security**

## 📋 Overview

The API Gateway serves as the **single entry point** for all client requests, providing routing, authentication, rate limiting, and load balancing across microservices.

---

## 🎯 Role & Responsibilities

### **Primary Functions:**
1. ✅ **Request Routing** - Routes requests to appropriate microservices
2. ✅ **Authentication & Authorization** - Validates JWT tokens for all protected endpoints
3. ✅ **Rate Limiting** - Prevents abuse with Resilience4j (100 req/60s)
4. ✅ **Load Balancing** - Distributes traffic across service instances
5. ✅ **CORS Handling** - Centralized cross-origin configuration
6. ✅ **Security Headers** - Adds security-related headers to responses
7. ✅ **Request/Response Transformation** - Modifies headers and bodies

---

## 🏗️ Spring Cloud Concepts

### **1. Spring Cloud Gateway**

```java
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

**Key Dependencies:**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

### **2. How Gateway Routing Works**

```mermaid
┌────────────────────────────────────────────────────┐
│  1. Client Request                                 │
│  POST http://localhost:8111/api/v1/auth/login      │
└─────────────┬──────────────────────────────────────┘
              │
              ↓
┌────────────────────────────────────────────────────┐
│  2. Gateway Receives Request                       │
│  • Match route predicates                          │
│  • Apply global filters (JWT validation)           │
└─────────────┬──────────────────────────────────────┘
              │
              ↓
┌────────────────────────────────────────────────────┐
│  3. Service Discovery                              │
│  • Query Eureka for USER-SERVICE instances         │
│  • Get list of available servers                   │
└─────────────┬──────────────────────────────────────┘
              │
              ↓
┌────────────────────────────────────────────────────┐
│  4. Load Balancing                                 │
│  • Select instance (round-robin)                   │
│  • Forward request: http://user-service:8891/...   │
└─────────────┬──────────────────────────────────────┘
              │
              ↓
┌────────────────────────────────────────────────────┐
│  5. Response                                       │
│  • Apply response filters                          │
│  • Return to client                                │
└────────────────────────────────────────────────────┘
```

### **3. Route Configuration**

#### **Declarative (YAML)**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://USER-SERVICE  # Load balanced using Eureka
          predicates:
            - Path=/api/v1/user/**, /api/v1/auth/**
          filters:
            - StripPrefix=0
```

#### **Programmatic (Java)**
```java
@Configuration
public class GatewayConfig {
    
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("user-service", r -> r
                .path("/api/v1/user/**", "/api/v1/auth/**")
                .uri("lb://USER-SERVICE")
            )
            .build();
    }
}
```

---

## 🔒 Security Implementation

### **1. JWT Authentication Filter**

```java
@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    
    @Autowired
    private HybridJwtValidator hybridJwtValidator;
    
    // Public endpoints that don't require authentication
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
        "/api/v1/auth/signup",
        "/api/v1/auth/login",
        "/api/v1/auth/oauth2",
        "/eureka",
        "/actuator"
    );
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        var path = request.getPath().toString();
        
        // Skip authentication for public endpoints
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }
        
        // Extract and validate JWT token
        var authHeader = request.getHeaders().getFirst("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
        }
        
        var token = authHeader.substring(7);
        
        try {
            // Validate token (supports both custom JWT and Keycloak JWT)
            var tokenInfo = hybridJwtValidator.validateToken(token);
            
            // Add user context to headers for downstream services
            var modifiedRequest = exchange.getRequest().mutate()
                .header("X-Auth-User-Id", tokenInfo.getUserId())
                .header("X-Auth-Username", tokenInfo.getUsername())
                .header("X-Auth-Email", tokenInfo.getEmail())
                .header("X-Auth-Token-Type", tokenInfo.getTokenType().toString())
                .build();
            
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
            
        } catch (Exception e) {
            return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
        }
    }
    
    @Override
    public int getOrder() {
        return -100; // Execute before other filters
    }
}
```

**Key Concepts:**
- `GlobalFilter` - Applied to all routes
- `Ordered` - Controls filter execution order (lower = earlier)
- `ServerWebExchange` - Reactive equivalent of HttpServletRequest/Response

### **2. Hybrid JWT Validation**

Supports two token types:
1. **Custom JWT** - Generated by User Service
2. **Keycloak JWT** - OAuth2/OIDC tokens from Keycloak

```java
@Component
public class HybridJwtValidator {
    
    private final JwtUtil customJwtUtil;
    private final JwtDecoder keycloakJwtDecoder;
    
    public TokenInfo validateToken(String token) {
        // Try custom JWT first
        try {
            if (customJwtUtil.validateToken(token)) {
                return extractCustomJwtInfo(token);
            }
        } catch (JwtException e) {
            // Fall through to Keycloak
        }
        
        // Try Keycloak JWT
        var jwt = keycloakJwtDecoder.decode(token);
        return extractKeycloakJwtInfo(jwt);
    }
}
```

---

## 🚦 Rate Limiting

### **Implementation with Resilience4j**

```yaml
resilience4j:
  ratelimiter:
    instances:
      gatewayRateLimiter:
        limitForPeriod: 100      # 100 requests
        limitRefreshPeriod: 60s  # per 60 seconds
        timeoutDuration: 5s      # Wait max 5s for permission
```

```java
@Component
@RefreshScope  // Enables dynamic config refresh
public class RateLimitingFilter implements GlobalFilter {

    @Value("${resilience4j.ratelimiter.instances.gatewayRateLimiter.limitForPeriod:100}")
    private int limitForPeriod;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String rateLimitKey = getRateLimitKey(exchange);
        RateLimiter rateLimiter = getRateLimiter(rateLimitKey);

        return Mono.just(exchange)
                .transformDeferred(RateLimiterOperator.of(rateLimiter))
                .flatMap(chain::filter)
                .onErrorResume(throwable -> {
                    return handleRateLimitExceeded(exchange);
                });
    }

    private String getRateLimitKey(ServerWebExchange exchange) {
        // Use authenticated user ID if available, otherwise IP address
        String userId = exchange.getRequest().getHeaders().getFirst("X-Auth-User-Id");
        return userId != null ? "user:" + userId : "ip:" + getClientIp(exchange);
    }
}
```

### **Rate Limiting Strategy:**
- **Per User**: If authenticated (via `X-Auth-User-Id`)
- **Per IP**: If anonymous (via `RemoteAddress`)
- **Resource-Level**: Each user/IP gets independent rate limits
- **429 Too Many Requests**: When limit exceeded

### **Configuration Parameters Explained:**

#### **`limitForPeriod`**
- **Purpose**: Maximum number of requests allowed per time window
- **Example**: `limitForPeriod: 100` = 100 requests per window
- **Scope**: Per user (if authenticated) or per IP (if anonymous)

#### **`limitRefreshPeriod`**
- **Purpose**: Time window duration for rate limiting
- **Example**: `limitRefreshPeriod: 60s` = 60-second windows
- **Behavior**: New permits are granted when window refreshes
- **Formats**: `60s`, `2m`, `1h`

#### **`timeoutDuration`**
- **Purpose**: How long to wait for permits when rate limit is exceeded
- **Example**: `timeoutDuration: 5s` = wait up to 5 seconds
- **Behavior**:
  - If refresh happens during wait → Request proceeds ✅
  - If timeout expires before refresh → HTTP 429 ❌

### **Rate Limiting Scenarios:**

#### **Scenario 1: Normal Usage**
```
User makes 50 requests in 60s → All succeed ✅
```

#### **Scenario 2: Limit Exceeded - Bad Timing**
```
00:00:30 - User makes 100 requests (limit reached)
00:00:45 - User makes 101st request
           → Wait 5s for permits
00:00:50 - Timeout expires (refresh at 00:01:00)
           → HTTP 429 ❌
```

#### **Scenario 3: Limit Exceeded - Good Timing**
```
00:00:58 - User makes 101st request
           → Wait 5s for permits
00:01:00 - Rate limit refreshes (2s into wait)
           → Request proceeds ✅
```

#### **Scenario 4: Multiple Users**
```
User A: 100 requests → User A rate limited
User B: 50 requests  → User B continues normally ✅
(Each user has independent rate limits)
```

### **Dynamic Configuration Refresh:**

Rate limiting supports dynamic configuration updates without restart:

```bash
# Update config in Config Server, then refresh gateway
curl -X POST http://localhost:8111/actuator/refresh

# Response shows changed properties:
["resilience4j.ratelimiter.instances.gatewayRateLimiter.limitForPeriod"]
```

**Requirements:**
- `@RefreshScope` annotation on rate limiting components
- Actuator refresh endpoint enabled
- Spring Cloud Config integration

---

## 🔧 Configuration

### **Gateway Settings** (`gateway-service.yml`)

```yaml
server:
  port: 8111

spring:
  application:
    name: gateway-service
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true  # Auto-create routes from Eureka
          lower-case-service-id: true
      routes:
        - id: user-service
          uri: lb://USER-SERVICE
          predicates:
            - Path=/api/v1/user/**, /api/v1/auth/**

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    fetch-registry: true
    register-with-eureka: true

# JWT Configuration
jwt:
  secret: ${JWT_SECRET}
  expiration: 3600000

# Keycloak Configuration
keycloak:
  enabled: true
  jwk-set-uri: http://localhost:9191/realms/realm-service/protocol/openid-connect/certs
```

---

## 📊 Predicates & Filters

### **Built-in Predicates**

| Predicate | Example | Description |
|-----------|---------|-------------|
| `Path` | `Path=/api/**` | Match URL path |
| `Method` | `Method=GET,POST` | Match HTTP method |
| `Header` | `Header=X-Request-Id, \d+` | Match header with regex |
| `Query` | `Query=version, 1.0` | Match query parameter |
| `Host` | `Host=**.example.com` | Match host name |
| `RemoteAddr` | `RemoteAddr=192.168.1.0/24` | Match IP range |
| `Before/After` | `Before=2023-01-01T00:00:00Z` | Time-based routing |

### **Built-in Filters**

| Filter | Example | Description |
|--------|---------|-------------|
| `AddRequestHeader` | `AddRequestHeader=X-Request-Foo, Bar` | Add header to request |
| `AddResponseHeader` | `AddResponseHeader=X-Response-Foo, Bar` | Add header to response |
| `StripPrefix` | `StripPrefix=1` | Remove path segments |
| `RewritePath` | `RewritePath=/api/(?<segment>.*), /${segment}` | Rewrite URL |
| `Retry` | `Retry=3` | Retry failed requests |
| `CircuitBreaker` | `CircuitBreaker=myCircuitBreaker` | Circuit breaker pattern |
| `RequestRateLimiter` | `RequestRateLimiter=10` | Built-in rate limiting |

---

## 🚀 Running the Service

### **Local (IntelliJ)**
```bash
# Run main method in GatewayApplication.java
# Access: http://localhost:8111
```

### **Docker**
```bash
# Start Gateway
docker-compose up -d gateway
```

### **Verify It's Running**
```bash
# Health check
curl http://localhost:8111/actuator/health

# Test routing (should fail without auth)
curl http://localhost:8111/api/v1/user

# Test with JWT
curl -H "Authorization: Bearer <token>" \
     http://localhost:8111/api/v1/user
```

---

## 📈 Advanced Features

### **1. Custom Global Filter**

```java
@Component
@Order(-1)  // Execute early
public class CustomGlobalFilter implements GlobalFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        
        // Add request ID for tracing
        var requestId = UUID.randomUUID().toString();
        exchange.getRequest().mutate()
            .header("X-Request-Id", requestId)
            .build();
        
        // Log request
        log.info("Request: {} {} - ID: {}", 
            request.getMethod(), 
            request.getURI(),
            requestId
        );
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // Log response
            log.info("Response: {} - ID: {}", 
                exchange.getResponse().getStatusCode(),
                requestId
            );
        }));
    }
}
```

### **2. Circuit Breaker Integration**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://USER-SERVICE
          predicates:
            - Path=/api/v1/user/**
          filters:
            - name: CircuitBreaker
              args:
                name: userServiceCircuitBreaker
                fallbackUri: forward:/fallback/user-service
```

```java
@RestController
@RequestMapping("/fallback")
public class FallbackController {
    
    @GetMapping("/user-service")
    public ResponseEntity<String> userServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body("User Service is currently unavailable. Please try again later.");
    }
}
```

### **3. Request/Response Logging**

```java
@Component
public class LoggingFilter implements GlobalFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        var startTime = System.currentTimeMillis();
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            var response = exchange.getResponse();
            var duration = System.currentTimeMillis() - startTime;
            
            log.info("Request: {} {} - Response: {} - Duration: {}ms",
                request.getMethod(),
                request.getURI(),
                response.getStatusCode(),
                duration
            );
        }));
    }
}
```

---

## 🐛 Troubleshooting

### **503 Service Unavailable**

**Causes:**
- Target service not registered in Eureka
- All instances are down
- Network connectivity issues

**Solutions:**
```bash
# Check Eureka registry
curl http://localhost:8761/eureka/apps/

# Verify service is running
docker-compose ps user-service

# Check Gateway logs
docker logs gateway
```

### **401 Unauthorized on Public Endpoints**

**Check filter logic:**
```java
private boolean isPublicEndpoint(String path) {
    return PUBLIC_ENDPOINTS.stream()
        .anyMatch(path::startsWith);  // Ensure this works correctly
}
```

### **CORS Errors**

Update SecurityConfig:
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    var configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(List.of("*"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Auth-User-Id"));
    configuration.setMaxAge(3600L);
    
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

---

## 📚 Additional Resources

- [Spring Cloud Gateway Documentation](https://spring.io/projects/spring-cloud-gateway)
- [API Gateway Pattern](https://microservices.io/patterns/apigateway.html)
- [Resilience4j Rate Limiter](https://resilience4j.readme.io/docs/ratelimiter)

---

**Port**: `8111` (Docker & Local)  
**Startup Order**: **3rd** (after Config & Discovery)  
**Entry Point**: All client requests go through Gateway



