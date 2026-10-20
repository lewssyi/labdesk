package dev.labdesk.dto;

import jakarta.validation.constraints.*;

public record QuestionRequest(@NotBlank @Size(max = 1000) String question) {}

