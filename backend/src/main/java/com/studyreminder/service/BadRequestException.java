package com.studyreminder.service;

public class BadRequestException extends RuntimeException {
  public BadRequestException(String message) {
    super(message);
  }
}

