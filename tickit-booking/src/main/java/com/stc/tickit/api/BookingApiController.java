package com.stc.tickit.api;

import com.stc.tickit.dto.BookingRequest;
import com.stc.tickit.dto.BookingResponse;
import com.stc.tickit.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/** US2 (create) and US3 (cancel) over REST. */
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingApiController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
        BookingResponse body = BookingResponse.from(bookingService.book(request));
        return ResponseEntity.created(URI.create("/api/v1/bookings/" + body.id())).body(body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id) {
        bookingService.cancel(id);
    }

    @GetMapping
    public List<BookingResponse> byEmail(@RequestParam String email) {
        return bookingService.bookingsForEmail(email).stream()
                .map(BookingResponse::from)
                .toList();
    }
}
