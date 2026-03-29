# Docker Setup Guide for HelloApp

This guide explains how to create and run a Docker image for the HelloApp Spring Boot application.

## Prerequisites

- Docker installed on your machine ([Download Docker](https://www.docker.com/products/docker-desktop))
- Docker Compose (usually comes with Docker Desktop)

## Method 1: Using Docker Build Command

### Step 1: Build the Docker Image

Navigate to the project root directory and run:

```bash
docker build -t helloapp:latest .
```

**Explanation:**
- `docker build` - Command to create a Docker image
- `-t helloapp:latest` - Tag the image with name `helloapp` and version `latest`
- `.` - Build context (current directory)

### Step 2: Run the Container

```bash
docker run -p 8080:8080 --name helloapp-container helloapp:latest
```

**Explanation:**
- `docker run` - Create and start a container from the image
- `-p 8080:8080` - Map port 8080 from container to host machine
- `--name helloapp-container` - Name the container
- `helloapp:latest` - Use the image we built

### Step 3: Access the Application

The application will be accessible at:
- Main URL: `http://localhost:8080`
- H2 Console: `http://localhost:8080/h2-console`

### Step 4: Stop the Container

```bash
docker stop helloapp-container
```

### Step 5: Remove the Container (optional)

```bash
docker rm helloapp-container
```

---

## Method 2: Using Docker Compose (Recommended)

### Step 1: Start the Application

Navigate to the project root directory and run:

```bash
docker-compose up --build
```

**Explanation:**
- `docker-compose up` - Start the services defined in docker-compose.yml
- `--build` - Build the image before starting

### Step 2: View Logs

```bash
docker-compose logs -f
```

### Step 3: Stop the Application

```bash
docker-compose down
```

This removes the container but keeps the image.

---

## Advanced Commands

### View Docker Images

```bash
docker images
```

### View Running Containers

```bash
docker ps
```

### View All Containers (including stopped)

```bash
docker ps -a
```

### Remove an Image

```bash
docker rmi helloapp:latest
```

### Interactive Shell in Container

```bash
docker exec -it helloapp-container /bin/sh
```

### Push to Docker Registry (e.g., Docker Hub)

First, tag your image:
```bash
docker tag helloapp:latest yourusername/helloapp:latest
```

Then push:
```bash
docker push yourusername/helloapp:latest
```

---

## Understanding the Dockerfile

The provided Dockerfile uses a **multi-stage build** approach:

**Stage 1: Builder**
- Uses Maven image to compile and package the application
- Reduces final image size by not including Maven in the final image

**Stage 2: Runtime**
- Uses lightweight Alpine JRE 21 image
- Copies only the compiled JAR file
- Much smaller final image size (~200MB vs ~500MB without multi-stage)

---

## Environment Variables

You can override Spring Boot properties using environment variables:

```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:h2:mem:testdb \
  -e SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop \
  helloapp:latest
```

Or in docker-compose.yml:

```yaml
environment:
  - SPRING_DATASOURCE_URL=jdbc:h2:mem:testdb
  - SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop
```

---

## Troubleshooting

### Container exits immediately
Check logs with: `docker logs helloapp-container`

### Port already in use
Use a different port: `docker run -p 9090:8080 helloapp:latest`

### Build fails with Maven dependencies
Clear Maven cache and try again:
```bash
docker build --no-cache -t helloapp:latest .
```

### H2 Console not accessible
Ensure `spring.h2.console.enabled=true` is set in application.properties

---

## Performance Optimization Tips

1. **Use Alpine images** - Already done in the Dockerfile for smaller size
2. **Layer caching** - Copy pom.xml before src to cache dependencies
3. **Remove dev tools** - In production, exclude spring-boot-devtools
4. **Health checks** - Already configured in the Dockerfile

---

## Example API Calls

Once running, you can test the endpoints:

```bash
# Test Hello Controller
curl http://localhost:8080/hello

# Get all employees
curl http://localhost:8080/allemployee

# Add new employee
curl -X POST http://localhost:8080/addemployee \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com","status":"Y"}'
```

---

## Production Considerations

For production deployments, consider:

1. Use a database volume to persist data (H2 in-memory)
2. Set up environment-specific properties
3. Use secrets management for sensitive data
4. Implement logging aggregation
5. Use container orchestration (Kubernetes, Docker Swarm)
6. Implement proper network policies
7. Use resource limits in docker-compose or orchestration platform

---

## Next Steps

After building your Docker image, you can:
- Push to Docker Hub for sharing
- Deploy to cloud platforms (AWS ECS, Azure Container Instances, Google Cloud Run)
- Use with Kubernetes for orchestration
- Integrate with CI/CD pipelines

