package com.library.system.service;

import com.library.system.domain.*;
import com.library.system.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LoanService {
    
    @Autowired
    private LoanRepository loanRepository;
    
    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    @Autowired
    private ReservationService reservationService;
    
    private static final int LOAN_PERIOD_DAYS = 14; // 2 weeks
    private static final int MAX_LOANS_PER_USER = 5;
    
    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }
    
    public Optional<Loan> getLoanById(Long id) {
        return loanRepository.findById(id);
    }
    
    public List<Loan> getLoansByUser(User user) {
        return loanRepository.findByUserFetchBook(user);
    }
    
    public List<Loan> getActiveLoansByUser(User user) {
        return loanRepository.findByUserAndStatusFetchBook(user, LoanStatus.ACTIVE);
    }
    
    public Loan borrowBook(Long userId, Long bookId) {
        User user = userService.getUserById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Book book = bookService.getBookById(bookId)
            .orElseThrow(() -> new RuntimeException("Book not found with id: " + bookId));

        // optimistic retry for SQLITE_BUSY (WAL snapshot conflicts)
        int attempts = 0;
        RuntimeException lastException = null;
        while (attempts < 3) {
            try {
                // Check if user already has this book borrowed
                Optional<Loan> existingLoan = loanRepository.findByUserAndBookAndStatus(user, book, LoanStatus.ACTIVE);
                if (existingLoan.isPresent()) {
                    throw new RuntimeException("User already has this book borrowed");
                }

                // Check if book is available
                if (book.getStatus() != BookStatus.AVAILABLE) {
                    throw new RuntimeException("Book is not available for borrowing");
                }

                // Check user's loan limit
                long activeLoanCount = loanRepository.countActiveLoansByUser(user);
                if (activeLoanCount >= MAX_LOANS_PER_USER) {
                    throw new RuntimeException("User has reached maximum loan limit");
                }

                // Create loan
                LocalDate borrowDate = LocalDate.now();
                LocalDate dueDate = borrowDate.plusDays(LOAN_PERIOD_DAYS);

                Loan loan = new Loan(user, book, borrowDate, dueDate);
                loan = loanRepository.save(loan);

                // Update book status
                book.setStatus(BookStatus.BORROWED);
                bookService.saveBook(book);

                return loan;
            } catch (RuntimeException ex) {
                lastException = ex;
                // If this is a transient SQLITE_BUSY/SNAPSHOT, backoff and retry
                String msg = ex.getMessage() != null ? ex.getMessage() : "";
                if (msg.contains("SQLITE_BUSY") || msg.contains("SNAPSHOT") || msg.contains("database is locked")) {
                    try { Thread.sleep(200L * (attempts + 1)); } catch (InterruptedException ignored) {}
                    attempts++;
                } else {
                    throw ex;
                }
            }
        }
        throw lastException != null ? lastException : new RuntimeException("Borrow failed due to database contention");
    }
    
    public Loan returnBook(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found with id: " + loanId));

        if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.EXTENDED) {
            throw new RuntimeException("Loan is not active");
        }

        // Update loan
        loan.setReturnDate(LocalDate.now());
        loan.setStatus(LoanStatus.RETURNED);
        loan.setUpdatedAt(java.time.LocalDateTime.now());
        loan.setReminderSentAt(null);
        loan = loanRepository.save(loan);

        // Update book status and handle reservations
        Book book = loan.getBook();

        // Check if there are pending reservations
        if (reservationService.hasActiveReservations(book)) {
            // Promote the first person in queue to READY_FOR_PICKUP
            reservationService.promoteQueue(book);
            // Book stays AVAILABLE but first person has 24 hours to pick it up
            book.setStatus(BookStatus.AVAILABLE);
        } else {
            // No reservations, book is simply available
            book.setStatus(BookStatus.AVAILABLE);
        }

        bookService.saveBook(book);

        return loan;
    }
    
    public Loan extendLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found with id: " + loanId));

        if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.EXTENDED) {
            throw new RuntimeException("Loan is not active");
        }

        // Check if loan is overdue
        if (loan.getDueDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Cannot extend overdue loan");
        }

        // Check if book has reservations - if yes, cannot extend
        Book book = loan.getBook();
        if (reservationService.hasActiveReservations(book)) {
            throw new RuntimeException("Cannot extend loan - book has active reservations");
        }

        // Extend loan by another period
        LocalDate newDueDate = loan.getDueDate().plusDays(LOAN_PERIOD_DAYS);
        loan.setDueDate(newDueDate);
        loan.setStatus(LoanStatus.EXTENDED);
        loan.setUpdatedAt(java.time.LocalDateTime.now());
        loan.setReminderSentAt(null);

        return loanRepository.save(loan);
    }
    
    public List<Loan> getOverdueLoans() {
        return loanRepository.findOverdueLoans(LocalDate.now());
    }
    
    public List<Loan> getLoansDueSoon(int days) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days);
        return loanRepository.findLoansDueBetween(startDate, endDate);
    }
}
