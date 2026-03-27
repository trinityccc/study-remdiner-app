package com.studyreminder.security;

import com.studyreminder.config.SupabaseProperties;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class SupabaseJwtVerifier {
  private final SupabaseProperties supabase;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public SupabaseJwtVerifier(SupabaseProperties supabase, ObjectMapper objectMapper) {
    this.supabase = supabase;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
  }

  public UUID verifyAndExtractUserId(String accessToken) {
    String url = supabase.url() + "/auth/v1/user";
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(10))
        .header("Accept", "application/json")
        .header("apikey", supabase.anonKey())
        .header("Authorization", "Bearer " + accessToken)
        .GET()
        .build();

    HttpResponse<String> response;
    try {
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (Exception e) {
      throw new UnauthorizedException("Unable to validate Supabase token");
    }

    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new UnauthorizedException("Invalid or expired token");
    }

    try {
      JsonNode user = objectMapper.readTree(response.body());
      JsonNode id = user.get("id");
      if (id == null || id.asText().isBlank()) {
        throw new UnauthorizedException("Token user id missing");
      }
      return UUID.fromString(id.asText());
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new UnauthorizedException("Unable to parse token user");
    }
  }
}

