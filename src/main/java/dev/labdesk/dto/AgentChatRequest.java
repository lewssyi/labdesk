package dev.labdesk.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record AgentChatRequest(@NotBlank @Size(max = 1000) String message,
        @Size(max = 6) List<@NotNull @Valid Turn> history) {
    public record Turn(@NotBlank @Pattern(regexp = "user|assistant") String role,
                       @NotBlank @Size(max = 4000) String content) {}
}
