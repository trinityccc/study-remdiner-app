package com.studyreminder.service;

import com.studyreminder.dto.ReminderCreateRequest;
import com.studyreminder.dto.ReminderResponse;
import com.studyreminder.dto.ReminderUpdateRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ReminderService {
  private final SupabaseRestClient supabaseRestClient;

  public ReminderService(SupabaseRestClient supabaseRestClient) {
    this.supabaseRestClient = supabaseRestClient;
  }

  public List<ReminderResponse> getReminders(UUID userId, String accessToken) {
    return supabaseRestClient.listReminders(userId, accessToken);
  }

  public ReminderResponse createReminder(UUID userId, ReminderCreateRequest req, String accessToken) {
    if (req.title() == null || req.title().trim().isEmpty()) {
      throw new BadRequestException("title is required");
    }
    return supabaseRestClient.createReminder(userId, req, accessToken);
  }

  public ReminderResponse updateReminder(UUID userId, UUID reminderId, ReminderUpdateRequest req, String accessToken) {
    // Validation: at least one updatable field should be present (enforced in SupabaseRestClient too).
    return supabaseRestClient.updateReminder(userId, reminderId, req, accessToken);
  }

  public void deleteReminder(UUID userId, UUID reminderId, String accessToken) {
    supabaseRestClient.deleteReminder(userId, reminderId, accessToken);
  }
}

