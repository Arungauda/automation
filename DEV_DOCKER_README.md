# Invoice Automation - Development Docker Setup

This directory contains Docker configuration files for containerizing the Invoice Automation Spring Boot application for **development only**.

## Files Created

- **Dockerfile** - Simplified single-stage build for development
- **docker-compose.yml** - Development setup with MySQL, Redis, and app
- **application-dev.properties** - Development-optimized application configuration
- **.dockerignore** - Files to exclude from Docker context

## Quick Start

### 1. Build and Run Services
```bash
# Build and start all services
docker-compose up -d --build

# View logs
docker-compose logs -f app

# Check service status
docker-compose ps
```

### 2. Access the Application
- **Application**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **MySQL**: localhost:3306 (user: dev_user, password: dev_pass)
- **Redis**: localhost:6379

## Development Services

### Application Container
- **Base Image**: Eclipse Temurin JDK 17 Alpine
- **Memory**: 256MB max, 128MB min (optimized for development)
- **Profile**: Development
- **Logging**: DEBUG level for detailed output

### MySQL Container
- **Version**: MySQL 8.0
- **Database**: invoice_automation
- **User**: dev_user / dev_pass
- **Persistence**: Named volume `mysql_dev_data`

### Redis Container
- **Version**: Redis 7 Alpine
- **Persistence**: Named volume `redis_dev_data`
- **Simple Configuration**: No memory limits for development

## Development Features

### Hot Reload Support
- **Spring DevTools**: Enabled for automatic restarts
- **Live Reload**: Enabled for frontend changes
- **Resource Mounting**: Configuration files mounted for easy editing

### Enhanced Logging
- **SQL Logging**: Enabled with formatting
- **Hibernate Statistics**: Enabled for performance monitoring
- **Debug Logging**: Full debug output for troubleshooting

### Development Tools
- **Actuator Endpoints**: All endpoints exposed
- **Swagger UI**: Enabled for API testing
- **Health Checks**: Detailed health information

## Useful Commands

### Development Workflow
```bash
# Start services
docker-compose up -d

# View application logs
docker-compose logs -f app

# Rebuild application after code changes
docker-compose up -d --build app

# Stop services
docker-compose down

# Remove data (start fresh)
docker-compose down -v
```

### Database Management
```bash
# Connect to MySQL
docker-compose exec mysql mysql -u dev_user -pdev_pass invoice_automation

# Connect to Redis
docker-compose exec redis redis-cli
```

### Debugging
```bash
# Check container status
docker-compose ps

# View all logs
docker-compose logs

# Enter app container
docker-compose exec app sh
```

## Configuration Details

### Environment Variables
- **SPRING_PROFILES_ACTIVE**: dev
- **LOGGING_LEVEL_COM_INVOICE_AUTOMATION**: DEBUG
- **JAVA_OPTS**: -Xmx256m -Xms128m -XX:+UseG1GC

### Database Connection
- **Host**: mysql (container name)
- **Port**: 3306
- **Database**: invoice_automation
- **Credentials**: dev_user / dev_pass

### Redis Connection
- **Host**: redis (container name)
- **Port**: 6379

## Development Optimizations

### Performance Settings
- **Connection Pool**: Smaller pool for development (2-10 connections)
- **Cache**: EhCache with shorter TTL for development
- **JVM**: Lower memory footprint for local development

### Convenience Features
- **Port Mapping**: Direct access to all services
- **Volume Mounting**: Easy access to logs and cache
- **No Health Checks**: Faster startup for development

## Troubleshooting

### Common Issues

1. **Port Already in Use**
   ```bash
   # Check what's using the port
   netstat -tulpn | grep :8080
   
   # Kill the process or change port in docker-compose.yml
   ```

2. **Database Connection Failed**
   ```bash
   # Check MySQL container logs
   docker-compose logs mysql
   
   # Verify database exists
   docker-compose exec mysql mysql -u root -p -e "SHOW DATABASES;"
   ```

3. **Application Won't Start**
   ```bash
   # Check application logs
   docker-compose logs app
   
   # Rebuild from scratch
   docker-compose down -v
   docker-compose up -d --build
   ```

### Reset Development Environment
```bash
# Complete reset (removes all data)
docker-compose down -v
docker system prune -f
docker-compose up -d --build
```

## Tips for Development

1. **Code Changes**: Use `docker-compose up -d --build app` to rebuild only the application
2. **Configuration**: Edit `application-dev.properties` and restart the app container
3. **Database**: Use your favorite MySQL client to connect to localhost:3306
4. **Testing**: Use Swagger UI at http://localhost:8080/swagger-ui/index.html
5. **Debugging**: Check logs with `docker-compose logs -f app`

## File Structure
```
├── Dockerfile                 # Development build
├── docker-compose.yml         # Development services
├── .dockerignore             # Build exclusions
├── src/main/resources/
│   ├── application.properties # Default config
│   └── application-dev.properties # Development config
└── DOCKER_README.md          # This file
```
