package com.library.system.controller;

import com.library.system.domain.Book;
import com.library.system.domain.Reservation;
import com.library.system.domain.User;
import com.library.system.service.BookService;
import com.library.system.service.ReservationService;
import com.library.system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    /**
     * Reserve a book
     * @param bookId The ID of the book to reserve
     * @param redirectAttributes For flash messages
     * @return Redirect to book details page
     */
    @PostMapping("/reserve/{bookId}")
    public String reserveBook(@PathVariable Long bookId, RedirectAttributes redirectAttributes) {
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
                redirectAttributes.addFlashAttribute("error", "You must be logged in to reserve a book");
                return "redirect:/auth/login";
            }

            User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Get the book
            Book book = bookService.getBookById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

            // Create the reservation
            Reservation reservation = reservationService.reserveBook(user, book);

            redirectAttributes.addFlashAttribute("success",
                "Book reserved successfully! You are #" + reservation.getQueuePosition() + " in the queue.");

            return "redirect:/books/" + bookId;

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/books/" + bookId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to reserve book: " + e.getMessage());
            return "redirect:/books/" + bookId;
        }
    }

    /**
     * Cancel a reservation
     * @param reservationId The ID of the reservation to cancel
     * @param redirectAttributes For flash messages
     * @return Redirect to my-loans page
     */
    @PostMapping("/cancel/{reservationId}")
    public String cancelReservation(@PathVariable Long reservationId, RedirectAttributes redirectAttributes) {
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
                redirectAttributes.addFlashAttribute("error", "You must be logged in");
                return "redirect:/auth/login";
            }

            User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Cancel the reservation
            reservationService.cancelReservation(reservationId, user);

            redirectAttributes.addFlashAttribute("success", "Reservation cancelled successfully");

            return "redirect:/loans/my-loans";

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/loans/my-loans";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to cancel reservation: " + e.getMessage());
            return "redirect:/loans/my-loans";
        }
    }
}
