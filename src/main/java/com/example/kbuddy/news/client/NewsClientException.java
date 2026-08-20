package com.example.kbuddy.news.client;

public class NewsClientException extends RuntimeException {

    public enum Reason {
        SERVER_UNAVAILABLE,
        REQUEST_TIMEOUT,
        RESPONSE_INVALID
    }

    private final Reason reason;

    public NewsClientException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
