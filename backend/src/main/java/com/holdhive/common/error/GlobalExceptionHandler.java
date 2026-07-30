package com.holdhive.common.error;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldErrorResponse> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> new FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage()))
            .toList();

        return ResponseEntity
            .badRequest()
            .body(error("VALIDATION_FAILED", "Request validation failed", fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(HttpMessageNotReadableException exception) {
        if (exception.getCause() instanceof InvalidFormatException invalidFormatException) {
            String field = invalidFieldName(invalidFormatException);
            String message = expectedValueMessage(invalidFormatException.getTargetType());
            return ResponseEntity
                .badRequest()
                .body(error(
                    "VALIDATION_FAILED",
                    "Request validation failed",
                    List.of(new FieldErrorResponse(field, message))
                ));
        }
        return ResponseEntity
            .badRequest()
            .body(error("MALFORMED_JSON", "Malformed JSON request", List.of()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingRequestParameter(
        MissingServletRequestParameterException exception
    ) {
        return ResponseEntity
            .badRequest()
            .body(error(
                "VALIDATION_FAILED",
                "Request validation failed",
                List.of(new FieldErrorResponse(
                    exception.getParameterName(),
                    "required request parameter is missing"
                ))
            ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleRequestParameterTypeMismatch(
        MethodArgumentTypeMismatchException exception
    ) {
        return ResponseEntity
            .badRequest()
            .body(error(
                "VALIDATION_FAILED",
                "Request validation failed",
                List.of(new FieldErrorResponse(
                    exception.getName(),
                    expectedValueMessage(exception.getRequiredType())
                ))
            ));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception) {
        return ResponseEntity
            .status(exception.status())
            .body(error(exception.code(), exception.getMessage(), List.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error("INTERNAL_ERROR", "Unexpected server error", List.of()));
    }

    private static ApiErrorResponse error(
        String code,
        String message,
        List<FieldErrorResponse> fieldErrors
    ) {
        return new ApiErrorResponse(
            code,
            message,
            fieldErrors,
            UUID.randomUUID().toString().replace("-", ""),
            Instant.now()
        );
    }

    private static String invalidFieldName(InvalidFormatException exception) {
        return exception.getPath().stream()
            .map(JsonMappingException.Reference::getFieldName)
            .filter(fieldName -> fieldName != null && !fieldName.isBlank())
            .reduce((ignored, last) -> last)
            .orElse("requestBody");
    }

    private static String expectedValueMessage(Class<?> targetType) {
        if (targetType != null && targetType.isEnum()) {
            String allowedValues = Arrays.stream(targetType.getEnumConstants())
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
            return "must be one of: " + allowedValues;
        }
        return "invalid value";
    }
}
