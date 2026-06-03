package com.example.multimodal.gemini;

/** Unchecked exception surfaced to controllers when a Gemini call fails. */
public class GeminiException extends RuntimeException {

    public GeminiException(String message) {
        super(message);
    }

    public GeminiException(String message, Throwable cause) {
        super(message, cause);
    }
}
