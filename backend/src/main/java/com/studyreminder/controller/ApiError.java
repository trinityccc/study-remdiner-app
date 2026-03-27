package com.studyreminder.controller;

public record ApiError(int status, String error, String message) {}

