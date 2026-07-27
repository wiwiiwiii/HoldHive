package com.holdhive.common.error;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
    String code,
    String message,
    List<FieldErrorResponse> fieldErrors,
    String traceId,
    Instant timestamp
) {
}
