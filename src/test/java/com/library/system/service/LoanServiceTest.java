package com.library.system.service;

import com.library.system.domain.*;
import com.library.system.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookService bookService;

    @Mock
    private UserService userService;

    @InjectMocks
    private LoanService loanService;

    private User testUser;
    private Book testBook;
    private Loan testLoan;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(UserRole.MEMBER);

        testBook = new Book();
        testBook.setId(1L);
        testBook.setIsbn("978-1234567890");
        testBook.setTitle("Test Book");
        testBook.setAuthor("Test Author");
        testBook.setStatus(BookStatus.AVAILABLE);

        testLoan = new Loan();
        testLoan.setId(1L);
        testLoan.setUser(testUser);
        testLoan.setBook(testBook);
        testLoan.setBorrowDate(LocalDate.now());
        testLoan.setDueDate(LocalDate.now().plusDays(14));
        testLoan.setStatus(LoanStatus.ACTIVE);
    }

    @Test
    void borrowBook_Success() {
        // Given
        when(userService.getUserById(1L)).thenReturn(Optional.of(testUser));
        when(bookService.getBookById(1L)).thenReturn(Optional.of(testBook));
        when(loanRepository.findByUserAndBookAndStatus(testUser, testBook, LoanStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(loanRepository.countActiveLoansByUser(testUser)).thenReturn(0L);
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);
        when(bookService.saveBook(any(Book.class))).thenReturn(testBook);

        // When
        Loan result = loanService.borrowBook(1L, 1L);

        // Then
        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertEquals(testBook, result.getBook());
        assertEquals(LoanStatus.ACTIVE, result.getStatus());
        verify(loanRepository).save(any(Loan.class));
        verify(bookService).saveBook(any(Book.class));
    }

    @Test
    void borrowBook_UserNotFound() {
        // Given
        when(userService.getUserById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> loanService.borrowBook(1L, 1L));
    }

    @Test
    void borrowBook_BookNotFound() {
        // Given
        when(userService.getUserById(1L)).thenReturn(Optional.of(testUser));
        when(bookService.getBookById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> loanService.borrowBook(1L, 1L));
    }

    @Test
    void borrowBook_BookNotAvailable() {
        // Given
        testBook.setStatus(BookStatus.BORROWED);
        when(userService.getUserById(1L)).thenReturn(Optional.of(testUser));
        when(bookService.getBookById(1L)).thenReturn(Optional.of(testBook));

        // When & Then
        assertThrows(RuntimeException.class, () -> loanService.borrowBook(1L, 1L));
    }

    @Test
    void borrowBook_UserAlreadyHasBook() {
        // Given
        when(userService.getUserById(1L)).thenReturn(Optional.of(testUser));
        when(bookService.getBookById(1L)).thenReturn(Optional.of(testBook));
        when(loanRepository.findByUserAndBookAndStatus(testUser, testBook, LoanStatus.ACTIVE))
            .thenReturn(Optional.of(testLoan));

        // When & Then
        assertThrows(RuntimeException.class, () -> loanService.borrowBook(1L, 1L));
    }

    @Test
    void borrowBook_MaxLoansReached() {
        // Given
        when(userService.getUserById(1L)).thenReturn(Optional.of(testUser));
        when(bookService.getBookById(1L)).thenReturn(Optional.of(testBook));
        when(loanRepository.findByUserAndBookAndStatus(testUser, testBook, LoanStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(loanRepository.countActiveLoansByUser(testUser)).thenReturn(5L);

        // When & Then
        assertThrows(RuntimeException.class, () -> loanService.borrowBook(1L, 1L));
    }

    @Test
    void returnBook_Success() {
        // Given
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);
        when(bookService.saveBook(any(Book.class))).thenReturn(testBook);

        // When
        Loan result = loanService.returnBook(1L);

        // Then
        assertNotNull(result);
        assertEquals(LoanStatus.RETURNED, result.getStatus());
        assertNotNull(result.getReturnDate());
        verify(loanRepository).save(any(Loan.class));
        verify(bookService).saveBook(any(Book.class));
    }

    @Test
    void returnBook_LoanNotFound() {
        // Given
        when(loanRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> loanService.returnBook(1L));
    }

    @Test
    void returnBook_LoanNotActive() {
        // Given
        testLoan.setStatus(LoanStatus.RETURNED);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

        // When & Then
        assertThrows(RuntimeException.class, () -> loanService.returnBook(1L));
    }

    @Test
    void extendLoan_Success() {
        // Given
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

        // When
        Loan result = loanService.extendLoan(1L);

        // Then
        assertNotNull(result);
        assertEquals(LoanStatus.EXTENDED, result.getStatus());
        assertTrue(result.getDueDate().isAfter(LocalDate.now().plusDays(14)));
        verify(loanRepository).save(any(Loan.class));
    }

    @Test
    void extendLoan_LoanNotFound() {
        // Given
        when(loanRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> loanService.extendLoan(1L));
    }

    @Test
    void extendLoan_LoanNotActive() {
        // Given
        testLoan.setStatus(LoanStatus.RETURNED);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

        // When & Then
        assertThrows(RuntimeException.class, () -> loanService.extendLoan(1L));
    }

    @Test
    void extendLoan_OverdueLoan() {
        // Given
        testLoan.setDueDate(LocalDate.now().minusDays(1));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

        // When & Then
        assertThrows(RuntimeException.class, () -> loanService.extendLoan(1L));
    }
}
