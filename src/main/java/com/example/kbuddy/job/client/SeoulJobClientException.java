package com.example.kbuddy.job.client;

public class SeoulJobClientException extends RuntimeException {

    public enum Reason {
        SERVER_UNAVAILABLE,
        REQUEST_TIMEOUT,
        RESPONSE_INVALID
    }

    private final Reason reason;

    public SeoulJobClientException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
