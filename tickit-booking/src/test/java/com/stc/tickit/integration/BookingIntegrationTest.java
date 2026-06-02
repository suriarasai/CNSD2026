package com.stc.tickit.integration;

import com.stc.tickit.dto.BookingRequest;
import com.stc.tickit.dto.BookingResponse;
import com.stc.tickit.dto.EventResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack test against a real PostgreSQL 16 container (Factor X parity).
 * Requires a running container engine (Podman Desktop or Docker).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private List<EventResponse> bookableEvents() {
        ResponseEntity<EventResponse[]> resp =
                rest.getForEntity(url("/api/v1/events"), EventResponse[].class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return Arrays.asList(resp.getBody());
    }

    @Test
    void us1_listExcludesSoldOutAndPastEvents() {
        // Order-independent: asserts the filter behavior, not an absolute count,
        // because other tests in this class mutate seat availability.
        List<EventResponse> events = bookableEvents();
        assertThat(events).noneMatch(e -> e.availableSeats() == 0);          // no sold-out
        assertThat(events).noneMatch(e -> e.name().equals("Last Month Drama"));   // no past event
        assertThat(events).noneMatch(e -> e.name().equals("Sold-Out Jazz Evening"));
        assertThat(events).anyMatch(e -> e.name().equals("Esplanade Symphony Night"));
    }

    @Test
    void us2_bookingDecrementsSeats() {
        EventResponse target = bookableEvents().stream()
                .filter(e -> e.name().equals("Esplanade Symphony Night"))
                .findFirst().orElseThrow();
        int before = target.availableSeats();

        BookingRequest req = new BookingRequest(target.id(), "Alice", "alice@example.com", 4);
        ResponseEntity<BookingResponse> created =
                rest.postForEntity(url("/api/v1/bookings"), req, BookingResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().status()).isEqualTo("CONFIRMED");

        int after = bookableEvents().stream()
                .filter(e -> e.id().equals(target.id())).findFirst().orElseThrow()
                .availableSeats();
        assertThat(after).isEqualTo(before - 4);
    }

    @Test
    void us2_concurrentBookingsNeverOverbook() throws Exception {
        // "Arts Theatre Musical" is seeded with exactly 2 seats.
        EventResponse arts = bookableEvents().stream()
                .filter(e -> e.name().equals("Arts Theatre Musical"))
                .findFirst().orElseThrow();
        assertThat(arts.availableSeats()).isEqualTo(2);

        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger created = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        List<Future<?>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final String email = "racer" + i + "@example.com";
            futures.add(pool.submit(() -> {
                start.await();
                BookingRequest req = new BookingRequest(arts.id(), "Racer", email, 1);
                ResponseEntity<String> r = rest.postForEntity(url("/api/v1/bookings"), req, String.class);
                if (r.getStatusCode() == HttpStatus.CREATED) created.incrementAndGet();
                else if (r.getStatusCode() == HttpStatus.CONFLICT) rejected.incrementAndGet();
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        // Exactly the 2 available seats are sold; the rest are rejected. No overbooking.
        assertThat(created.get()).isEqualTo(2);
        assertThat(rejected.get()).isEqualTo(threads - 2);

        int remaining = bookableEvents().stream()
                .filter(e -> e.id().equals(arts.id())).findFirst()
                .map(EventResponse::availableSeats).orElse(0);
        assertThat(remaining).isZero();
    }

    @Test
    void us3_cancelReleasesSeats() {
        EventResponse target = bookableEvents().stream()
                .filter(e -> e.name().equals("Shaw Classics Retrospective"))
                .findFirst().orElseThrow();
        int before = target.availableSeats();

        BookingRequest req = new BookingRequest(target.id(), "Bob", "bob@example.com", 5);
        BookingResponse booking =
                rest.postForEntity(url("/api/v1/bookings"), req, BookingResponse.class).getBody();
        assertThat(booking).isNotNull();

        rest.delete(url("/api/v1/bookings/" + booking.id()));

        int after = bookableEvents().stream()
                .filter(e -> e.id().equals(target.id())).findFirst().orElseThrow()
                .availableSeats();
        assertThat(after).isEqualTo(before); // seats returned
    }

    @Test
    void api_returnsTypedErrorForMissingEvent() {
        BookingRequest req = new BookingRequest(999999L, "Nobody", "no@example.com", 1);
        ResponseEntity<String> r = rest.postForEntity(url("/api/v1/bookings"), req, String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
