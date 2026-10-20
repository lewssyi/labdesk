package dev.labdesk.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record ReserveRequest(@Positive long deviceId, @NotNull LocalDateTime start, @NotNull LocalDateTime end) {}

