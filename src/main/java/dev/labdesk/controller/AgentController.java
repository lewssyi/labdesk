package dev.labdesk.controller;

import dev.labdesk.dto.AgentChatRequest;
import dev.labdesk.security.CurrentUser;
import dev.labdesk.service.AgentService;
import dev.labdesk.vo.AgentAnswer;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final AgentService agent;
    public AgentController(AgentService agent) { this.agent = agent; }

    @GetMapping("/status")
    public Map<String, Boolean> status() { return Map.of("configured", agent.isConfigured()); }

    @PostMapping("/chat")
    public AgentAnswer chat(@RequestAttribute("currentUser") CurrentUser actor,
                            @Valid @RequestBody AgentChatRequest request) {
        return agent.chat(actor, request);
    }
}
