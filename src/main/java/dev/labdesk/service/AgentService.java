package dev.labdesk.service;

import dev.labdesk.dto.AgentChatRequest;
import dev.labdesk.security.CurrentUser;
import dev.labdesk.vo.AgentAnswer;

public interface AgentService {
    boolean isConfigured();
    AgentAnswer chat(CurrentUser actor, AgentChatRequest request);
}
