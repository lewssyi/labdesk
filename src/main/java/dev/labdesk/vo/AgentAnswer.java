package dev.labdesk.vo;

import java.util.List;

public record AgentAnswer(String mode, String answer, List<ToolStep> tools) {
    public record ToolStep(String name, boolean success, String result) {}
}
