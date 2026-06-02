package com.stc.tickit.service;

import com.stc.tickit.domain.*;
import com.stc.tickit.dto.BookingRequest;
import com.stc.tickit.exception.InvalidBookingException;
import com.stc.tickit.exception.SeatUnavailableException;
import com.stc.tickit.repository.AppUserRepository;
import com.stc.tickit.repository.BookingRepository;
import com.stc.tickit.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Pure unit tests for the booking rules (US2/US3). No database required. */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock EventRepository eventRepository;
    @Mock AppUserRepository userRepository;

    @InjectMocks BookingService bookingService;

    Event event;

    @BeforeEach
    void setUp() {
        event = Event.builder()
                .id(1L).name("Demo").venue("Hall")
                .eventDate(LocalDateTime.now().plusDays(5))
                .totalSeats(10).availableSeats(10).version(0)
                .build();
    }

    @Test
    void book_decrementsAvailableSeats_andConfirms() {
        when(eventRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(event));
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(AppUser.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.book(
                new BookingRequest(1L, "Jane", "jane@example.com", 3));

        assertThat(event.getAvailableSeats()).isEqualTo(7);
        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(result.getQuantity()).isEqualTo(3);
    }

    @Test
    void book_rejectsOverbooking() {
        event.setAvailableSeats(2);
        when(eventRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> bookingService.book(
                new BookingRequest(1L, "Jane", "jane@example.com", 5)))
                .isInstanceOf(SeatUnavailableException.class);

        assertThat(event.getAvailableSeats()).isEqualTo(2); // unchanged
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancel_releasesSeats_andMarksCancelled() {
        event.setAvailableSeats(7);
        Booking booking = Booking.builder()
                .id(99L).event(event)
                .user(AppUser.builder().id(1L).email("jane@example.com").name("Jane").build())
                .quantity(3).status(BookingStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(booking));
        when(eventRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(event));

        bookingService.cancel(99L);

        assertThat(event.getAvailableSeats()).isEqualTo(10);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancel_rejectsAlreadyCancelled() {
        Booking booking = Booking.builder()
                .id(99L).event(event)
                .user(AppUser.builder().id(1L).email("j@e.com").name("J").build())
                .quantity(3).status(BookingStatus.CANCELLED)
                .createdAt(LocalDateTime.now())
                .build();
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(99L))
                .isInstanceOf(InvalidBookingException.class);
    }
}
