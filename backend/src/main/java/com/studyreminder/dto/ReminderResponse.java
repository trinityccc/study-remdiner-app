package com.studyreminder.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReminderResponse(
    UUID id,
    String title,
    String description,
    ReminderType type,
    ReminderPriority priority,
    @JsonProperty("due_date") LocalDateTime dueDate,
    ReminderStatus status,
    @JsonProperty("created_at") LocalDateTime createdAt
) {}

