# Deployment Guide

This document provides instructions for deploying the Library Management System to various platforms.

## Local Development

### Using Docker Compose

1. **Build and run the application:**
   ```bash
   docker-compose up --build
   ```

2. **Access the application:**
   - Application: http://localhost:8080
   - Health check: http://localhost:8080/actuator/health

### Using Maven

1. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

2. **Build the JAR:**
   ```bash
   mvn clean package
   java -jar target/library-management-system-0.0.1-SNAPSHOT.jar
   ```

## Production Deployment

### Using Docker

1. **Build the Docker image:**
   ```bash
   docker build -t library-management-system .
   ```

2. **Run the container:**
   ```bash
   docker run -p 8080:8080 -v library-data:/app/data library-management-system
   ```

3. **Using Docker Compose (Production):**
   ```bash
   docker-compose -f docker-compose.prod.yml up -d
   ```

### Deploying to Render

#### Option 1: Using Render's Docker Support

1. **Connect your GitHub repository to Render**
2. **Create a new Web Service**
3. **Configure the service:**
   - **Build Command:** `./build.sh`
   - **Start Command:** `java -jar target/library-management-system-0.0.1-SNAPSHOT.jar`
   - **Environment:** `SPRING_PROFILES_ACTIVE=production`
   - **Health Check Path:** `/actuator/health`

#### Option 2: Using Dockerfile

1. **Use the provided `Dockerfile.render`**
2. **Set the following environment variables:**
   - `SPRING_PROFILES_ACTIVE=production`
   - `JAVA_OPTS=-Xmx512m -Xms256m`

#### Option 3: Using render.yaml

1. **Push the `render.yaml` file to your repository**
2. **Render will automatically detect and use the configuration**

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Spring profile to use | `production` |
| `SPRING_DATASOURCE_URL` | Database URL | `jdbc:sqlite:/app/data/library.db` |
| `JAVA_OPTS` | JVM options | `-Xmx512m -Xms256m` |
| `SERVER_PORT` | Server port | `8080` |

### Health Checks

The application provides health check endpoints:

- **Health Check:** `/actuator/health`
- **Info:** `/actuator/info`
- **Metrics:** `/actuator/metrics`

### Database

The application uses SQLite for data persistence. The database file is stored in the `/app/data` directory and will persist across container restarts when using Docker volumes.

### Monitoring

The application includes Spring Boot Actuator for monitoring:

- **Health:** http://your-app-url/actuator/health
- **Info:** http://your-app-url/actuator/info
- **Metrics:** http://your-app-url/actuator/metrics

### Scaling

For horizontal scaling, consider:

1. **Using a shared database** (PostgreSQL, MySQL)
2. **Session management** with Redis
3. **Load balancing** with multiple instances

### Security Considerations

1. **Change default passwords** in production
2. **Use HTTPS** in production
3. **Configure proper CORS** settings
4. **Set up proper logging** and monitoring
5. **Regular security updates**

### Troubleshooting

#### Common Issues

1. **Port conflicts:** Ensure port 8080 is available
2. **Memory issues:** Adjust `JAVA_OPTS` for your environment
3. **Database issues:** Check file permissions for SQLite database
4. **Health check failures:** Verify the application is fully started

#### Logs

Check application logs for errors:

```bash
# Docker
docker logs <container-name>

# Render
# Check logs in Render dashboard
```

### Performance Optimization

1. **JVM tuning:** Adjust `JAVA_OPTS` based on available memory
2. **Database optimization:** Consider connection pooling settings
3. **Caching:** Implement Redis for session management
4. **CDN:** Use CDN for static assets

### Backup and Recovery

1. **Database backup:** Regularly backup the SQLite database file
2. **Configuration backup:** Keep configuration files in version control
3. **Disaster recovery:** Have a recovery plan for data loss scenarios
