package com.stc.tickit.exception;

/** Raised when a booking requests more seats than are available (US2). */
public class SeatUnavailableException extends RuntimeException {
    public SeatUnavailableException(String message) {
        super(message);
    }
}
