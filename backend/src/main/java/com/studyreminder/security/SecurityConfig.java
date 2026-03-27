package com.studyreminder.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
  private final SupabaseJwtVerifier verifier;
  private final ObjectMapper objectMapper;

  public SecurityConfig(SupabaseJwtVerifier verifier, ObjectMapper objectMapper) {
    this.verifier = verifier;
    this.objectMapper = objectMapper;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(verifier, objectMapper);

    http
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        // This app is stateless and uses Supabase JWTs. Disable default Spring login mechanisms.
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            // Public config for the frontend (supabase URL + anon key only).
            .requestMatchers("/config").permitAll()
            // Static frontend (served by Spring).
            .requestMatchers("/", "/login.html", "/signup.html", "/dashboard.html", "/assets/**").permitAll()
            // Health checks.
            .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}

