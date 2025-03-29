package com.team1_5.credwise.exception;
import org.springframework.http.HttpStatus;

public class LoanApplicationException extends RuntimeException {

    private HttpStatus status;

    // Correct constructor
    public LoanApplicationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

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