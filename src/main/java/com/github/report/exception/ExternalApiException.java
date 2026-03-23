package com.github.report.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ExternalApiException extends RuntimeException {
    private final HttpStatus status;
    public ExternalApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
