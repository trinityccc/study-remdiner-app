package com.studyreminder.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyreminder.config.SupabaseProperties;
import com.studyreminder.dto.ReminderCreateRequest;
import com.studyreminder.dto.ReminderResponse;
import com.studyreminder.dto.ReminderUpdateRequest;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.stereotype.Component;

@Component
public class SupabaseRestClient {
  private final SupabaseProperties supabase;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final String tableUrl;

  public SupabaseRestClient(SupabaseProperties supabase, ObjectMapper objectMapper) {
    this.supabase = supabase;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().build();
    this.tableUrl = supabase.url() + "/rest/v1/study_reminders";
  }

  public List<ReminderResponse> listReminders(UUID userId, String accessToken) {
    String url = tableUrl + "?select=*&user_id=eq." + userId + "&order=created_at.desc";
    HttpResponse<String> response = send(
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build(),
        accessToken,
        null,
        Map.of()
    );
    ensureOk(response);
    return parseArray(response.body(), new TypeReference<List<ReminderResponse>>() {});
  }

  public ReminderResponse createReminder(UUID userId, ReminderCreateRequest req, String accessToken) {
    Map<String, Object> row = new HashMap<>();
    row.put("user_id", userId);
    row.put("title", req.title().trim());
    if (req.description() == null || req.description().trim().isEmpty()) {
      row.put("description", null);
    } else {
      row.put("description", req.description().trim());
    }
    row.put("type", req.type().name());
    row.put("priority", req.priority().name());
    row.put("status", req.status().name());
    // Serialize due_date only if present; Supabase allows NULL.
    if (req.dueDate() != null) {
      row.put("due_date", req.dueDate());
    } else {
      row.put("due_date", null);
    }

    String body;
    try {
      body = objectMapper.writeValueAsString(List.of(row));
    } catch (Exception e) {
      throw new RuntimeException("Unable to serialize request", e);
    }

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(tableUrl))
        .header("Prefer", "return=representation")
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    HttpResponse<String> response = send(request, accessToken, body, Map.of("Prefer", "return=representation"));
    ensureOk(response);

    List<ReminderResponse> created = parseArray(response.body(), new TypeReference<List<ReminderResponse>>() {});
    return created.isEmpty() ? null : created.get(0);
  }

  public ReminderResponse updateReminder(UUID userId, UUID reminderId, ReminderUpdateRequest req, String accessToken) {
    Map<String, Object> patch = new HashMap<>();
    if (req.title().isPresent()) {
      String title = req.title().get().trim();
      if (title.isBlank()) throw new BadRequestException("title cannot be empty");
      patch.put("title", title);
    }
    if (req.description().isPresent()) {
      String desc = req.description().get();
      patch.put("description", desc == null || desc.trim().isEmpty() ? null : desc.trim());
    }
    if (req.type().isPresent()) patch.put("type", req.type().get().name());
    if (req.priority().isPresent()) patch.put("priority", req.priority().get().name());
    if (req.status().isPresent()) patch.put("status", req.status().get().name());
    if (req.dueDate().isPresent()) {
      // Include due_date when explicitly provided.
      patch.put("due_date", req.dueDate().get());
    }

    if (patch.isEmpty()) {
      throw new BadRequestException("No fields provided for update");
    }

    String url = tableUrl + "?id=eq." + reminderId + "&user_id=eq." + userId;
    String body;
    try {
      body = objectMapper.writeValueAsString(patch);
    } catch (Exception e) {
      throw new RuntimeException("Unable to serialize update body", e);
    }

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Prefer", "return=representation")
        .header("Content-Type", "application/json")
        .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
        .build();

    HttpResponse<String> response = send(request, accessToken, body, Map.of("Prefer", "return=representation"));
    ensureOk(response);

    List<ReminderResponse> updated = parseArray(response.body(), new TypeReference<List<ReminderResponse>>() {});
    return updated.isEmpty() ? null : updated.get(0);
  }

  public void deleteReminder(UUID userId, UUID reminderId, String accessToken) {
    String url = tableUrl + "?id=eq." + reminderId + "&user_id=eq." + userId;

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Prefer", "return=representation")
        .DELETE()
        .build();

    HttpResponse<String> response = send(request, accessToken, null, Map.of());
    ensureOk(response);
  }

  private HttpResponse<String> send(
      HttpRequest request,
      String accessToken,
      String body,
      Map<String, String> extraHeaders
  ) {
    HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri())
        .method(request.method(), request.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()));

    // Copy headers
    request.headers().map().forEach((k, v) -> v.forEach(val -> builder.header(k, val)));
    // Add required Supabase headers
    builder.header("apikey", supabase.anonKey());
    builder.header("Authorization", "Bearer " + accessToken);
    extraHeaders.forEach(builder::header);

    HttpRequest built = builder.build();
    try {
      return httpClient.send(built, HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      throw new RuntimeException("Supabase REST call failed", e);
    }
  }

  private void ensureOk(HttpResponse<String> response) {
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new RuntimeException("Supabase REST error: HTTP " + response.statusCode() + " " + response.body());
    }
  }

  private <T> T parseArray(String json, TypeReference<T> typeRef) {
    try {
      return objectMapper.readValue(json, typeRef);
    } catch (Exception e) {
      throw new RuntimeException("Unable to parse Supabase response", e);
    }
  }
}

