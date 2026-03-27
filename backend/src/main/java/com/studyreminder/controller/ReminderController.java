package com.studyreminder.controller;

import com.studyreminder.dto.ReminderCreateRequest;
import com.studyreminder.dto.ReminderResponse;
import com.studyreminder.dto.ReminderUpdateRequest;
import com.studyreminder.security.CurrentUser;
import com.studyreminder.service.ReminderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/reminders")
public class ReminderController {
  private final ReminderService service;

  public ReminderController(ReminderService service) {
    this.service = service;
  }

  @GetMapping
  public List<ReminderResponse> getReminders(HttpServletRequest request) {
    UUID userId = currentUserId();
    String accessToken = accessToken(request);
    return service.getReminders(userId, accessToken);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ReminderResponse createReminder(
      HttpServletRequest request,
      @Valid @RequestBody ReminderCreateRequest body
  ) {
    UUID userId = currentUserId();
    String accessToken = accessToken(request);
    return service.createReminder(userId, body, accessToken);
  }

  @PutMapping("/{id}")
  public ReminderResponse updateReminder(
      HttpServletRequest request,
      @PathVariable("id") UUID id,
      @Valid @RequestBody ReminderUpdateRequest body
  ) {
    UUID userId = currentUserId();
    String accessToken = accessToken(request);
    return service.updateReminder(userId, id, body, accessToken);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteReminder(HttpServletRequest request, @PathVariable("id") UUID id) {
    UUID userId = currentUserId();
    String accessToken = accessToken(request);
    service.deleteReminder(userId, id, accessToken);
  }

  private UUID currentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof CurrentUser cu)) {
      throw new com.studyreminder.security.UnauthorizedException("Missing authenticated user");
    }
    return cu.userId();
  }

  private String accessToken(HttpServletRequest request) {
    Object token = request.getAttribute("accessToken");
    if (!(token instanceof String tokenStr) || tokenStr.isBlank()) {
      throw new com.studyreminder.security.UnauthorizedException("Missing access token");
    }
    return tokenStr;
  }
}

