package com.studyreminder.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final SupabaseJwtVerifier verifier;
  private final ObjectMapper objectMapper;

  public JwtAuthenticationFilter(SupabaseJwtVerifier verifier, ObjectMapper objectMapper) {
    this.verifier = verifier;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    // Only secure API endpoints (static frontend + config remain public).
    return !path.startsWith("/reminders");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (auth == null || !auth.startsWith("Bearer ")) {
      writeUnauthorized(response, "Missing or invalid Authorization header");
      return;
    }

    String token = auth.substring("Bearer ".length());
    try {
      UUID userId = verifier.verifyAndExtractUserId(token);

      CurrentUser principal = new CurrentUser(userId);
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of());
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authentication);

      // Store the raw token so services can call Supabase with the same JWT (for RLS).
      request.setAttribute("accessToken", token);

      filterChain.doFilter(request, response);
    } catch (UnauthorizedException e) {
      writeUnauthorized(response, e.getMessage());
    } catch (Exception e) {
      writeUnauthorized(response, "Unauthorized");
    }
  }

  private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    var body = java.util.Map.of(
        "error", "unauthorized",
        "message", message
    );
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}

