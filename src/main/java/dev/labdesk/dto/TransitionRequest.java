package dev.labdesk.dto;

import jakarta.validation.constraints.*;

public record TransitionRequest(@NotBlank String status, @Min(0) int version,
                                @NotNull @Size(max = 500) String note) {}

