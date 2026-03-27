package com.studyreminder.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Minimal .env loader (KEY=VALUE) to avoid relying on shell env vars.
 * Supports quoted values and ignores blank lines + comments (#...).
 */
public final class DotEnvLoader {
  private DotEnvLoader() {}

  public static void loadIfPresent(String relativePath) {
    Path path = Path.of(System.getProperty("user.dir")).resolve(relativePath);
    if (!Files.exists(path)) {
      return;
    }

    List<String> lines;
    try {
      lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    } catch (IOException e) {
      return;
    }

    for (String rawLine : lines) {
      String line = rawLine.trim();
      if (line.isEmpty() || line.startsWith("#")) continue;

      // Allow "export KEY=VALUE"
      if (line.startsWith("export ")) {
        line = line.substring("export ".length()).trim();
      }

      int eq = line.indexOf('=');
      if (eq <= 0) continue;

      String key = line.substring(0, eq).trim();
      String value = line.substring(eq + 1).trim();

      if (key.isEmpty()) continue;

      // Strip single/double quotes around values.
      if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
        value = value.substring(1, value.length() - 1);
      }

      if (!value.isEmpty()) {
        System.setProperty(key, value);
      }
    }
  }
}

