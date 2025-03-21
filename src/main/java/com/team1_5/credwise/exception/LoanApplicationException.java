package com.team1_5.credwise.exception;

/**
 * General exception for loan application-related errors
 */
public class LoanApplicationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public LoanApplicationException(String message) {
        super(message);
    }

    public LoanApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}