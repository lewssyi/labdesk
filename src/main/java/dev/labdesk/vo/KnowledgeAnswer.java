package dev.labdesk.vo;

import dev.labdesk.entity.Knowledge;
import java.util.List;

public record KnowledgeAnswer(String mode, String answer, List<Knowledge> sources, String notice) {}

