package com.buffden.aitextintelligencedashboard.exception;

public class LlmUnavailableException extends RuntimeException {
    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
