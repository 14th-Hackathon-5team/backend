package com.example.kbuddy.ai.client;

public class AiClientException extends RuntimeException {

    public enum Reason {
        SERVER_UNAVAILABLE,
        REQUEST_TIMEOUT,
        RESPONSE_INVALID
    }

    private final Reason reason;

    public AiClientException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
