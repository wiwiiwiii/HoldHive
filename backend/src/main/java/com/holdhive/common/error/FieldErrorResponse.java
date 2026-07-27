package com.holdhive.common.error;

public record FieldErrorResponse(
    String field,
    String message
) {
}
