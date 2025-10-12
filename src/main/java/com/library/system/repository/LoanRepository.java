package com.library.system.repository;

import com.library.system.domain.Loan;
import com.library.system.domain.LoanStatus;
import com.library.system.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    
    List<Loan> findByUser(User user);
    
    List<Loan> findByUserAndStatus(User user, LoanStatus status);
    
    Optional<Loan> findByUserAndBookAndStatus(User user, com.library.system.domain.Book book, LoanStatus status);
    
    @Query("SELECT l FROM Loan l WHERE l.dueDate < :date AND l.status = 'ACTIVE'")
    List<Loan> findOverdueLoans(@Param("date") LocalDate date);
    
    @Query("SELECT l FROM Loan l WHERE l.dueDate BETWEEN :startDate AND :endDate AND l.status = 'ACTIVE'")
    List<Loan> findLoansDueBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.user = :user AND l.status = 'ACTIVE'")
    long countActiveLoansByUser(@Param("user") User user);
}
