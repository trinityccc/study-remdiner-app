package com.studyreminder.controller;

import com.studyreminder.security.UnauthorizedException;
import com.studyreminder.service.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ApiError> handleBadRequest(BadRequestException e) {
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(new ApiError(HttpStatus.BAD_REQUEST.value(), "bad_request", e.getMessage()));
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException e) {
    return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(new ApiError(HttpStatus.UNAUTHORIZED.value(), "unauthorized", e.getMessage()));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ApiError> handleIllegalState(IllegalStateException e) {
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "configuration_error", e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
    String msg = e.getBindingResult().getFieldErrors().stream()
        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
        .collect(Collectors.joining("; "));
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(new ApiError(HttpStatus.BAD_REQUEST.value(), "validation_error", msg));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleOther(HttpServletRequest req, Exception e) {
    String msg = e.getMessage();
    if (msg == null || msg.isBlank()) msg = "Unexpected server error";
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "internal_error", msg));
  }
}

