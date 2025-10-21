package com.library.system.repository;

import com.library.system.domain.Reservation;
import com.library.system.domain.ReservationStatus;
import com.library.system.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUser(User user);

    List<Reservation> findByUserOrderByCreatedAtDesc(User user);

    List<Reservation> findByUserAndStatus(User user, ReservationStatus status);

    List<Reservation> findByBookAndStatus(com.library.system.domain.Book book, ReservationStatus status);

    List<Reservation> findByBookOrderByQueuePositionAsc(com.library.system.domain.Book book);

    @Query("SELECT r FROM Reservation r WHERE r.book = :book AND r.status = 'PENDING' ORDER BY r.queuePosition ASC")
    List<Reservation> findPendingReservationsByBook(@Param("book") com.library.system.domain.Book book);

    @Query("SELECT r FROM Reservation r WHERE r.book = :book AND r.status = 'PENDING' ORDER BY r.createdAt ASC")
    List<Reservation> findPendingReservationsByBookOrderByCreatedAt(@Param("book") com.library.system.domain.Book book);

    @Query("SELECT r FROM Reservation r WHERE r.expiresAt < :date AND r.status = 'READY_FOR_PICKUP'")
    List<Reservation> findExpiredReadyReservations(@Param("date") LocalDateTime date);

    Optional<Reservation> findByUserAndBookAndStatus(User user, com.library.system.domain.Book book, ReservationStatus status);

    @Query("SELECT r FROM Reservation r WHERE r.book = :book AND r.user = :user AND r.status IN ('PENDING', 'READY_FOR_PICKUP')")
    Optional<Reservation> findActiveReservationByBookAndUser(@Param("book") com.library.system.domain.Book book, @Param("user") User user);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.book = :book AND r.status = 'PENDING'")
    long countPendingReservationsByBook(@Param("book") com.library.system.domain.Book book);

    @Query("SELECT r FROM Reservation r WHERE r.user = :user AND r.status IN ('PENDING', 'READY_FOR_PICKUP') ORDER BY r.createdAt DESC")
    List<Reservation> findActiveReservationsByUser(@Param("user") User user);
}
