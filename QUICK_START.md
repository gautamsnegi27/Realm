# Realm Microservices - Quick Start Guide

## 🎯 Overview

This project is designed for seamless development and deployment with **production-ready features**:

- **`docker-compose up -d`** (default) → Starts everything (Infrastructure + all microservices)
- All services configured for both Docker and local (IntelliJ) development
- **Production-grade JWT authentication** at Gateway level
- **Environment-specific configurations** (local/test/prod)
- **Rate limiting** (100 requests/60s per user/IP)
- **Graceful shutdown** for zero-downtime deployments
- **AWS deployment ready** with automated scripts

---

## 🆕 Production Features

### **1. Environment Profiles**
- **Local:** Development-friendly (hardcoded secrets, DEBUG logs)
- **Test:** CI/CD testing (configurable via env vars)
- **Prod:** AWS-ready (all secrets from environment variables)

```bash
# Local development (default)
SPRING_PROFILES_ACTIVE=local docker-compose up -d

# Test environment
SPRING_PROFILES_ACTIVE=test docker-compose up -d

# Production (AWS ECS)
SPRING_PROFILES_ACTIVE=prod  # Managed by ECS task definition
```

### **2. Rate Limiting**
- **100 requests per 60 seconds** per resource (IP or User ID)
- Configurable via environment variables
- Built with Resilience4j (free, open-source)

### **3. Graceful Shutdown**
- All services support graceful shutdown
- Completes ongoing requests before shutdown
- Perfect for rolling updates

### **4. AWS Deployment**
- Deploy with: `./deploy-to-aws.sh prod us-east-1 <account-id>`
- GitHub Actions CI/CD included
- AWS CodePipeline support

📚 **See:** [PRODUCTION_FEATURES_SUMMARY.md](./PRODUCTION_FEATURES_SUMMARY.md) for complete details

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Understanding Startup Order](#understanding-startup-order)
3. [Deployment Scenarios](#deployment-scenarios)
4. [Scenario 1: Hybrid Development (RECOMMENDED)](#scenario-1-hybrid-development-recommended-)
5. [Scenario 2: Full Docker Deployment](#scenario-2-full-docker-deployment)
6. [Scenario 3: Everything in IntelliJ](#scenario-3-everything-in-intellij)
7. [Service Access Points](#service-access-points)
8. [Common Commands](#common-commands)
9. [Troubleshooting](#troubleshooting)
10. [Quick Reference](#quick-reference)

---

## Prerequisites

### For Docker Deployment:
- Docker Desktop installed and running
- At least 4GB RAM available
- Ports available: 5432, 27017, 8081, 1080, 1025, 9191, 8888, 8761, 8111, 8892

**Quick Start:**
```bash
docker-compose up -d  # Start everything!
```

### For IntelliJ Development:
- Java 21
- Maven 3.9+
- IntelliJ IDEA (or any IDE)

---

## 🔄 Understanding Startup Order

### Why Order Matters

Microservices have dependencies that must be satisfied before they can start properly. The system follows this hierarchy:

```
Infrastructure → Config Server → Discovery Server → Application Services
```

### Docker Compose Automatic Ordering

When using `docker-compose --profile apps up -d`, services start automatically in the correct order using `depends_on` with health checks:

#### **Level 1: Infrastructure (Start Immediately)**
```
postgres, mongodb, mongo-express, mail-dev, keycloak
↓ Start in parallel, no dependencies
```

#### **Level 2: Config Server**
```yaml
config:
  healthcheck: service_healthy  # Must pass health check!
```
- **Starts:** After infrastructure
- **Health check:** Verifies `http://localhost:8888/discovery/default` responds
- **Wait time:** 30 seconds initial + health checks every 10s
- **Next level waits for:** Config to be **HEALTHY** ✅

#### **Level 3: Discovery Server**
```yaml
discovery:
  depends_on:
    config: service_healthy  # Waits for Config to be HEALTHY
  healthcheck: service_healthy
```
- **Starts:** Only after Config is **HEALTHY**
- **Health check:** Verifies `http://localhost:8761/eureka/apps` responds
- **Wait time:** 40 seconds initial + health checks every 10s
- **Next level waits for:** Discovery to be **HEALTHY** ✅

#### **Level 4: Application Services (Start in Parallel)**
```yaml
gateway, user-service:
  depends_on:
    config: service_healthy      # Both must be HEALTHY
    discovery: service_healthy
    mongodb: service_started     # Just needs to be started
```
- **Starts:** Only after Config AND Discovery are **HEALTHY**
- **All start in parallel** once dependencies are met

### Timeline Example

```
0:00  Infrastructure starts (postgres, mongodb, etc.)
      ↓
0:05  All infrastructure running
      ↓
0:05  Config Server starts
      ↓
0:35  Config Server becomes HEALTHY ✅
      ↓
0:35  Discovery Server starts
      ↓
1:15  Discovery Server becomes HEALTHY ✅
      ↓
1:15  Gateway & User Service start in PARALLEL
      ↓
1:45  All services up and registered in Eureka ✅
```

**Total startup time:** ~2 minutes (first run with image building)

### Key Conditions

**`condition: service_healthy`**
- Service must pass health check (not just be running)
- Used for: Config, Discovery
- Ensures services are fully functional before dependents start

**`condition: service_started`**
- Service just needs to be started
- Used for: postgres, mongodb
- These start quickly and are reliable

### IntelliJ Startup Order

When running services manually in IntelliJ, follow the same order:

1. **Config Server** (port 8888) - Start first, wait for full startup
2. **Discovery Server** (port 8761) - Start after Config, wait for full startup  
3. **Gateway & User Service** (ports 8111, 8892) - Start after Discovery

**Wait for each service to fully start before starting the next one!**

---

## 🚀 Deployment Scenarios

Choose the deployment approach that fits your needs:

| Scenario | Command | Infrastructure | Microservices | Best For |
|----------|---------|---------------|---------------|----------|
| **Full Docker** ⭐ | `docker-compose up -d` | Docker | Docker | Complete system, testing, demos |
| **Hybrid** | `docker-compose up -d` + stop apps | Docker | IntelliJ | Daily development, debugging |
| **All IntelliJ** | (manual setup) | Local install | IntelliJ | Pure local development |

---

## Scenario 1: Full Docker Deployment (RECOMMENDED) ⭐

**Perfect for:** Testing complete system, demos, production-like environment

### Start Everything with One Command

```bash
# Start all services (infrastructure + microservices)
docker-compose up -d

# Check status
docker-compose ps
```

**What runs:** Everything!
- ✅ PostgreSQL (port 5432)
- ✅ MongoDB (port 27017)
- ✅ Mongo Express (port 8081)
- ✅ MailDev (port 1080, 1025)
- ✅ Keycloak (port 9191)
- ✅ Config Server (port 8888)
- ✅ Discovery Server (port 8761)
- ✅ Gateway (port 8111)
- ✅ User Service (port 8892)

⏱️ **First startup:** 2-3 minutes (building Docker images)  
⏱️ **Subsequent startups:** 30-60 seconds

### Verify Everything Works


1. **Check Eureka Dashboard:** http://localhost:8761
   - Should show: GATEWAY-SERVICE, USER-SERVICE

2. **Test Authentication:**
   ```bash
   # Signup
   curl -X POST http://localhost:8111/api/v1/auth/signup \
     -H "Content-Type: application/json" \
     -d '{"username":"john","email":"john@example.com","password":"password123"}'
   
   # Login (returns JWT token)
   curl -X POST http://localhost:8111/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"usernameOrEmail":"john","password":"password123"}'
   ```

3. **Check MongoDB:** http://localhost:8081 (login: admin/test)
   - Should see `user-service` database with users

---

## Scenario 2: Hybrid Development

**Perfect for:** Daily development with debugging capabilities

### Step 1: Start Everything in Docker

```bash
docker-compose up -d
```

### Step 2: Stop Microservices (keep infrastructure running)

```bash
docker-compose stop config discovery gateway user-service
```

### Step 3: Start Microservices in IntelliJ

**IMPORTANT:** Start services in this order:

1. **Config Service** (`ConfigApplication.java`) - Port: 8888
2. **Discovery Service** (`DiscoveryApplication.java`) - Port: 8761
3. **Gateway Service** (`GatewayApplication.java`) - Port: 8111
4. **User Service** (`UserApplication.java`) - Port: 8892

### Step 4: Daily Development Workflow

```bash
# Morning
docker-compose up -d                              # Start everything
docker-compose stop config discovery gateway user-service  # Stop microservices

# Run microservices in IntelliJ for debugging
# Debug, hot reload, set breakpoints freely

# Evening
docker-compose down
```

**Advantages:**
- ✅ Easy debugging with breakpoints
- ✅ Instant code changes (hot reload)
- ✅ Direct log access in IntelliJ console
- ✅ Can restart individual services quickly
- ✅ Lower memory usage (only infra in Docker)

**Note:** Docker Compose handles startup order automatically. In IntelliJ, you must start services manually in the correct order (see [Understanding Startup Order](#understanding-startup-order)).

---

## Scenario 3: Everything in IntelliJ

**Perfect for:** Pure local development without Docker

### View Logs (Docker Mode)

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f config
docker-compose logs -f user-service

# Last 50 lines
docker-compose logs --tail=50 user-service
```

### Debugging Docker Services

**Option 1: Check Logs**
```bash
docker-compose logs -f user-service
```

**Option 2: Stop & Debug in IntelliJ**
```bash
docker-compose stop user-service
# Run UserApplication in IntelliJ with breakpoints
```

### Stop Everything

```bash
docker-compose down      # Stop all services
docker-compose down -v   # Stop + remove volumes (clean slate)
```

---

**Perfect for:** Pure local development without any Docker

### Prerequisites

Install locally:
- PostgreSQL (port 5432, database: `user-service`, user: `admin`, password: `test`)
- MongoDB (port 27017, user: `admin`, password: `test`)

### Start Services

Run in IntelliJ in order:
1. ConfigApplication (8888)
2. DiscoveryApplication (8761)
3. GatewayApplication (8111)
4. UserApplication (8892)

**Note:** Services automatically use `localhost` for local development

---

## 🌐 Service Access Points

| Service | URL | Credentials | Notes |
|---------|-----|-------------|-------|
| **Config Server** | http://localhost:8888/actuator/health | - | Check: `/discovery/default` |
| **Eureka Dashboard** | http://localhost:8761 | - | View registered services |
| **API Gateway** | http://localhost:8111 | - | All APIs route through here |
| **User Service** | http://localhost:8892 | - | Direct access (dev only) |
| **Mongo Express** | http://localhost:8081 | admin / test | MongoDB web UI |
| **MailDev** | http://localhost:1080 | - | Email testing UI |
| **Keycloak** | http://localhost:9191 | admin / test | Auth server |
| **PostgreSQL** | localhost:5432 | admin / test | Via DBeaver/pgAdmin |
| **MongoDB** | localhost:27017 | admin / test | Via Compass/Studio 3T |

---

## 🛠️ Common Commands

### Docker Commands

```bash
# Start everything (default)
docker-compose up -d

# Start and rebuild everything
docker-compose up -d --build

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v

# View all services status
docker-compose ps
```

### Service Management

```bash
# Check status
docker-compose ps

# View logs (follow mode)
docker-compose logs -f config
docker-compose logs -f discovery
docker-compose logs -f user-service

# Restart specific service
docker-compose restart user-service

# Stop specific service
docker-compose stop user-service

# Start stopped service
docker-compose start user-service

# Rebuild single service
docker-compose up -d --build user-service
```

### Cleanup Commands

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v

# Remove all images
docker-compose down --rmi all

# Nuclear option - clean everything
docker-compose down -v --rmi all
docker system prune -a --volumes
```

### Monitoring

```bash
# Check resource usage
docker stats

# List all containers
docker ps -a

# View specific container logs
docker logs ms_config --tail 50
docker logs ms_discovery --follow

# Check container health
docker inspect ms_config | grep -i health
```

---

## 🔧 Troubleshooting

### Issue: Services won't start

**Symptoms:**
```bash
docker-compose up -d
# Some containers fail to start
```

**Solution:**
```bash
# Check logs for specific service
docker logs ms_config --tail 50

# Clean restart
docker-compose down
docker-compose up -d --build
```

---

### Issue: Config Server unhealthy

**Symptoms:**
```
dependency failed to start: container ms_config is unhealthy
```

**Check logs:**
```bash
docker logs ms_config --tail 50
```

**Common causes:**
- Port 8888 already in use
- Service starting too slowly (increase health check `start_period`)

**Solution:**
```bash
# Stop conflicting service in IntelliJ
# Or change port in docker-compose.yml

# Restart
docker-compose down
docker-compose up -d
```

---

### Issue: Services not registering with Eureka

**Check Eureka:** http://localhost:8761

**Symptoms:** Gateway or User service not showing

**Solutions:**

1. **If using IntelliJ:** Restart the service
2. **If using Docker:**
   ```bash
   docker-compose logs user-service
   # Look for: "DiscoveryClient_USER-SERVICE - registration status: 204"
   ```
3. **Check Config Server:**
   ```bash
   curl http://localhost:8888/user-service/default
   # Should return configuration JSON
   ```

---

### Issue: Gateway returns 503 (Service Unavailable)

**Cause:** Backend service not registered in Eureka

**Solution:**
1. Check Eureka Dashboard: http://localhost:8761
2. Ensure USER-SERVICE is registered
3. Wait 30 seconds for service discovery to refresh
4. Restart Gateway if needed

---

### Issue: MongoDB connection failed (User Service)

**Error in logs:**
```
com.mongodb.MongoTimeoutException: Timed out after 30000 ms
```

**Solutions:**

**If using Docker:**
```bash
# Ensure MongoDB is running
docker-compose ps mongodb

# Check MongoDB logs
docker logs ms_mongo_db

# Restart user-service
docker-compose restart user-service
```

**If using IntelliJ:**
- Ensure MongoDB Docker container is running: `docker-compose ps`

---


### Issue: Port already in use

**Error:**
```
bind: address already in use
```

**Find what's using the port:**
```bash
# Windows
netstat -ano | findstr :8888

# macOS/Linux
lsof -i :8888
```

**Solutions:**
1. Stop the conflicting service
2. Or change port in `docker-compose.yml`

---

### Clean Restart (Nuclear Option)

If nothing works, do a complete clean restart:

```bash
# Stop everything
docker-compose down -v

# Remove orphaned containers
docker-compose down --remove-orphans

# Rebuild from scratch
docker-compose build --no-cache
docker-compose up -d

# Watch logs
docker-compose logs -f
```

---

## 📚 Quick Reference

### Service Overview

```
docker-compose up -d  →  Starts Everything:

┌──────────────────────────────────────┐
│  INFRASTRUCTURE                      │
│  • postgres                          │
│  • mongodb                           │
│  • mongo-express                     │
│  • mail-dev                          │
│  • keycloak                          │
└──────────────────────────────────────┘
           ↓ (automatic startup order)
┌──────────────────────────────────────┐
│  MICROSERVICES                       │
│  • config          (healthy check)   │
│  • discovery       (healthy check)   │
│  • gateway                           │
│  • user-service                      │
└──────────────────────────────────────┘
```

### Startup Order (IntelliJ)

```
1. ConfigApplication (8888)      ← Start first, wait for full startup
      ↓
2. DiscoveryApplication (8761)   ← Wait for Config to be fully running
      ↓
3. GatewayApplication (8111)     ← Wait for Discovery to be fully running
   UserApplication (8892)        ← Can start in parallel with Gateway
```

**Important:** Always wait for each service to fully start before starting the next one!

### Port Reference

```
Infrastructure:
  5432  → PostgreSQL
  27017 → MongoDB
  8081  → Mongo Express
  1080  → MailDev (Web UI)
  1025  → MailDev (SMTP)
  9191  → Keycloak

Microservices:
  8888  → Config Server
  8761  → Discovery (Eureka)
  8111  → Gateway
  8892  → User Service
```

### Default Credentials

```
MongoDB:          admin / test
PostgreSQL:       admin / test
Mongo Express:    admin / test
Keycloak:         admin / test
MailDev:          (no auth)
```

---

## 🎯 Recommended Workflows

### Daily Development (Docker + IntelliJ Hybrid)

```bash
# Start everything
docker-compose up -d

# Stop microservices to run in IntelliJ
docker-compose stop config discovery gateway user-service

# In IntelliJ: Run services for debugging

# Evening
docker-compose down
```

---

### Testing/Demo (Full Docker)

```bash
# Start everything
docker-compose up -d

# Verify
curl http://localhost:8761  # Eureka
curl http://localhost:8081  # Mongo Express

# Test authentication
curl -X POST http://localhost:8111/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","email":"demo@example.com","password":"password123"}'

# Stop
docker-compose down
```

---

## 📖 Additional Resources

- **Complete Setup Guide:** [DOCKER_SETUP.md](DOCKER_SETUP.md)
- **All Fixes Explained:** [DOCKER_FIXES_SUMMARY.md](DOCKER_FIXES_SUMMARY.md)
- **Local vs Docker Config:** [LOCAL_VS_DOCKER_CONFIG.md](LOCAL_VS_DOCKER_CONFIG.md)

---

## 🎉 You're Ready!

**Quick Start:**
```bash
docker-compose up -d
```

Everything starts automatically! For debugging, stop specific services and run them in IntelliJ. 🚀

**Authentication is live:** Use `/api/v1/auth/signup` and `/api/v1/auth/login` endpoints!

---

**Need help?** Check the [Troubleshooting](#troubleshooting) section or the detailed guides linked above.
