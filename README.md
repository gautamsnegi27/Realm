# Realm Microservices

A production-ready Spring Boot microservices application with JWT authentication, rate limiting, and AWS deployment support.

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-ready-blue.svg)](https://www.docker.com/)
[![AWS](https://img.shields.io/badge/AWS-ECS%20ready-yellow.svg)](https://aws.amazon.com/ecs/)

---

## 🚀 Quick Start

### **Start Everything (Default)**
```bash
# Start all services (infrastructure + microservices)
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f gateway
```

### **Test the Application**
```bash
# Run authentication tests
./test-authentication.sh

# Or manually test
curl -X POST http://localhost:8111/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'
```

**📚 Full Setup Guide:** [QUICK_START.md](./QUICK_START.md)

---

## 🏗️ Architecture

### **Microservices**
- **Config Server** (8888) - Centralized configuration
- **Discovery Server** (8761) - Service registry (Eureka)
- **API Gateway** (8111) - Single entry point with JWT auth & rate limiting
- **User Service** (8892) - User management & authentication

### **Infrastructure**
- **PostgreSQL** (5432) - Relational database
- **MongoDB** (27017) - NoSQL database for user service
- **Mongo Express** (8081) - MongoDB admin UI
- **MailDev** (1080/1025) - Email testing
- **Keycloak** (9191) - Identity management (optional)

### **Architecture Diagram**
```
┌─────────────────────────────────────────────────┐
│  Client (Browser/Mobile App)                    │
└────────────────┬────────────────────────────────┘
                 │
                 ↓
┌─────────────────────────────────────────────────┐
│  API Gateway (8111)                             │
│  - JWT Authentication                           │
│  - Rate Limiting (100 req/60s)                  │
│  - CORS Configuration                           │
└────────────────┬────────────────────────────────┘
                 │
                 ↓
┌─────────────────────────────────────────────────┐
│  Discovery Server (Eureka)                      │
│  - Service Registration                         │
│  - Load Balancing                               │
└────────────────┬────────────────────────────────┘
                 │
        ┌────────┴────────┐
        ↓                 ↓
┌───────────────┐   ┌───────────────┐
│ User Service  │   │ Future        │
│ (MongoDB)     │   │ Services...   │
└───────────────┘   └───────────────┘
```

---

## ✨ Features

### **🔒 Security**
- ✅ **JWT Authentication** - Stateless, secure token-based auth
- ✅ **Rate Limiting** - 100 requests/60s per user/IP (Resilience4j)
- ✅ **API Gateway** - Centralized security & routing
- ✅ **CORS** - Configured for cross-origin requests
- ✅ **Password Encryption** - BCrypt hashing

### **⚙️ Configuration**
- ✅ **Environment Profiles** - local, test, prod configurations
- ✅ **Centralized Config** - Spring Cloud Config Server
- ✅ **External Secrets** - AWS Secrets Manager ready
- ✅ **Service Discovery** - Automatic service registration

### **🚀 Deployment**
- ✅ **Docker Compose** - One-command local deployment
- ✅ **Graceful Shutdown** - Zero-downtime deployments
- ✅ **Health Checks** - All services monitored
- ✅ **AWS Ready** - ECS/Fargate deployment support
- ✅ **CI/CD** - GitHub Actions & AWS CodePipeline

### **📊 Observability**
- ✅ **Actuator Endpoints** - Health checks & metrics
- ✅ **Structured Logging** - Production-ready logging
- ✅ **Service Registry UI** - Eureka dashboard

---

## 📋 Prerequisites

### **For Local Development**
- Docker Desktop
- Java 17+
- Maven 3.9+
- 4GB+ RAM available

### **For AWS Deployment**
- AWS Account
- AWS CLI configured
- GitHub account (for CI/CD)

---

## 🎯 API Endpoints

### **Authentication**
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/signup` | Create new user account |
| POST | `/api/v1/auth/login` | Login & get JWT token |

### **User Management**
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/v1/user` | Get current user profile | ✅ Yes |

---

## 🌍 Environment Profiles

### **Local (Default)**
```bash
# Development-friendly
# - Hardcoded secrets
# - DEBUG logging
# - localhost databases
docker-compose up -d
```

### **Test**
```bash
# CI/CD testing
# - Configurable via env vars
# - INFO logging
SPRING_PROFILES_ACTIVE=test docker-compose up -d
```

### **Production**
```bash
# AWS deployment
# - All secrets from AWS Secrets Manager
# - INFO/WARN logging
# - Managed databases (RDS, DocumentDB)
SPRING_PROFILES_ACTIVE=prod
```

---

## 🔧 Configuration

### **Local Development**
```yaml
# Hardcoded for convenience
jwt:
  secret: LocalDevSecretKey...
  expiration: 86400000  # 24 hours

database:
  host: localhost
  port: 27017
```

### **Production (AWS)**
```yaml
# Environment variables (AWS Secrets Manager)
jwt:
  secret: ${JWT_SECRET}  # Required
  expiration: ${JWT_EXPIRATION:3600000}  # 1 hour

database:
  host: ${MONGO_HOST}  # DocumentDB endpoint
  username: ${MONGO_USER}
  password: ${MONGO_PASSWORD}
```

---

## 🚀 Deployment

### **Local Development**
```bash
# Start everything
docker-compose up -d

# Stop everything
docker-compose down
```

### **AWS Deployment**

**Option 1: GitHub Actions (Recommended)**
```bash
# Automatic deployment on push
git push origin main      # → Production
git push origin develop   # → Test environment
```

**Option 2: Manual Deployment**
```bash
# Build and push to AWS ECR
./deploy-to-aws.sh prod us-east-1 <aws-account-id>
```

**Option 3: AWS CodePipeline**
- Uses `buildspec.yml`
- Native AWS CI/CD integration

**📚 Complete Deployment Guide:** [AWS_DEPLOYMENT_GUIDE.md](./docs/AWS_DEPLOYMENT_GUIDE.md)

---

## 📦 Project Structure

```
Realm/
├── services/
│   ├── config/          # Config Server (8888)
│   ├── discovery/       # Eureka Server (8761)
│   ├── gateway/         # API Gateway (8111)
│   └── user/            # User Service (8892)
├── keycloak/            # Keycloak configuration
├── docker-compose.yml   # Local deployment
├── buildspec.yml        # AWS CodeBuild spec
├── QUICK_START.md       # Getting started guide
└── docs/
    ├── AWS_DEPLOYMENT_GUIDE.md
    ├── deploy-to-aws.sh
    └── test-authentication.sh
```

---

## 🛠️ Technology Stack

### **Backend**
- Java 17
- Spring Boot 3.x
- Spring Cloud (Config, Gateway, Eureka)
- Spring Security with JWT
- Resilience4j (Rate Limiting)

### **Databases**
- MongoDB (User data)
- PostgreSQL (Future services)

### **Infrastructure**
- Docker & Docker Compose
- AWS ECS/Fargate
- AWS RDS & DocumentDB
- AWS Secrets Manager
- GitHub Actions

---

## 📊 Service Ports

| Service | Port | URL |
|---------|------|-----|
| API Gateway | 8111 | http://localhost:8111 |
| Config Server | 8888 | http://localhost:8888 |
| Discovery Server | 8761 | http://localhost:8761 |
| User Service | 8892 | http://localhost:8892 |
| MongoDB | 27017 | mongodb://localhost:27017 |
| Mongo Express | 8081 | http://localhost:8081 |
| PostgreSQL | 5432 | postgresql://localhost:5432 |
| MailDev UI | 1080 | http://localhost:1080 |
| Keycloak | 9191 | http://localhost:9191 |

---

## 🧪 Testing

### **Authentication Flow**
```bash
# Automated test script
./test-authentication.sh

# Manual testing
# 1. Signup
curl -X POST http://localhost:8111/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@example.com","password":"pass123"}'

# 2. Login
curl -X POST http://localhost:8111/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"john","password":"pass123"}'

# 3. Get user profile (use token from login)
curl -X GET http://localhost:8111/api/v1/user \
  -H "Authorization: Bearer <your-jwt-token>"
```

### **Rate Limiting Test**
```bash
# Should get 429 after 100 requests
for i in {1..101}; do
  curl -X GET http://localhost:8111/api/v1/user \
    -H "Authorization: Bearer <token>"
done
```

---

## 🐛 Troubleshooting

### **Services Not Starting**
```bash
# Check logs
docker-compose logs -f

# Restart specific service
docker-compose restart gateway

# Clean restart
docker-compose down -v
docker-compose up -d
```

### **Port Conflicts**
```bash
# Check what's using a port
netstat -ano | findstr :8111  # Windows
lsof -i :8111                 # Mac/Linux

# Kill process or change port in docker-compose.yml
```

### **Database Connection Issues**
```bash
# Check MongoDB
docker-compose exec mongodb mongosh -u admin -p test

# Check PostgreSQL
docker-compose exec postgres psql -U admin -d reminder_db
```

**📚 More Help:** See [QUICK_START.md](./QUICK_START.md#troubleshooting)

---

## 📈 Production Considerations

### **Cost Estimate (AWS)**
- **ECS Fargate**: ~$75/month
- **RDS PostgreSQL**: ~$30/month
- **DocumentDB**: ~$120/month
- **Other Services**: ~$50/month
- **Total**: ~$275-400/month

**Cost Optimization:**
- Use Fargate Spot (-70% cost)
- Single NAT Gateway for dev/staging
- RDS Single-AZ for non-prod

### **Performance**
- **Rate Limiting**: 100 req/60s per user/IP
- **JWT Expiration**: 1 hour (production), 24 hours (local)
- **Graceful Shutdown**: 60s timeout
- **Health Checks**: 30s interval

### **Scaling**
- Horizontal scaling via ECS service auto-scaling
- Load balancing via AWS ALB + Eureka
- Stateless services (JWT tokens)
- Database read replicas for scaling

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [QUICK_START.md](./QUICK_START.md) | Local development & Docker setup |
| [Keycloak Setup](./keycloak/README.md) | Keycloak configuration for OAuth2/Social login |

---

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📝 License

This project is licensed under the MIT License.

---

## 🙋 Support

- **Issues**: Create a GitHub issue
- **Documentation**: Check the docs folder
- **Email**: gautamsnegi27@gmail.com


---

**Built with ❤️ using Spring Boot & AWS**

🚀 **Ready to deploy?**

