package com.stc.tickit.exception;

/** Raised for invalid operations such as cancelling after the event date (US3). */
public class InvalidBookingException extends RuntimeException {
    public InvalidBookingException(String message) {
        super(message);
    }
}
