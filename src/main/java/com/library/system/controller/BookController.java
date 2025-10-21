package com.library.system.controller;

import com.library.system.domain.Book;
import com.library.system.domain.BookStatus;
import com.library.system.domain.Loan;
import com.library.system.domain.User;
import com.library.system.service.BookService;
import com.library.system.service.LoanService;
import com.library.system.service.ReservationService;
import com.library.system.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/books")
public class BookController {
    
    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    @Autowired
    private LoanService loanService;

    @Autowired
    private ReservationService reservationService;
    
    @GetMapping("/{id}")
    public String bookDetails(@PathVariable Long id, Model model) {
        // Get book
        Optional<Book> bookOpt = bookService.getBookById(id);
        if (bookOpt.isEmpty()) {
            return "redirect:/?error=Book not found";
        }

        Book book = bookOpt.get();
        model.addAttribute("book", book);
        model.addAttribute("isAvailable", bookService.isBookAvailable(id));

        // Add current user if authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = null;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            Optional<User> userOpt = userService.getUserByUsername(authentication.getName());
            if (userOpt.isPresent()) {
                currentUser = userOpt.get();
                model.addAttribute("user", currentUser);
            }
        }

        // Check if book is borrowed and determine if user can reserve
        if (book.getStatus() == BookStatus.BORROWED && currentUser != null) {
            // Check if current user is the borrower
            List<Loan> activeLoans = loanService.getActiveLoansByUser(currentUser);
            boolean isBorrower = activeLoans.stream()
                .anyMatch(loan -> loan.getBook().getId().equals(book.getId()));

            // Check if user already has a reservation
            boolean hasReservation = reservationService.getUserReservations(currentUser).stream()
                .anyMatch(reservation -> reservation.getBook().getId().equals(book.getId()));

            // User can reserve if they are not the borrower and don't already have a reservation
            model.addAttribute("canReserve", !isBorrower && !hasReservation);
            model.addAttribute("hasReservation", hasReservation);
        } else {
            model.addAttribute("canReserve", false);
            model.addAttribute("hasReservation", false);
        }

        // Get reservation count for this book
        long reservationCount = reservationService.hasActiveReservations(book) ?
            reservationService.getUserReservations(currentUser != null ? currentUser : new User()).size() : 0;
        model.addAttribute("reservationCount", reservationCount);

        return "book-details";
    }
    
    @GetMapping("/isbn/{isbn}")
    public String bookDetailsByIsbn(@PathVariable String isbn, Model model) {
        // Add current user if authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            userService.getUserByUsername(authentication.getName()).ifPresent(user -> model.addAttribute("user", user));
        }
        Optional<Book> book = bookService.getBookByIsbn(isbn);
        if (book.isPresent()) {
            model.addAttribute("book", book.get());
            model.addAttribute("isAvailable", bookService.isBookAvailable(book.get().getId()));
            return "book-details";
        } else {
            return "redirect:/?error=Book not found";
        }
    }
}
