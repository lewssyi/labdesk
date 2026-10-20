package dev.labdesk.dto;

import jakarta.validation.constraints.*;

public record CreateTicketRequest(@Positive long deviceId, @NotBlank @Size(max = 100) String title,
                                  @NotBlank @Size(max = 4000) String description) {}

