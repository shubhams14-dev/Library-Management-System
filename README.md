# Library Management System

A comprehensive Library Management System built with Spring Boot, featuring book catalog search, user authentication, loan management, and more.

## Features

### Sprint 1 - Walking Skeleton & First Deploy
- ✅ **Search Catalog (Public)** - Search books without login
- ✅ **Book Details View** - Status-aware book information
- ✅ **Login/Logout** - Using seeded users (no registration)
- ✅ **Borrow Book (Happy Path)** - Basic borrowing functionality
- ✅ **Schema & Seed Data** - Database design with sample data
- ✅ **Dev Container** - VS Code development environment
- ✅ **Docker** - Multi-stage containerization
- ✅ **Render Deployment** - Live deployment
- ✅ **Unit Tests** - Loan rules testing

### Core Functionality
- **Public Book Search** - Browse and search books without authentication
- **User Authentication** - Secure login/logout with role-based access
- **Book Borrowing** - Members can borrow available books
- **Loan Management** - View active loans, return books, extend loans
- **Book Status Tracking** - Real-time availability status
- **Responsive UI** - Modern, mobile-friendly interface

## Technology Stack

- **Backend**: Spring Boot 3.2.0, Spring Security, Spring Data JPA
- **Frontend**: Thymeleaf, Bootstrap 5, Font Awesome
- **Database**: SQLite with Hibernate
- **Containerization**: Docker (multi-stage build)
- **CI/CD**: GitHub Actions
- **Deployment**: Render.com
- **Development**: VS Code Dev Container
- **Testing**: JUnit 5, Mockito
- **Monitoring**: Spring Boot Actuator

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- Docker (optional)
- VS Code with Dev Containers extension (recommended)

### Local Development

#### Prerequisites
- VS Code with "Dev Containers" extension
- Docker Desktop (for dev container support)

#### Using Dev Container (Recommended)

1. **Open the project**
   ```bash
   # Option 1: Open workspace file
   code library-management-system.code-workspace
   
   # Option 2: Open folder directly
   code .
   ```

2. **Start Dev Container**
   - Press `Ctrl+Shift+P` (or `Cmd+Shift+P` on Mac)
   - Type "Dev Containers: Reopen in Container"
   - Wait for container to build and start (first time may take a few minutes)

3. **Run setup script**
   ```bash
   ./dev-setup.sh
   ```

4. **Start the application**
   ```bash
   mvn spring-boot:run
   ```

5. **Access the application**
   - Open http://localhost:8080
   - Use demo credentials (see below)

#### Manual Setup (Not Recommended)
If you prefer to run locally without dev container:
- Ensure you have Java 17+ installed
- Run `mvn clean install` and `mvn spring-boot:run`

### Demo Credentials

| Role | Username | Password |
|------|----------|----------|
| Admin | admin | admin123 |
| Member | john.doe | password123 |
| Member | jane.smith | password123 |
| Member | bob.wilson | password123 |
| Member | alice.brown | password123 |

## API Endpoints

### Public Endpoints
- `GET /` - Home page with book search
- `GET /search` - Advanced search page
- `GET /books/{id}` - Book details
- `GET /books/isbn/{isbn}` - Book details by ISBN
- `GET /auth/login` - Login page

### Authenticated Endpoints
- `GET /loans/my-loans` - User's loan history
- `POST /loans/borrow` - Borrow a book
- `POST /loans/return/{id}` - Return a book
- `POST /loans/extend/{id}` - Extend a loan

## Database Schema

### Core Entities
- **User** - System users (Members, Admins)
- **Book** - Library catalog items
- **Loan** - Book borrowing records
- **Reservation** - Book reservation queue

### Key Relationships
- User → Loan (One-to-Many)
- Book → Loan (One-to-Many)
- User → Reservation (One-to-Many)
- Book → Reservation (One-to-Many)

## Docker Deployment

### Build Image
```bash
docker build -t library-management-system .
```

### Run Container
```bash
docker run -p 8080:8080 library-management-system
```

### Docker Compose (Optional)
```yaml
version: '3.8'
services:
  library-app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
```

## Testing

### Run Unit Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn verify
```

### Test Coverage
```bash
mvn jacoco:report
```

## CI/CD Pipeline

The project includes a GitHub Actions workflow that:
1. **Tests** - Runs unit tests on every push/PR
2. **Builds** - Compiles and packages the application
3. **Docker** - Builds and pushes Docker image
4. **Deploys** - Automatically deploys to Render.com

## Project Structure

```
src/
├── main/
│   ├── java/com/library/system/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── domain/          # Entity classes
│   │   ├── repository/      # Data access layer
│   │   ├── service/         # Business logic
│   │   └── LibraryManagementSystemApplication.java
│   └── resources/
│       ├── templates/       # Thymeleaf templates
│       ├── static/          # CSS, JS, images
│       └── application.yml  # Configuration
└── test/
    └── java/com/library/system/
        └── service/         # Unit tests
```

## Development Guidelines

### Code Style
- Follow Java naming conventions
- Use meaningful variable and method names
- Add Javadoc for public methods
- Keep methods small and focused

### Testing
- Write unit tests for all service methods
- Aim for >80% code coverage
- Use Mockito for mocking dependencies
- Test both success and failure scenarios

### Security
- All user inputs are validated
- Passwords are encrypted with BCrypt
- CSRF protection is enabled
- Role-based access control implemented

## Deployment

### Docker Deployment

#### Local Development
```bash
# Build and run with Docker Compose
docker-compose up --build

# Access the application at http://localhost:8080
```

#### Production Deployment
```bash
# Build the Docker image
docker build -t library-management-system .

# Run the container
docker run -p 8080:8080 -v library-data:/app/data library-management-system

# Or use production Docker Compose
docker-compose -f docker-compose.prod.yml up -d
```

### Render Deployment

#### Option 1: Using render.yaml
1. Push the `render.yaml` file to your repository
2. Connect your GitHub repository to Render
3. Render will automatically detect and deploy using the configuration

#### Option 2: Manual Configuration
1. Create a new Web Service in Render
2. Connect your GitHub repository
3. Configure:
   - **Build Command:** `./build.sh`
   - **Start Command:** `java -jar target/library-management-system-0.0.1-SNAPSHOT.jar`
   - **Environment:** `SPRING_PROFILES_ACTIVE=production`
   - **Health Check Path:** `/actuator/health`

### CI/CD Pipeline

The project includes a comprehensive GitHub Actions CI/CD pipeline:

- **Automated Testing:** Runs on every push and PR
- **Docker Build:** Multi-architecture builds (AMD64, ARM64)
- **Security Scanning:** Trivy vulnerability scanning
- **Container Registry:** Automatic push to GitHub Container Registry

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Spring profile | `production` |
| `SPRING_DATASOURCE_URL` | Database URL | `jdbc:sqlite:/app/data/library.db` |
| `JAVA_OPTS` | JVM options | `-Xmx512m -Xms256m` |

### Health Monitoring

- **Health Check:** `/actuator/health`
- **Application Info:** `/actuator/info`
- **Metrics:** `/actuator/metrics`

For detailed deployment instructions, see [DEPLOYMENT.md](DEPLOYMENT.md).

## Future Enhancements

### Sprint 2+ Features
- Book reservations with FIFO queue
- Email notifications for due dates
- Overdue fee management
- Admin catalog management
- Reporting and analytics
- Book reviews and ratings

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For questions or issues:
- Create an issue in the repository
- Check the documentation
- Review the test cases for usage examples

---

**Built with ❤️ using Spring Boot and modern web technologies**
