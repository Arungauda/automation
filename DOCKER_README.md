# Invoice Automation - Docker Setup

This directory contains Docker configuration files for containerizing the Invoice Automation Spring Boot application.

## Files Created

- **Dockerfile** - Multi-stage build for optimized container image
- **docker-compose.yml** - Complete multi-service setup with MySQL, Redis, and app
- **.env.example** - Environment variables template
- **nginx.conf** - Nginx reverse proxy configuration
- **application-prod.properties** - Production-ready application configuration
- **.dockerignore** - Files to exclude from Docker context

## Quick Start

### 1. Setup Environment Variables
```bash
cp .env.example .env
# Edit .env with your preferred values
```

### 2. Build and Run Services
```bash
# Build and start all services
docker-compose up -d --build

# View logs
docker-compose logs -f app

# Check service health
docker-compose ps
```

### 3. Access the Application
- **Application**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **Health Check**: http://localhost:8080/actuator/health
- **MySQL**: localhost:3306
- **Redis**: localhost:6379

## Services

### Application Container
- **Base Image**: Eclipse Temurin JRE 17 Alpine
- **User**: Non-root user (appuser:1000)
- **Memory**: 512MB max, 256MB min
- **Health Check**: Every 30s after 60s startup

### MySQL Container
- **Version**: MySQL 8.0
- **Persistence**: Named volume `mysql_data`
- **Health Check**: MySQL admin ping

### Redis Container
- **Version**: Redis 7 Alpine
- **Memory Limit**: 256MB
- **Persistence**: Named volume `redis_data`
- **Policy**: LRU eviction

### Nginx (Optional)
- **Profile**: Use `--profile production` to enable
- **Ports**: 80, 443
- **Features**: Security headers, API routing, SSL ready

## Production Deployment

### With Nginx Reverse Proxy
```bash
docker-compose --profile production up -d --build
```

### Environment-Specific Configuration
- **Development**: Use default `application.properties`
- **Production**: Uses `application-prod.properties`
- **Profile**: Set via `SPRING_PROFILES_ACTIVE=prod`

## Monitoring & Maintenance

### Health Checks
```bash
# Check all services
docker-compose ps

# Check specific service
docker-compose exec app curl -f http://localhost:8080/actuator/health
```

### Logs
```bash
# Follow application logs
docker-compose logs -f app

# View MySQL logs
docker-compose logs mysql

# View Redis logs
docker-compose logs redis
```

### Database Management
```bash
# Connect to MySQL
docker-compose exec mysql mysql -u root -p invoice_automation

# Connect to Redis
docker-compose exec redis redis-cli
```

## Security Considerations

1. **Change Default Passwords**: Update `.env` with secure passwords
2. **Network Isolation**: Services communicate via internal network
3. **Non-root User**: Application runs as non-root user
4. **Volume Permissions**: Proper file permissions for cache and logs
5. **SSL/TLS**: Uncomment HTTPS section in nginx.conf for production

## Performance Tuning

### JVM Options
- **Heap Size**: Configured via `JAVA_OPTS` environment variable
- **GC**: G1 Garbage Collector optimized for containers
- **Memory**: Uses 75% of container memory by default

### Database Connection Pool
- **Min Idle**: 5 connections
- **Max Pool**: 20 connections
- **Timeout**: 20s connection timeout

### Caching Strategy
- **EhCache**: Local disk persistence
- **Redis**: Distributed caching for sessions
- **Hibernate**: Second-level cache enabled

## Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Check MySQL container health: `docker-compose logs mysql`
   - Verify environment variables in `.env`
   - Ensure database is created and accessible

2. **Redis Connection Failed**
   - Check Redis container: `docker-compose logs redis`
   - Verify Redis is running: `docker-compose exec redis redis-cli ping`

3. **Application Won't Start**
   - Check application logs: `docker-compose logs app`
   - Verify Java memory settings
   - Check for port conflicts

### Cleanup
```bash
# Stop and remove all containers
docker-compose down

# Remove volumes (WARNING: This deletes data!)
docker-compose down -v

# Remove images
docker-compose down --rmi all
```

## Development Workflow

### Local Development
```bash
# Start only database and Redis
docker-compose up -d mysql redis

# Run application locally with containerized services
./gradlew bootRun
```

### Testing
```bash
# Run tests with containerized dependencies
docker-compose -f docker-compose.test.yml up --build --abort-on-container-exit
```

### Scaling
```bash
# Scale application instances
docker-compose up -d --scale app=3
```
