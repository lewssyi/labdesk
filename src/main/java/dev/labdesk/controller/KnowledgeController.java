package dev.labdesk.controller;

import dev.labdesk.dto.QuestionRequest;
import dev.labdesk.service.KnowledgeService;
import dev.labdesk.vo.KnowledgeAnswer;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @PostMapping("/ask")
    public KnowledgeAnswer ask(@Valid @RequestBody QuestionRequest request) {
        return knowledgeService.ask(request.question());
    }
}
