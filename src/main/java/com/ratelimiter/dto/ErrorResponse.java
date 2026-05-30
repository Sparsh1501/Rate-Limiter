package com.ratelimiter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Structured error body returned by the global exception handler.
 */
@Data
@Builder
@AllArgsConstructor
public class ErrorResponse {

    /** When the error occurred (ISO-8601). */
    private Instant timestamp;

    /** HTTP status code. */
    private int status;

    /** Short error reason phrase. */
    private String error;

    /** Human readable detail message. */
    private String message;

    /** Request path that produced the error. */
    private String path;
}
