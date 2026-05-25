package com.buffden.aitextintelligencedashboard.exception;

public class ParseException extends RuntimeException {
    private final String rawResponse;

    public ParseException(String message, String rawResponse, Throwable cause) {
        super(message, cause);
        this.rawResponse = rawResponse;
    }

    public String getRawResponse() {
        return rawResponse;
    }
}
