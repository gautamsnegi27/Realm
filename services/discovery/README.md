# Discovery Server (Eureka)

**Service Registry and Discovery for Microservices**

## 📋 Overview

The Discovery Server (Eureka) provides **service registration and discovery** capabilities, allowing microservices to find and communicate with each other without hard-coding host names and ports.

---

## 🎯 Role & Responsibilities

### **Primary Functions:**
1. ✅ **Service Registration** - All microservices register themselves on startup
2. ✅ **Service Discovery** - Clients can discover service instances dynamically
3. ✅ **Health Monitoring** - Tracks health of registered services via heartbeats
4. ✅ **Load Balancing** - Provides list of available instances for client-side load balancing
5. ✅ **Fault Tolerance** - Automatically removes unhealthy instances

---

## 🏗️ Spring Cloud Concepts

### **1. Netflix Eureka Server**

```java
@SpringBootApplication
@EnableEurekaServer  // Enables Eureka Server functionality
public class DiscoveryApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiscoveryApplication.class, args);
    }
}
```

**Key Annotations:**
- `@EnableEurekaServer` - Configures the application as a Eureka Server

### **2. How Service Discovery Works**

```mermaid
┌───────────────────────────────────────────────────┐
│  1. Service Registration                          │
│  User Service → POST /eureka/apps/USER-SERVICE    │
│  Register instance with metadata                  │
└─────────────┬─────────────────────────────────────┘
              │
              ↓
┌───────────────────────────────────────────────────┐
│  2. Heartbeat (every 30s)                         │
│  User Service → PUT /eureka/apps/USER-SERVICE/... │
│  "I'm alive"                                      │
└─────────────┬─────────────────────────────────────┘
              │
              ↓
┌───────────────────────────────────────────────────┐
│  3. Service Discovery                             │
│  Gateway → GET /eureka/apps/USER-SERVICE          │
│  Returns list of available instances              │
└─────────────┬─────────────────────────────────────┘
              │
              ↓
┌───────────────────────────────────────────────────┐
│  4. Client-Side Load Balancing                    │
│  Gateway picks instance from list                 │
│  Sends request to selected instance               │
└───────────────────────────────────────────────────┘
```

### **3. Service Registration (Client Side)**

Services register using `@EnableDiscoveryClient`:

```java
@SpringBootApplication
@EnableDiscoveryClient  // Register with Eureka
public class UserServiceApplication {
    // ...
}
```

Configuration:
```yaml
spring:
  application:
    name: user-service  # Service name in registry

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    fetch-registry: true    # Retrieve other services
    register-with-eureka: true  # Register self
  instance:
    prefer-ip-address: true   # Use IP instead of hostname
    lease-renewal-interval-in-seconds: 30  # Heartbeat interval
```

### **4. Service Discovery (Client Side)**

Using Spring Cloud LoadBalancer:
```java
@Configuration
public class RestTemplateConfig {
    
    @Bean
    @LoadBalanced  // Enable service discovery
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

// Usage
String response = restTemplate.getForObject(
    "http://USER-SERVICE/api/v1/user",  // Service name, not URL
    String.class
);
```

---

## 🔧 Configuration

### **Eureka Server Settings** (`application.yml`)

```yaml
server:
  port: 8761

spring:
  application:
    name: discovery-server

eureka:
  instance:
    hostname: localhost
  client:
    register-with-eureka: false  # Don't register self
    fetch-registry: false        # Don't fetch registry
    service-url:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
  server:
    enable-self-preservation: false  # Disable for dev (enable in prod)
    eviction-interval-timer-in-ms: 60000  # Check for expired leases every 60s
```

### **Understanding Key Properties**

| Property | Purpose | Recommended Value |
|----------|---------|-------------------|
| `register-with-eureka` | Should this server register itself? | `false` (for standalone) |
| `fetch-registry` | Should this server fetch registry? | `false` (for standalone) |
| `enable-self-preservation` | Protect against network partitions | `false` (dev), `true` (prod) |
| `lease-renewal-interval-in-seconds` | Heartbeat frequency | `30` seconds |
| `lease-expiration-duration-in-seconds` | Time before eviction | `90` seconds |

---

## 🚀 Running the Service

### **Local (IntelliJ)**
```bash
# Run main method in DiscoveryApplication.java
# Access dashboard: http://localhost:8761
```

### **Docker**
```bash
# Start Discovery Server
docker-compose up -d discovery
```

### **Verify It's Running**
```bash
# Check health
curl http://localhost:8761/actuator/health

# View registered services (Web UI)
open http://localhost:8761
```

---

## 📊 Eureka Dashboard

Access the Eureka Dashboard at **http://localhost:8761**

### **Dashboard Features:**

1. **System Status**
   - Uptime
   - Environment
   - Lease expiration enabled
   - Self-preservation mode

2. **Registered Instances**
   - Service name
   - Status (UP/DOWN)
   - Availability Zones
   - Instance ID

3. **General Info**
   - Total memory
   - Available instances
   - Renewal threshold

### **Example Dashboard View:**

```
Currently registered services:
┌─────────────────┬──────────┬─────────────────────────┐
│ Service Name    │ Status   │ Instance                │
├─────────────────┼──────────┼─────────────────────────┤
│ GATEWAY-SERVICE │ UP (1)   │ localhost:gateway:8111  │
│ USER-SERVICE    │ UP (1)   │ localhost:user:8891     │
│ CONFIG-SERVER   │ UP (1)   │ localhost:config:8888   │
└─────────────────┴──────────┴─────────────────────────┘
```

---

## 🔍 Key Concepts

### **1. Self-Preservation Mode**

**What is it?**
- Protection mechanism against network partitions
- Prevents Eureka from evicting all instances during temporary network failures

**When to enable?**
- ✅ **Production**: Always enable
- ❌ **Development**: Usually disable for faster instance eviction

```yaml
eureka:
  server:
    enable-self-preservation: true  # Production
    # enable-self-preservation: false  # Development
```

### **2. Service Instance Lifecycle**

```
1. STARTING → Service starts, begins registration
2. UP → Service registered and healthy
3. DOWN → Service stopped or unhealthy
4. OUT_OF_SERVICE → Manually marked as unavailable
5. UNKNOWN → Connection lost, waiting for eviction
```

### **3. Heartbeat Mechanism**

**Client Side:**
```
Every 30s → Client sends heartbeat to Eureka
If heartbeat fails 3 times → Instance marked as DOWN
After 90s without heartbeat → Instance evicted
```

**Server Side:**
```
Every 60s → Eureka checks for expired leases
If expired → Remove instance from registry
Notify clients of registry changes
```

---

## 🏆 Benefits of Service Discovery

| Benefit | Description |
|---------|-------------|
| **Dynamic Scaling** | Add/remove instances without configuration changes |
| **Fault Tolerance** | Automatically route around failed instances |
| **Load Distribution** | Distribute load across multiple instances |
| **No Hard-Coded URLs** | Services reference each other by name |
| **Zero-Downtime Deployments** | New instances registered automatically |

---

## 📈 Advanced Features

### **1. Zones and Regions**

For multi-datacenter deployments:
```yaml
eureka:
  instance:
    availability-zone: us-east-1a
  client:
    region: us-east-1
    availability-zones:
      us-east-1: us-east-1a,us-east-1b
```

### **2. Custom Metadata**

Add custom metadata to instances:
```yaml
eureka:
  instance:
    metadata-map:
      version: 1.0.0
      env: production
      team: platform
```

Retrieve in code:
```java
ServiceInstance instance = discoveryClient.getInstances("USER-SERVICE").get(0);
String version = instance.getMetadata().get("version");
```

### **3. Health Check Integration**

Integrate with Spring Boot Actuator:
```yaml
eureka:
  client:
    healthcheck:
      enabled: true  # Use /actuator/health for status
```

---

## 🐛 Troubleshooting

### **Service Not Appearing in Registry**

**Check:**
1. Service is running
2. Eureka URL is correct in service config
3. Eureka server is accessible
4. Check service logs for registration errors

```bash
# From service container
curl http://discovery:8761/eureka/apps/

# Check service logs
docker logs user-service | grep "Registered"
```

### **Instances Stuck in "OUT_OF_SERVICE"**

```bash
# Restart the instance
docker-compose restart user-service

# Or manually remove from Eureka
curl -X DELETE http://localhost:8761/eureka/apps/USER-SERVICE/instance-id
```

### **Self-Preservation Mode Triggered**

```
EMERGENCY! EUREKA MAY BE INCORRECTLY CLAIMING INSTANCES ARE UP
```

**Causes:**
- Network issues causing mass heartbeat failures
- Clock skew between instances

**Solutions:**
1. Check network connectivity
2. Sync system clocks (NTP)
3. Temporarily disable self-preservation (dev only)

---

## 🔐 Security Considerations

### **Enable Authentication**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .httpBasic();
        return http.build();
    }
}
```

Client configuration:
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://user:password@localhost:8761/eureka/
```

---

## 📚 Additional Resources

- [Netflix Eureka Documentation](https://github.com/Netflix/eureka/wiki)
- [Spring Cloud Netflix](https://spring.io/projects/spring-cloud-netflix)
- [Service Discovery Pattern](https://microservices.io/patterns/service-registry.html)

---

**Port**: `8761` (Docker), `8761` (Local)  
**Startup Order**: **2nd** (after Config Server)  
**UI**: http://localhost:8761



