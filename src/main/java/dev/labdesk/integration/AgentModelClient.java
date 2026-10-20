package dev.labdesk.integration;

import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;

public interface AgentModelClient {
    boolean isConfigured();
    AssistantMessage chat(List<Message> messages, List<ToolCallback> tools);
}
