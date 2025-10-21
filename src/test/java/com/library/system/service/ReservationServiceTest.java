package com.library.system.service;

import com.library.system.domain.*;
import com.library.system.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReservationService reservationService;

    private User testUser;
    private User otherUser;
    private Book testBook;
    private Reservation testReservation;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(UserRole.MEMBER);

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");
        otherUser.setRole(UserRole.MEMBER);

        testBook = new Book();
        testBook.setId(1L);
        testBook.setIsbn("978-1234567890");
        testBook.setTitle("Test Book");
        testBook.setAuthor("Test Author");
        testBook.setStatus(BookStatus.BORROWED);

        testReservation = new Reservation();
        testReservation.setId(1L);
        testReservation.setUser(testUser);
        testReservation.setBook(testBook);
        testReservation.setStatus(ReservationStatus.PENDING);
        testReservation.setQueuePosition(1);
        testReservation.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void reserveBook_Success() {
        // Given
        when(reservationRepository.findActiveReservationByBookAndUser(testBook, testUser))
            .thenReturn(Optional.empty());
        when(reservationRepository.countPendingReservationsByBook(testBook)).thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        // When
        Reservation result = reservationService.reserveBook(testUser, testBook);

        // Then
        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertEquals(testBook, result.getBook());
        assertEquals(1, result.getQueuePosition());
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void reserveBook_UserAlreadyHasReservation_ThrowsException() {
        // Given
        when(reservationRepository.findActiveReservationByBookAndUser(testBook, testUser))
            .thenReturn(Optional.of(testReservation));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> reservationService.reserveBook(testUser, testBook));

        assertEquals("You already have an active reservation for this book", exception.getMessage());
        verify(reservationRepository, never()).save(any());
    }

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
        assertEquals(3, result.getQueuePosition());
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void hasActiveReservations_ReturnsTrue() {
        // Given
        when(reservationRepository.countPendingReservationsByBook(testBook)).thenReturn(3L);

        // When
        boolean result = reservationService.hasActiveReservations(testBook);

        // Then
        assertTrue(result);
        verify(reservationRepository).countPendingReservationsByBook(testBook);
    }

    @Test
    void hasActiveReservations_ReturnsFalse() {
        // Given
        when(reservationRepository.countPendingReservationsByBook(testBook)).thenReturn(0L);

        // When
        boolean result = reservationService.hasActiveReservations(testBook);

        // Then
        assertFalse(result);
        verify(reservationRepository).countPendingReservationsByBook(testBook);
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
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        reservationService.promoteQueue(testBook);

        // Then
        verify(reservationRepository).save(argThat(reservation ->
            reservation.getStatus() == ReservationStatus.READY_FOR_PICKUP &&
            reservation.getNotifiedAt() != null &&
            reservation.getExpiresAt() != null
        ));
    }

    @Test
    void promoteQueue_NoReservations_DoesNothing() {
        // Given
        when(reservationRepository.findPendingReservationsByBook(testBook))
            .thenReturn(new ArrayList<>());

        // When
        reservationService.promoteQueue(testBook);

        // Then
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancelReservation_Success_ReordersQueue() {
        // Given
        Reservation firstRes = new Reservation(testUser, testBook, 1);
        firstRes.setId(1L);
        firstRes.setStatus(ReservationStatus.PENDING);

        Reservation secondRes = new Reservation(otherUser, testBook, 2);
        secondRes.setId(2L);
        secondRes.setStatus(ReservationStatus.PENDING);

        Reservation thirdRes = new Reservation(new User(), testBook, 3);
        thirdRes.setId(3L);
        thirdRes.setStatus(ReservationStatus.PENDING);

        List<Reservation> pendingReservations = List.of(secondRes, thirdRes);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(firstRes));
        when(reservationRepository.findPendingReservationsByBook(testBook))
            .thenReturn(pendingReservations);
        when(reservationRepository.save(any(Reservation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        reservationService.cancelReservation(1L, testUser);

        // Then
        verify(reservationRepository).save(argThat(res ->
            res.getId().equals(1L) && res.getStatus() == ReservationStatus.CANCELLED
        ));
        verify(reservationRepository, times(3)).save(any(Reservation.class)); // 1 cancel + 2 reorders
    }

    @Test
    void cancelReservation_NotOwner_ThrowsException() {
        // Given
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> reservationService.cancelReservation(1L, otherUser));

        assertEquals("You can only cancel your own reservations", exception.getMessage());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancelReservation_NotFound_ThrowsException() {
        // Given
        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> reservationService.cancelReservation(999L, testUser));

        assertEquals("Reservation not found", exception.getMessage());
    }

    @Test
    void getUserReservations_ReturnsActiveReservations() {
        // Given
        List<Reservation> reservations = List.of(testReservation);
        when(reservationRepository.findActiveReservationsByUser(testUser))
            .thenReturn(reservations);

        // When
        List<Reservation> result = reservationService.getUserReservations(testUser);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testReservation, result.get(0));
        verify(reservationRepository).findActiveReservationsByUser(testUser);
    }

    @Test
    void processExpiredReservations_MarksExpiredAndPromotesNext() {
        // Given
        Reservation expiredRes = new Reservation(testUser, testBook, 1);
        expiredRes.setStatus(ReservationStatus.READY_FOR_PICKUP);
        expiredRes.setExpiresAt(LocalDateTime.now().minusHours(1));

        Reservation nextInQueue = new Reservation(otherUser, testBook, 2);
        nextInQueue.setStatus(ReservationStatus.PENDING);

        when(reservationRepository.findExpiredReadyReservations(any(LocalDateTime.class)))
            .thenReturn(List.of(expiredRes));
        when(reservationRepository.findPendingReservationsByBook(testBook))
            .thenReturn(List.of(nextInQueue));
        when(reservationRepository.save(any(Reservation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        reservationService.processExpiredReservations();

        // Then
        verify(reservationRepository).save(argThat(res ->
            res.equals(expiredRes) && res.getStatus() == ReservationStatus.EXPIRED
        ));
        verify(reservationRepository).save(argThat(res ->
            res.equals(nextInQueue) && res.getStatus() == ReservationStatus.READY_FOR_PICKUP
        ));
    }

    @Test
    void completeReservation_SetsStatusToFulfilled() {
        // Given
        testReservation.setStatus(ReservationStatus.READY_FOR_PICKUP);
        when(reservationRepository.save(any(Reservation.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        reservationService.completeReservation(testReservation);

        // Then
        verify(reservationRepository).save(argThat(res ->
            res.equals(testReservation) && res.getStatus() == ReservationStatus.FULFILLED
        ));
    }
}
