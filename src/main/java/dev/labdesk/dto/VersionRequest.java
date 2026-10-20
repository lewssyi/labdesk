package dev.labdesk.dto;

import jakarta.validation.constraints.*;

public record VersionRequest(@Min(0) int version) {}

