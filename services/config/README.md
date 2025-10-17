# Config Server

**Centralized Configuration Management Service**

## 📋 Overview

The Config Server provides **centralized external configuration** for all microservices in the Realm application. It acts as a single source of truth for application configurations across different environments.

---

## 🎯 Role & Responsibilities

### **Primary Functions:**
1. ✅ **Centralized Configuration Storage** - All service configurations in one place
2. ✅ **Environment-Specific Configs** - Separate configurations for `local`, `test`, and `prod`
3. ✅ **Dynamic Configuration Updates** - Services can refresh configs without restart (with `/actuator/refresh`)
4. ✅ **Version Control Integration** - Configuration changes tracked via Git (optional)
5. ✅ **Configuration Encryption** - Sensitive data encryption support

---

## 🏗️ Spring Cloud Concepts

### **1. Spring Cloud Config Server**

```java
@SpringBootApplication
@EnableConfigServer  // Enables Config Server functionality
public class ConfigApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigApplication.class, args);
    }
}
```

**Key Annotations:**
- `@EnableConfigServer` - Enables the application to serve configurations to other services

### **2. Configuration Structure**

```
src/main/resources/
├── application.yml              # Config server settings
└── configurations/
    ├── local/                   # Local development
    │   ├── discovery.yml
    │   ├── gateway-service.yml
    │   └── user-service.yml
    ├── test/                    # Testing environment
    │   ├── discovery.yml
    │   ├── gateway-service.yml
    │   └── user-service.yml
    └── prod/                    # Production environment
        ├── discovery.yml
        ├── gateway-service.yml
        └── user-service.yml
```

### **3. How Config Server Works**

```mermaid
┌─────────────────────────────────────────────────┐
│  1. Service Startup                             │
│  Spring Boot App starts                         │
└─────────────┬───────────────────────────────────┘
              │
              ↓
┌─────────────────────────────────────────────────┐
│  2. Fetch Configuration                         │
│  Service makes HTTP request to Config Server    │
│  GET /gateway-service/local                     │
└─────────────┬───────────────────────────────────┘
              │
              ↓
┌─────────────────────────────────────────────────┐
│  3. Config Server Responds                      │
│  Returns YAML/Properties configuration          │
│  Based on service name + profile                │
└─────────────┬───────────────────────────────────┘
              │
              ↓
┌─────────────────────────────────────────────────┐
│  4. Service Bootstraps                          │
│  Uses configuration to initialize beans          │
│  Database connections, ports, etc.              │
└─────────────────────────────────────────────────┘
```

### **4. Client Configuration**

Services connect using:
```yaml
spring:
  config:
    import: optional:configserver:http://localhost:8888
  application:
    name: user-service  # Must match config file name
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
```

---

## 🔧 Configuration

### **Server Settings** (`application.yml`)

```yaml
server:
  port: 8888

spring:
  application:
    name: config-server
  cloud:
    config:
      server:
        native:
          search-locations: classpath:/configurations/{profile}
  profiles:
    active: native  # Use file-based config (not Git)
```

### **Profile-Based Configuration Retrieval**

Config Server serves configurations based on:
1. **Service Name**: `{application-name}.yml`
2. **Profile**: `local`, `test`, or `prod`

**Example Request:**
```
GET http://localhost:8888/user-service/local
```

Returns:
- `configurations/local/user-service.yml`

---

## 🚀 Running the Service

### **Local (IntelliJ)**
```bash
# Run main method in ConfigApplication.java
# No special VM options needed
```

### **Docker**
```bash
# Start Config Server only
docker-compose up -d config
```

### **Verify It's Running**
```bash
# Check health
curl http://localhost:8888/actuator/health

# Fetch user-service config for local profile
curl http://localhost:8888/user-service/local
```

---

## 📊 Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/{application}/{profile}` | GET | Get service configuration |
| `/{application}-{profile}.yml` | GET | Get raw YAML config |
| `/actuator/health` | GET | Health check |
| `/actuator/env` | GET | Environment properties |

### **Example Requests:**
```bash
# Get gateway config for prod
curl http://localhost:8888/gateway-service/prod

# Get user-service config for local
curl http://localhost:8888/user-service/local

# Get raw YAML file
curl http://localhost:8888/user-service-local.yml
```

---

## 🔐 Security Best Practices

### **1. Encryption**
Config Server supports encrypted properties:
```yaml
spring:
  datasource:
    password: '{cipher}AQA...'  # Encrypted value
```

Enable with:
```yaml
encrypt:
  key: my-secret-key  # Use environment variable in prod
```

### **2. Access Control**
Protect Config Server with Spring Security:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

---

## 🏆 Benefits

| Benefit | Description |
|---------|-------------|
| **Centralization** | All configs in one place, easier to manage |
| **Environment Separation** | Clear separation of local, test, prod configs |
| **No Redeployment** | Change configs without rebuilding services |
| **Auditability** | Configuration changes tracked (if using Git backend) |
| **Consistency** | All services use same configuration source |

---

## 📈 Advanced Features

### **1. Dynamic Refresh**
Services can reload configuration without restart:
```bash
# Trigger refresh on a service
curl -X POST http://localhost:8891/actuator/refresh
```

Requires `@RefreshScope` on beans:
```java
@RefreshScope
@Component
public class MyComponent {
    @Value("${my.property}")
    private String myProperty;
}
```

### **2. Git Backend** (Alternative)
Instead of native file-based storage:
```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/org/config-repo
          default-label: main
```

### **3. Composite Configuration**
Combine multiple configuration sources:
```yaml
spring:
  cloud:
    config:
      server:
        composite:
          - type: native
            search-locations: classpath:/configurations
          - type: git
            uri: https://github.com/org/config-repo
```

---

## 🐛 Troubleshooting

### **Config Not Loading**
```bash
# Check if Config Server is accessible
curl http://localhost:8888/actuator/health

# Verify configuration exists
curl http://localhost:8888/user-service/local
```

### **Service Can't Connect**
Check client configuration:
```yaml
spring:
  config:
    import: optional:configserver:http://config:8888  # Docker
    # OR
    import: optional:configserver:http://localhost:8888  # Local
```

---

## 📚 Additional Resources

- [Spring Cloud Config Documentation](https://spring.io/projects/spring-cloud-config)
- [Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Encryption and Decryption](https://cloud.spring.io/spring-cloud-config/reference/html/#_encryption_and_decryption)

---

**Port**: `8888` (Docker), `8888` (Local)  
**Startup Order**: **1st** (must start before other services)



