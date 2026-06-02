package com.stc.tickit.exception;

import java.time.Instant;

/** Stable JSON error shape returned by the REST API. */
public record ApiError(Instant timestamp, int status, String error, String message) {
}
