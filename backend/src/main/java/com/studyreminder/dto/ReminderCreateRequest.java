package com.studyreminder.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record ReminderCreateRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String description,
    @NotNull ReminderType type,
    @NotNull ReminderPriority priority,
    @JsonProperty("due_date") LocalDateTime dueDate,
    @NotNull ReminderStatus status
) {}

