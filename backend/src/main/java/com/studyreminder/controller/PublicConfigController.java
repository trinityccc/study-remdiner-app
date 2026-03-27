package com.studyreminder.controller;

import com.studyreminder.config.SupabaseProperties;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicConfigController {
  private final SupabaseProperties supabase;

  public PublicConfigController(SupabaseProperties supabase) {
    this.supabase = supabase;
  }

  @GetMapping("/config")
  public Map<String, String> config() {
    // Fail fast with a clear message if Supabase env vars are missing.
    // This prevents the frontend from silently using invalid `${...}` config.
    String url = supabase.url();
    String anon = supabase.anonKey();
    if (url == null || url.isBlank() || url.contains("${")) {
      throw new IllegalStateException("Missing SUPABASE_URL. Set it via environment variables or create backend/.env.");
    }
    if (anon == null || anon.isBlank() || anon.contains("${")) {
      throw new IllegalStateException("Missing SUPABASE_ANON_KEY. Set it via environment variables or create backend/.env.");
    }

    // Intentionally public: anon key + project URL are safe for client-side auth.
    return Map.of(
        "supabaseUrl", url,
        "supabaseAnonKey", anon
    );
  }
}

