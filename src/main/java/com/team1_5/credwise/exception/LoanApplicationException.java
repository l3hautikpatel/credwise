package com.team1_5.credwise.exception;

public class LoanApplicationException extends RuntimeException {

    public LoanApplicationException(String message) {
        super(message);
    }

    // Optional additional constructors for advanced use cases:
    public LoanApplicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public LoanApplicationException(Throwable cause) {
        super(cause);
    }
}