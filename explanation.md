# Library Management System - Complete Codebase Explanation

## Table of Contents
1. [System Overview](#system-overview)
2. [Technology Stack & Architecture](#technology-stack--architecture)
3. [Database Model (JPA-Based)](#database-model-jpa-based)
4. [Core Functionalities](#core-functionalities)
5. [Frontend Templates](#frontend-templates)
6. [Testing Strategy](#testing-strategy)
7. [DevOps & Deployment](#devops--deployment)

---

## System Overview

The Library Management System is a full-stack Spring Boot application that provides:
- **Public Access**: Browse and search books without authentication
- **Member Features**: Borrow, return, extend loans, and reserve books
- **Reservation System**: FIFO queue with 24-hour pickup windows
- **Admin Features**: User and catalog management (future enhancement)

**Key Business Rules**:
- Maximum 5 active loans per user
- 14-day loan period with one-time extension
- Cannot extend if book has active reservations
- FIFO reservation queue with automatic promotion
- 24-hour pickup window when book becomes available

---

## Technology Stack & Architecture

### Backend Stack
- **Framework**: Spring Boot 3.2.0 (Java 17)
- **Data Access**: **Spring Data JPA with Hibernate** (NOT JDBC)
- **Database**: SQLite with WAL mode for concurrency
- **Security**: Spring Security with BCrypt password hashing
- **Template Engine**: Thymeleaf (server-side rendering)

### Frontend Stack
- **CSS Framework**: Bootstrap 5
- **Icons**: Font Awesome 6
- **JavaScript**: Minimal vanilla JS (Bootstrap components)

### Architecture Pattern
**MVC (Model-View-Controller)**:
```
┌─────────────┐
│  Templates  │ ← View Layer (Thymeleaf)
└──────┬──────┘
       │
┌──────▼──────┐
│ Controllers │ ← Web Layer (Spring MVC)
└──────┬──────┘
       │
┌──────▼──────┐
│  Services   │ ← Business Logic Layer
└──────┬──────┘
       │
┌──────▼──────┐
│ Repositories│ ← Data Access Layer (Spring Data JPA)
└──────┬──────┘
       │
┌──────▼──────┐
│   SQLite    │ ← Database (library.db)
└─────────────┘
```

### Package Structure
```
com.library.system/
├── config/              # Spring Security, UserDetailsService
├── controller/          # REST/Web controllers
│   ├── AuthController.java
│   ├── BookController.java
│   ├── HomeController.java
│   ├── LoanController.java
│   └── ReservationController.java
├── domain/              # JPA Entities (Data Model)
│   ├── User.java
│   ├── Book.java
│   ├── Loan.java
│   └── Reservation.java
├── repository/          # Spring Data JPA Repositories
│   ├── UserRepository.java
│   ├── BookRepository.java
│   ├── LoanRepository.java
│   └── ReservationRepository.java
└── service/             # Business Logic
    ├── UserService.java
    ├── BookService.java
    ├── LoanService.java
    ├── ReservationService.java
    └── DataInitializationService.java
```

---

## Database Model (JPA-Based)

### Important: JPA vs JDBC

**This project uses JPA (Java Persistence API) with Hibernate as the ORM, NOT JDBC.**

**How JPA is Used**:
1. **Entity Classes**: Domain models annotated with `@Entity`, `@Table`, `@Column`
2. **Repositories**: Extend `JpaRepository<T, ID>` for automatic CRUD operations
3. **JPQL Queries**: Use `@Query` annotations with object-oriented queries
4. **Automatic DDL**: Hibernate generates database tables from entity classes
5. **Relationship Mapping**: `@ManyToOne`, `@OneToMany` for associations
6. **Transaction Management**: `@Transactional` for atomic operations

**Example JPA Entity**:
```java
@Entity
@Table(name = "books")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String isbn;

    @OneToMany(mappedBy = "book")
    private List<Loan> loans;
}
```

**Example JPA Repository**:
```java
public interface BookRepository extends JpaRepository<Book, Long> {
    @Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Book> searchByTitle(@Param("keyword") String keyword);
}
```

### Entity Relationships

#### **User Entity** (`domain/User.java`)
```java
@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; // BCrypt hashed

    private String fullName;
    private String email;

    @Enumerated(EnumType.STRING)
    private UserRole role; // ADMIN, MEMBER

    @OneToMany(mappedBy = "user")
    private List<Loan> loans;

    @OneToMany(mappedBy = "user")
    private List<Reservation> reservations;
}
```

**Key Points**:
- BCrypt password encoding (configured in SecurityConfig)
- Role-based access control (ADMIN vs MEMBER)
- One-to-Many relationship with Loans and Reservations

#### **Book Entity** (`domain/Book.java`)
```java
@Entity
@Table(name = "books")
public class Book {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String isbn;

    private String title;
    private String author;
    private String publisher;
    private LocalDate publicationDate;
    private String description;

    @Enumerated(EnumType.STRING)
    private BookStatus status; // AVAILABLE, BORROWED

    @OneToMany(mappedBy = "book")
    private List<Loan> loans;

    @OneToMany(mappedBy = "book")
    private List<Reservation> reservations;
}
```

**Key Points**:
- ISBN is unique identifier
- Status tracks availability (AVAILABLE/BORROWED)
- Publication date supports advanced search filtering

#### **Loan Entity** (`domain/Loan.java`)
```java
@Entity
@Table(name = "loans")
public class Loan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    private LoanStatus status; // ACTIVE, EXTENDED, RETURNED, OVERDUE
}
```

**Key Points**:
- Tracks borrowing lifecycle (borrow date, due date, return date)
- Status includes EXTENDED for loans that have been extended once
- Many-to-One relationships with User and Book

#### **Reservation Entity** (`domain/Reservation.java`)
```java
@Entity
@Table(name = "reservations")
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "queue_position")
    private Integer queuePosition; // FIFO queue position

    @Enumerated(EnumType.STRING)
    private ReservationStatus status; // PENDING, READY_FOR_PICKUP, FULFILLED, CANCELLED, EXPIRED

    private LocalDateTime createdAt;
    private LocalDateTime notifiedAt;
    private LocalDateTime expiresAt; // 24-hour pickup window
}
```

**Key Points**:
- `queuePosition` enables FIFO queue management
- `expiresAt` enforces 24-hour pickup window
- Status tracks reservation lifecycle

### Database Configuration

**SQLite-Specific Optimizations** (`application.yml`):
```yaml
spring:
  datasource:
    url: jdbc:sqlite:library.db
    driver-class-name: org.sqlite.JDBC
  jpa:
    hibernate:
      ddl-auto: update # Auto-create tables from entities
    properties:
      hibernate:
        dialect: org.hibernate.community.dialect.SQLiteDialect
        jdbc:
          time_zone: UTC
  hikari:
    maximum-pool-size: 1 # SQLite limitation
    connection-init-sql: "PRAGMA journal_mode=WAL; PRAGMA busy_timeout=30000;"
```

**Why single connection pool?**
- SQLite doesn't support true concurrent writes
- WAL mode improves concurrency for reads
- Single connection prevents lock conflicts

---

## Core Functionalities

### 1. User Authentication & Authorization

**Flow**: Login → CustomUserDetailsService → Spring Security → Session

**Files Involved**:
- `config/SecurityConfig.java` - Security configuration
- `config/CustomUserDetailsService.java` - User loading for authentication
- `service/UserService.java` - User business logic
- `controller/AuthController.java` - Login/logout endpoints

**How It Works**:

1. **Password Encoding** (`SecurityConfig.java`):
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

2. **Custom User Details** (`config/CustomUserDetailsService.java`):
```java
@Override
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    return org.springframework.security.core.userdetails.User
        .withUsername(user.getUsername())
        .password(user.getPassword())
        .roles(user.getRole().name())
        .build();
}
```

3. **Authorization Rules** (`SecurityConfig.java`):
```java
http.authorizeHttpRequests(authorize -> authorize
    .requestMatchers("/", "/search", "/books/**", "/css/**").permitAll()
    .requestMatchers("/loans/**", "/reservations/**").authenticated()
    .requestMatchers("/admin/**").hasRole("ADMIN")
    .anyRequest().authenticated()
);
```

**Demo Accounts** (from DataInitializationService):
- `admin / admin123` (ADMIN role)
- `john.doe / password123` (MEMBER role)
- `jane.smith / password123` (MEMBER role)

### 2. Book Search & Catalog

**Simple Search** (Home Page):
- Keyword search across title, author, ISBN
- Case-insensitive matching
- Returns all matching books

**Advanced Search** (Advanced Search Page):
- Filter by title, author, ISBN, publication year
- Publication year supports range filtering
- Combines multiple criteria with AND logic

**Implementation** (`repository/BookRepository.java`):
```java
// Simple search
@Query("SELECT b FROM Book b WHERE " +
       "LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
       "LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
       "LOWER(b.isbn) LIKE LOWER(CONCAT('%', :keyword, '%'))")
List<Book> searchByKeyword(@Param("keyword") String keyword);

// Advanced search with publication year
@Query("SELECT b FROM Book b WHERE " +
       "(:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
       "(:author IS NULL OR LOWER(b.author) LIKE LOWER(CONCAT('%', :author, '%'))) AND " +
       "(:isbn IS NULL OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :isbn, '%'))) AND " +
       "(:startDate IS NULL OR b.publicationDate >= :startDate) AND " +
       "(:endDate IS NULL OR b.publicationDate <= :endDate)")
List<Book> advancedSearch(@Param("title") String title,
                         @Param("author") String author,
                         @Param("isbn") String isbn,
                         @Param("startDate") LocalDate startDate,
                         @Param("endDate") LocalDate endDate);
```

**Publication Year Filtering** (`controller/HomeController.java`):
```java
// Convert year to LocalDate range for SQLite compatibility
Integer publicationYear = searchCriteria.getPublicationYear();
LocalDate startDate = null;
LocalDate endDate = null;
if (publicationYear != null) {
    startDate = LocalDate.of(publicationYear, 1, 1);
    endDate = LocalDate.of(publicationYear, 12, 31);
}
```

**Why LocalDate instead of YEAR()?**
- SQLite doesn't support YEAR() function
- Solution: Convert year to date range (Jan 1 - Dec 31)
- Repository method filters using `publicationDate >= startDate AND publicationDate <= endDate`

### 3. Borrowing Books

**Business Rules**:
- User must be authenticated
- Book must be AVAILABLE
- User cannot have >5 active loans
- User cannot borrow same book twice
- Book status changes to BORROWED
- Loan period is 14 days from borrow date

**Flow**: Book Details Page → Borrow Button → LoanService → Database Update

**Implementation** (`service/LoanService.java`):
```java
@Transactional
public Loan borrowBook(Long userId, Long bookId) {
    // 1. Fetch user and book
    User user = userService.getUserById(userId)
        .orElseThrow(() -> new RuntimeException("User not found"));
    Book book = bookService.getBookById(bookId)
        .orElseThrow(() -> new RuntimeException("Book not found"));

    // 2. Validate book availability
    if (book.getStatus() != BookStatus.AVAILABLE) {
        throw new RuntimeException("Book is not available");
    }

    // 3. Check if user already has this book
    Optional<Loan> existingLoan = loanRepository
        .findByUserAndBookAndStatus(user, book, LoanStatus.ACTIVE);
    if (existingLoan.isPresent()) {
        throw new RuntimeException("You already have this book borrowed");
    }

    // 4. Check max loans limit
    long activeLoanCount = loanRepository.countActiveLoansByUser(user);
    if (activeLoanCount >= MAX_LOANS_PER_USER) {
        throw new RuntimeException("Maximum loan limit reached");
    }

    // 5. Create loan
    Loan loan = new Loan();
    loan.setUser(user);
    loan.setBook(book);
    loan.setBorrowDate(LocalDate.now());
    loan.setDueDate(LocalDate.now().plusDays(LOAN_PERIOD_DAYS)); // 14 days
    loan.setStatus(LoanStatus.ACTIVE);

    // 6. Update book status
    book.setStatus(BookStatus.BORROWED);
    bookService.saveBook(book);

    return loanRepository.save(loan);
}
```

**SQLite Concurrency Handling**:
```java
// Retry logic for SQLite WAL conflicts
int retryCount = 0;
while (retryCount < 3) {
    try {
        return loanRepository.save(loan);
    } catch (Exception e) {
        if (e.getMessage().contains("SQLITE_BUSY")) {
            retryCount++;
            Thread.sleep(100 * retryCount); // Exponential backoff
        } else {
            throw e;
        }
    }
}
```

### 4. Returning Books

**Business Rules**:
- Loan must exist and be ACTIVE or EXTENDED
- Book status changes to AVAILABLE
- Return date is recorded
- If book has reservations, first person in queue is promoted

**Flow**: My Loans Page → Return Button → LoanService → Queue Promotion (if applicable)

**Implementation** (`service/LoanService.java`):
```java
@Transactional
public Loan returnBook(Long loanId) {
    // 1. Fetch loan
    Loan loan = loanRepository.findById(loanId)
        .orElseThrow(() -> new RuntimeException("Loan not found"));

    // 2. Validate loan status
    if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.EXTENDED) {
        throw new RuntimeException("Loan is not active");
    }

    // 3. Update loan
    loan.setStatus(LoanStatus.RETURNED);
    loan.setReturnDate(LocalDate.now());

    // 4. Check for reservations and promote queue
    Book book = loan.getBook();
    if (reservationService.hasActiveReservations(book)) {
        reservationService.promoteQueue(book); // Promotes first in queue to READY_FOR_PICKUP
        book.setStatus(BookStatus.AVAILABLE); // Book is available but reserved
    } else {
        book.setStatus(BookStatus.AVAILABLE);
    }

    bookService.saveBook(book);
    return loanRepository.save(loan);
}
```

### 5. Extending Loans

**Business Rules**:
- Loan must be ACTIVE or EXTENDED
- Loan must not be overdue
- Book must NOT have active reservations (critical rule!)
- Extension adds 14 days to due date
- Status changes to EXTENDED

**Flow**: My Loans Page → Extend Button → LoanService → Validation

**Implementation** (`service/LoanService.java`):
```java
@Transactional
public Loan extendLoan(Long loanId) {
    // 1. Fetch loan
    Loan loan = loanRepository.findById(loanId)
        .orElseThrow(() -> new RuntimeException("Loan not found"));

    // 2. Validate loan status
    if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.EXTENDED) {
        throw new RuntimeException("Loan cannot be extended");
    }

    // 3. Check for overdue
    if (loan.getDueDate().isBefore(LocalDate.now())) {
        throw new RuntimeException("Cannot extend overdue loan");
    }

    // 4. CRITICAL: Check for reservations
    Book book = loan.getBook();
    if (reservationService.hasActiveReservations(book)) {
        throw new RuntimeException("Cannot extend loan - book has active reservations");
    }

    // 5. Extend loan
    loan.setDueDate(loan.getDueDate().plusDays(LOAN_PERIOD_DAYS)); // Add 14 days
    loan.setStatus(LoanStatus.EXTENDED);

    return loanRepository.save(loan);
}
```

**Why Block Extension with Reservations?**
- Ensures fairness: reserved books should be returned ASAP
- Prevents indefinite borrowing when others are waiting
- Encourages timely returns

### 6. Reservation System (FIFO Queue)

**Business Rules**:
- Only BORROWED books can be reserved
- Borrower cannot reserve their own borrowed book
- User cannot have duplicate reservation for same book
- Reservations are queued in FIFO order (queuePosition field)
- First person in queue gets 24-hour pickup window when book is returned
- If pickup expires, next person in queue is promoted

**Flow**: Book Details → Reserve Button → Queue Assignment → Return Triggers Promotion

#### **Reserving a Book** (`service/ReservationService.java`):
```java
@Transactional
public Reservation reserveBook(User user, Book book) {
    // 1. Check for duplicate reservation
    Optional<Reservation> existingReservation = reservationRepository
        .findActiveReservationByBookAndUser(book, user);
    if (existingReservation.isPresent()) {
        throw new IllegalStateException("You already have an active reservation for this book");
    }

    // 2. Calculate queue position (FIFO)
    long pendingCount = reservationRepository.countPendingReservationsByBook(book);
    int queuePosition = (int) (pendingCount + 1);

    // 3. Create reservation
    Reservation reservation = new Reservation(user, book, queuePosition);
    reservation.setStatus(ReservationStatus.PENDING);
    reservation.setCreatedAt(LocalDateTime.now());

    return reservationRepository.save(reservation);
}
```

#### **Promoting Queue** (`service/ReservationService.java`):
```java
@Transactional
public void promoteQueue(Book book) {
    // Get all pending reservations ordered by queue position
    List<Reservation> pendingReservations = reservationRepository
        .findPendingReservationsByBook(book);

    if (!pendingReservations.isEmpty()) {
        // Promote first person in queue
        Reservation firstInQueue = pendingReservations.get(0);
        firstInQueue.setStatus(ReservationStatus.READY_FOR_PICKUP);
        firstInQueue.setNotifiedAt(LocalDateTime.now());
        firstInQueue.setExpiresAt(LocalDateTime.now().plusHours(24)); // 24-hour window

        reservationRepository.save(firstInQueue);
    }
}
```

**Repository Query** (`repository/ReservationRepository.java`):
```java
@Query("SELECT r FROM Reservation r WHERE r.book = :book AND r.status = 'PENDING' ORDER BY r.queuePosition ASC")
List<Reservation> findPendingReservationsByBook(@Param("book") Book book);
```

#### **Canceling Reservation** (`service/ReservationService.java`):
```java
@Transactional
public void cancelReservation(Long reservationId, User user) {
    // 1. Fetch reservation
    Reservation reservation = reservationRepository.findById(reservationId)
        .orElseThrow(() -> new IllegalStateException("Reservation not found"));

    // 2. Authorization check
    if (!reservation.getUser().getId().equals(user.getId())) {
        throw new IllegalStateException("You can only cancel your own reservations");
    }

    // 3. Cancel reservation
    reservation.setStatus(ReservationStatus.CANCELLED);
    reservationRepository.save(reservation);

    // 4. Reorder queue (decrement positions after cancelled one)
    Book book = reservation.getBook();
    Integer canceledPosition = reservation.getQueuePosition();

    List<Reservation> pendingReservations = reservationRepository
        .findPendingReservationsByBook(book);

    for (Reservation r : pendingReservations) {
        if (r.getQueuePosition() != null && r.getQueuePosition() > canceledPosition) {
            r.setQueuePosition(r.getQueuePosition() - 1);
            reservationRepository.save(r);
        }
    }
}
```

#### **Handling Expired Pickups** (`service/ReservationService.java`):
```java
@Transactional
public void processExpiredReservations() {
    // Find all READY_FOR_PICKUP reservations past expiration
    List<Reservation> expiredReservations = reservationRepository
        .findExpiredReadyReservations(LocalDateTime.now());

    for (Reservation reservation : expiredReservations) {
        // Mark as expired
        reservation.setStatus(ReservationStatus.EXPIRED);
        reservationRepository.save(reservation);

        // Promote next person in queue
        promoteQueue(reservation.getBook());
    }
}
```

**Scheduled Job** (could be added with `@Scheduled`):
```java
@Scheduled(cron = "0 */15 * * * *") // Every 15 minutes
public void checkExpiredReservations() {
    reservationService.processExpiredReservations();
}
```

#### **UI Integration**

**Book Details Page** (`templates/book-details.html`):
```html
<!-- Book is Borrowed -->
<div th:unless="${isAvailable}">
    <!-- Show Reserve Button if user can reserve -->
    <div th:if="${canReserve}">
        <button class="btn btn-secondary btn-lg w-100 mb-2" disabled>
            <i class="fas fa-times"></i> Currently Borrowed
        </button>
        <form th:action="@{/reservations/reserve/{bookId}(bookId=${book.id})}" method="post">
            <button type="submit" class="btn btn-primary btn-lg w-100">
                <i class="fas fa-bookmark"></i> Reserve Book
            </button>
        </form>
        <small class="text-muted d-block">
            Join the waitlist to borrow this book when it becomes available.
        </small>
    </div>

    <!-- Show message if user already has reservation -->
    <div th:if="${hasReservation}">
        <button class="btn btn-info btn-lg w-100 mb-2" disabled>
            <i class="fas fa-check"></i> Already Reserved
        </button>
        <small class="text-muted d-block">
            You already have an active reservation for this book.
        </small>
    </div>
</div>
```

**My Loans Page - Reservations Section** (`templates/my-loans.html`):
```html
<div class="row mb-5">
    <div class="col-12">
        <h3 class="mb-3">My Reservations</h3>
        <table class="table table-striped">
            <thead>
                <tr>
                    <th>Book</th>
                    <th>Reserved On</th>
                    <th>Queue Position</th>
                    <th>Status</th>
                    <th>Pickup Deadline</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="reservation : ${activeReservations}">
                    <td>
                        <a th:href="@{/books/{id}(id=${reservation.book.id})}"
                           th:text="${reservation.book.title}">Book Title</a>
                    </td>
                    <td th:text="${#temporals.format(reservation.createdAt, 'MMM dd, yyyy HH:mm')}">
                        Reserved Date
                    </td>
                    <td>
                        <span th:if="${reservation.status.name() == 'PENDING'}" class="badge bg-info">
                            #<span th:text="${reservation.queuePosition}">1</span> in queue
                        </span>
                        <span th:if="${reservation.status.name() == 'READY_FOR_PICKUP'}"
                              class="badge bg-success">
                            Ready for pickup!
                        </span>
                    </td>
                    <td>
                        <span class="badge"
                              th:classappend="${reservation.status.name() == 'PENDING'} ? 'bg-warning' :
                                             (${reservation.status.name() == 'READY_FOR_PICKUP'} ? 'bg-success' : 'bg-secondary')"
                              th:text="${reservation.status.name()}">Status</span>
                    </td>
                    <td>
                        <span th:if="${reservation.expiresAt != null}">
                            <span th:text="${#temporals.format(reservation.expiresAt, 'MMM dd, yyyy HH:mm')}">
                                Expires At
                            </span>
                            <span th:if="${reservation.expiresAt.isBefore(T(java.time.LocalDateTime).now())}"
                                  class="badge bg-danger ms-2">Expired</span>
                        </span>
                        <span th:unless="${reservation.expiresAt != null}">-</span>
                    </td>
                    <td>
                        <form th:action="@{/reservations/cancel/{id}(id=${reservation.id})}"
                              method="post" class="d-inline">
                            <button type="submit" class="btn btn-sm btn-outline-danger">
                                <i class="fas fa-times"></i> Cancel
                            </button>
                        </form>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</div>
```

**Controller Logic** (`controller/BookController.java`):
```java
@GetMapping("/{id}")
public String bookDetails(@PathVariable Long id, Model model) {
    Book book = bookService.getBookById(id)
        .orElseThrow(() -> new RuntimeException("Book not found"));

    // Get current user
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    User currentUser = null;
    if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
        currentUser = userService.getUserByUsername(auth.getName()).orElse(null);
    }

    boolean isAvailable = book.getStatus() == BookStatus.AVAILABLE;

    // Check if book is borrowed and determine if user can reserve
    if (book.getStatus() == BookStatus.BORROWED && currentUser != null) {
        // Check if current user is the borrower
        List<Loan> activeLoans = loanService.getActiveLoansByUser(currentUser);
        boolean isBorrower = activeLoans.stream()
            .anyMatch(loan -> loan.getBook().getId().equals(book.getId()));

        // Check if user already has a reservation
        boolean hasReservation = reservationService.getUserReservations(currentUser).stream()
            .anyMatch(reservation -> reservation.getBook().getId().equals(book.getId()));

        model.addAttribute("canReserve", !isBorrower && !hasReservation);
        model.addAttribute("hasReservation", hasReservation);
    }

    model.addAttribute("book", book);
    model.addAttribute("user", currentUser);
    model.addAttribute("isAvailable", isAvailable);

    return "book-details";
}
```

---

## Frontend Templates

### Template Structure
```
src/main/resources/templates/
├── index.html              # Home page with search
├── search.html            # Advanced search page
├── book-details.html      # Book details with borrow/reserve
├── my-loans.html          # User's loans and reservations
├── login.html             # Login form
└── error.html             # Error page
```

### Common Template Features

**Thymeleaf Expressions**:
```html
<!-- Variable expressions -->
<span th:text="${book.title}">Default Text</span>

<!-- URL expressions -->
<a th:href="@{/books/{id}(id=${book.id})}">Book Link</a>

<!-- Conditional rendering -->
<div th:if="${user != null}">Logged in content</div>
<div th:unless="${user != null}">Guest content</div>

<!-- Iteration -->
<tr th:each="book : ${books}">
    <td th:text="${book.title}">Title</td>
</tr>

<!-- Date formatting -->
<span th:text="${#temporals.format(loan.dueDate, 'MMM dd, yyyy')}">Date</span>

<!-- Enum comparison -->
<span th:if="${book.status.name() == 'AVAILABLE'}" class="badge bg-success">Available</span>
```

**Bootstrap Components**:
- Navbar with authentication state
- Cards for book details
- Tables for loans/reservations
- Alerts for flash messages
- Forms with CSRF protection

**Flash Messages** (RedirectAttributes):
```java
// Controller
redirectAttributes.addFlashAttribute("success", "Book borrowed successfully!");
redirectAttributes.addFlashAttribute("error", "Book not available");

// Template
<div th:if="${success}" class="alert alert-success">
    <span th:text="${success}"></span>
</div>
```

---

## Testing Strategy

### Test Structure
```
src/test/java/com/library/system/service/
├── LoanServiceTest.java
├── ReservationServiceTest.java
├── BookServiceTest.java
└── UserServiceTest.java
```

### Testing Approach

**Framework**: JUnit 5 + Mockito + Spring Test

**Pattern**: AAA (Arrange-Act-Assert)
```java
@Test
void borrowBook_Success() {
    // Arrange (Given)
    when(userService.getUserById(1L)).thenReturn(Optional.of(testUser));
    when(bookService.getBookById(1L)).thenReturn(Optional.of(testBook));
    when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

    // Act (When)
    Loan result = loanService.borrowBook(1L, 1L);

    // Assert (Then)
    assertNotNull(result);
    assertEquals(testUser, result.getUser());
    verify(loanRepository).save(any(Loan.class));
}
```

### Key Test Examples

#### **LoanServiceTest.java** - Reservation Integration
```java
@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookService bookService;

    @Mock
    private UserService userService;

    @Mock
    private ReservationService reservationService; // CRITICAL: Mock dependency

    @InjectMocks
    private LoanService loanService;

    @Test
    void extendLoan_FailsWhenBookHasReservations() {
        // Given
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(reservationService.hasActiveReservations(testBook)).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> loanService.extendLoan(1L));

        assertEquals("Cannot extend loan - book has active reservations", exception.getMessage());
        verify(reservationService).hasActiveReservations(testBook);
        verify(loanRepository, never()).save(any());
    }

    @Test
    void returnBook_WithReservations_PromotesQueue() {
        // Given
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);
        when(bookService.saveBook(any(Book.class))).thenReturn(testBook);
        when(reservationService.hasActiveReservations(testBook)).thenReturn(true);

        // When
        Loan result = loanService.returnBook(1L);

        // Then
        assertNotNull(result);
        assertEquals(LoanStatus.RETURNED, result.getStatus());
        verify(reservationService).hasActiveReservations(testBook);
        verify(reservationService).promoteQueue(testBook); // CRITICAL: Queue promotion
    }
}
```

#### **ReservationServiceTest.java** - FIFO Queue Tests
```java
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void reserveBook_MultipleUsers_AssignsCorrectQueuePosition() {
        // Given
        when(reservationRepository.findActiveReservationByBookAndUser(testBook, otherUser))
            .thenReturn(Optional.empty());
        when(reservationRepository.countPendingReservationsByBook(testBook)).thenReturn(2L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation res = invocation.getArgument(0);
            res.setId(3L);
            return res;
        });

        // When
        Reservation result = reservationService.reserveBook(otherUser, testBook);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getQueuePosition()); // 3rd in queue
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void promoteQueue_FirstInQueue_MovesToReadyForPickup() {
        // Given
        Reservation firstReservation = new Reservation(testUser, testBook, 1);
        firstReservation.setStatus(ReservationStatus.PENDING);
        List<Reservation> pendingReservations = new ArrayList<>();
        pendingReservations.add(firstReservation);

        when(reservationRepository.findPendingReservationsByBook(testBook))
            .thenReturn(pendingReservations);
        when(reservationRepository.save(any(Reservation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        reservationService.promoteQueue(testBook);

        // Then
        verify(reservationRepository).save(argThat(reservation ->
            reservation.getStatus() == ReservationStatus.READY_FOR_PICKUP &&
            reservation.getNotifiedAt() != null &&
            reservation.getExpiresAt() != null // 24-hour window
        ));
    }

    @Test
    void cancelReservation_Success_ReordersQueue() {
        // Given
        Reservation firstRes = new Reservation(testUser, testBook, 1);
        firstRes.setId(1L);

        Reservation secondRes = new Reservation(otherUser, testBook, 2);
        secondRes.setId(2L);

        Reservation thirdRes = new Reservation(new User(), testBook, 3);
        thirdRes.setId(3L);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(firstRes));
        when(reservationRepository.findPendingReservationsByBook(testBook))
            .thenReturn(List.of(secondRes, thirdRes));

        // When
        reservationService.cancelReservation(1L, testUser);

        // Then
        verify(reservationRepository).save(argThat(res ->
            res.getId().equals(1L) && res.getStatus() == ReservationStatus.CANCELLED
        ));
        verify(reservationRepository, times(3)).save(any(Reservation.class)); // 1 cancel + 2 reorders
    }
}
```

### Running Tests

**Maven**:
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=LoanServiceTest

# Run with coverage report
mvn jacoco:report
```

**Docker**:
```bash
docker run --rm -v "$(pwd)":/app -w /app maven:3.9-eclipse-temurin-17 mvn clean test
```

---

## DevOps & Deployment

### Docker Configuration

**Multi-stage Dockerfile**:
```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Create data directory for SQLite
RUN mkdir -p /app/data

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Docker Compose** (`docker-compose.yml`):
```yaml
version: '3.8'

services:
  library-app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    volumes:
      - ./data:/app/data # Persist SQLite database
```

### CI/CD Pipeline (GitHub Actions)

**Workflow** (`.github/workflows/ci-cd.yml`):
```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    name: Test
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run tests
        run: mvn clean test

      - name: Upload test results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: test-results
          path: target/surefire-reports/

  build:
    name: Build Docker Image
    needs: test
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Build Docker image
        uses: docker/build-push-action@v5
        with:
          context: .
          push: false
          tags: library-management-system:latest
          platforms: linux/amd64,linux/arm64

  security-scan:
    name: Security Scan
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: library-management-system:latest
          format: 'sarif'
          output: 'trivy-results.sarif'
```

**Pipeline Stages**:
1. **Test** - Run JUnit tests with Maven
2. **Build** - Create multi-architecture Docker image
3. **Security Scan** - Trivy vulnerability scanning
4. **Deploy** - Push to container registry and deploy to Render.com

### Deployment Environments

**Local Development**:
- SQLite database in project root (`library.db`)
- Spring Boot DevTools for hot reload
- H2 console for database inspection

**Docker Development**:
- Persistent volume for SQLite (`./data:/app/data`)
- `SPRING_PROFILES_ACTIVE=docker`
- Port mapping 8080:8080

**Production (Render.com)**:
- Automatic deployment from GitHub
- Environment variables for secrets
- `SPRING_PROFILES_ACTIVE=production`
- Health check endpoint: `/actuator/health`

---

## Summary

This Library Management System demonstrates a complete Spring Boot application with:

1. **JPA-based persistence** with Hibernate ORM (NOT JDBC)
2. **SQLite database** with WAL mode for concurrency
3. **Spring Security** with role-based access control
4. **FIFO reservation queue** with 24-hour pickup windows
5. **Thymeleaf templates** with Bootstrap UI
6. **Comprehensive unit tests** with JUnit 5 and Mockito
7. **CI/CD pipeline** with GitHub Actions
8. **Docker containerization** for deployment

**Key Architectural Decisions**:
- JPA entities instead of raw SQL for maintainability
- Service layer for business logic separation
- Repository pattern for data access abstraction
- Optimistic locking and retry logic for SQLite concurrency
- Server-side rendering with Thymeleaf for simplicity
- Single connection pool to prevent SQLite lock conflicts

**Important Files to Reference**:
- **Loan Rules**: `service/LoanService.java:26-27` (LOAN_PERIOD_DAYS, MAX_LOANS_PER_USER)
- **Queue Logic**: `service/ReservationService.java:reserveBook(), promoteQueue()`
- **Security Config**: `config/SecurityConfig.java`
- **Test Examples**: `src/test/java/com/library/system/service/LoanServiceTest.java`, `ReservationServiceTest.java`
