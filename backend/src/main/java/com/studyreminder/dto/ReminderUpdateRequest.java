package com.studyreminder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReminderUpdateRequest(
    // Validate the String value inside Optional, not the Optional container itself.
    Optional<@Size(max = 200) String> title,
    Optional<@Size(max = 2000) String> description,
    Optional<ReminderType> type,
    Optional<ReminderPriority> priority,
    @JsonProperty("due_date") Optional<LocalDateTime> dueDate,
    Optional<ReminderStatus> status
) {}

