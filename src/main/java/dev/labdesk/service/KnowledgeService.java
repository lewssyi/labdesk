package dev.labdesk.service;

import dev.labdesk.vo.KnowledgeAnswer;

public interface KnowledgeService {
    KnowledgeAnswer ask(String question);
}
