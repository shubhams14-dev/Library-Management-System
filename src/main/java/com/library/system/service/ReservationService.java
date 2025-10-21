package com.library.system.service;

import com.library.system.domain.Book;
import com.library.system.domain.Reservation;
import com.library.system.domain.ReservationStatus;
import com.library.system.domain.User;
import com.library.system.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReservationService {

    private static final int PICKUP_WINDOW_HOURS = 24;

    @Autowired
    private ReservationRepository reservationRepository;

    /**
     * Reserve a book for a user. Adds them to the end of the queue.
     * @param user The user making the reservation
     * @param book The book to reserve
     * @return The created reservation
     * @throws IllegalStateException if user already has an active reservation for this book
     */
    @Transactional
    public Reservation reserveBook(User user, Book book) {
        // Check if user already has an active reservation for this book
        Optional<Reservation> existingReservation = reservationRepository
            .findActiveReservationByBookAndUser(book, user);

        if (existingReservation.isPresent()) {
            throw new IllegalStateException("You already have an active reservation for this book");
        }

        // Get the next queue position (count of pending reservations + 1)
        long pendingCount = reservationRepository.countPendingReservationsByBook(book);
        int queuePosition = (int) (pendingCount + 1);

        // Create the reservation
        Reservation reservation = new Reservation(user, book, queuePosition);
        reservation.setStatus(ReservationStatus.PENDING);

        return reservationRepository.save(reservation);
    }

    /**
     * Check if a book has any active reservations (PENDING or READY_FOR_PICKUP)
     * @param book The book to check
     * @return true if there are active reservations
     */
    public boolean hasActiveReservations(Book book) {
        long count = reservationRepository.countPendingReservationsByBook(book);
        return count > 0;
    }

    /**
     * Promote the first person in queue to READY_FOR_PICKUP status with 24-hour window
     * @param book The book that was just returned
     */
    @Transactional
    public void promoteQueue(Book book) {
        List<Reservation> pendingReservations = reservationRepository
            .findPendingReservationsByBook(book);

        if (!pendingReservations.isEmpty()) {
            // Get the first in queue (lowest queue position)
            Reservation firstInQueue = pendingReservations.get(0);

            // Set status to READY_FOR_PICKUP and set expiration time
            firstInQueue.setStatus(ReservationStatus.READY_FOR_PICKUP);
            firstInQueue.setNotifiedAt(LocalDateTime.now());
            firstInQueue.setExpiresAt(LocalDateTime.now().plusHours(PICKUP_WINDOW_HOURS));

            reservationRepository.save(firstInQueue);
        }
    }

    /**
     * Cancel a reservation and reorder the queue
     * @param reservationId The ID of the reservation to cancel
     * @param user The user requesting cancellation (must be the owner)
     * @throws IllegalStateException if reservation doesn't exist or user is not the owner
     */
    @Transactional
    public void cancelReservation(Long reservationId, User user) {
        Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);

        if (reservationOpt.isEmpty()) {
            throw new IllegalStateException("Reservation not found");
        }

        Reservation reservation = reservationOpt.get();

        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("You can only cancel your own reservations");
        }

        Book book = reservation.getBook();
        Integer canceledPosition = reservation.getQueuePosition();

        // Set status to CANCELLED
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        // Reorder queue - decrease position for all reservations after the cancelled one
        if (canceledPosition != null) {
            List<Reservation> pendingReservations = reservationRepository
                .findPendingReservationsByBook(book);

            for (Reservation r : pendingReservations) {
                if (r.getQueuePosition() != null && r.getQueuePosition() > canceledPosition) {
                    r.setQueuePosition(r.getQueuePosition() - 1);
                    reservationRepository.save(r);
                }
            }
        }
    }

    /**
     * Get all active reservations for a user (PENDING and READY_FOR_PICKUP)
     * @param user The user
     * @return List of active reservations
     */
    public List<Reservation> getUserReservations(User user) {
        return reservationRepository.findActiveReservationsByUser(user);
    }

    /**
     * Get all reservations for a user (all statuses)
     * @param user The user
     * @return List of all reservations
     */
    public List<Reservation> getAllUserReservations(User user) {
        return reservationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Process expired READY_FOR_PICKUP reservations
     * This should be called periodically (e.g., scheduled task)
     */
    @Transactional
    public void processExpiredReservations() {
        List<Reservation> expiredReservations = reservationRepository
            .findExpiredReadyReservations(LocalDateTime.now());

        for (Reservation reservation : expiredReservations) {
            // Mark as expired
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);

            // Promote the next person in queue
            promoteQueue(reservation.getBook());
        }
    }

    /**
     * Get a reservation by ID
     * @param id The reservation ID
     * @return Optional containing the reservation if found
     */
    public Optional<Reservation> getReservationById(Long id) {
        return reservationRepository.findById(id);
    }

    /**
     * Complete a reservation (when user picks up the book)
     * @param reservation The reservation to complete
     */
    @Transactional
    public void completeReservation(Reservation reservation) {
        reservation.setStatus(ReservationStatus.FULFILLED);
        reservationRepository.save(reservation);
    }
}
