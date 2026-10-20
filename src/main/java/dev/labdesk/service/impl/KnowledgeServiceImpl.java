package dev.labdesk.service.impl;

import dev.labdesk.integration.ChatModelClient;
import dev.labdesk.mapper.KnowledgeMapper;
import dev.labdesk.service.KnowledgeService;
import dev.labdesk.vo.KnowledgeAnswer;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {
    private final KnowledgeMapper knowledgeMapper;
    private final ChatModelClient modelClient;

    public KnowledgeServiceImpl(KnowledgeMapper knowledgeMapper, ChatModelClient modelClient) {
        this.knowledgeMapper = knowledgeMapper;
        this.modelClient = modelClient;
    }

    @Override
    public KnowledgeAnswer ask(String question) {
        String normalized = question.toLowerCase(Locale.ROOT);
        var sources = knowledgeMapper.findAll().stream()
                .filter(entry -> Arrays.stream(entry.getKeywords().split(","))
                        .anyMatch(word -> normalized.contains(word.toLowerCase(Locale.ROOT))))
                .limit(3).toList();
        if (sources.isEmpty()) {
            return new KnowledgeAnswer("NO_EVIDENCE", "知识库未找到依据，请补充问题或创建工单。", sources, null);
        }
        String context = sources.stream()
                .map(entry -> "[" + entry.getId() + "] " + entry.getTitle() + "：" + entry.getContent())
                .reduce("", (a, b) -> a + "\n" + b);
        if (!modelClient.isConfigured()) {
            return new KnowledgeAnswer("RETRIEVAL_ONLY", context, sources,
                    "尚未配置模型 API Key；当前为检索结果，不是大模型回答。");
        }
        var answer = modelClient.answer(question, context);
        return answer.map(value -> new KnowledgeAnswer("LLM", value, sources, null))
                .orElseGet(() -> new KnowledgeAnswer("FALLBACK", context, sources, "模型服务暂不可用，已返回检索依据。"));
    }
}
