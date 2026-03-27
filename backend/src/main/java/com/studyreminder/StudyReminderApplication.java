package com.studyreminder;

import com.studyreminder.config.SupabaseProperties;
import com.studyreminder.config.DotEnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SupabaseProperties.class)
public class StudyReminderApplication {
  public static void main(String[] args) {
    // Allow users to define backend/.env locally without relying on shell env vars.
    DotEnvLoader.loadIfPresent(".env");
    DotEnvLoader.loadIfPresent("backend/.env");
    SpringApplication.run(StudyReminderApplication.class, args);
  }
}

