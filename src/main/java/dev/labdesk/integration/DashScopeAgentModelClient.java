package dev.labdesk.integration;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** 使用 Spring AI Alibaba 的原生 DashScope 适配器，工具循环由业务层限次执行。 */
@Component
public class DashScopeAgentModelClient implements AgentModelClient {
    private final DashScopeChatModel chatModel;
    private final String model;

    public DashScopeAgentModelClient(@Value("${spring.ai.dashscope.api-key:}") String key,
            @Value("${spring.ai.dashscope.base-url}") String baseUrl,
            @Value("${spring.ai.dashscope.chat.options.model}") String model) {
        this.model = model;
        if (key.isBlank()) {
            chatModel = null;
            return;
        }
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(20));
        var api = DashScopeApi.builder().apiKey(key).baseUrl(baseUrl)
                .restClientBuilder(RestClient.builder().requestFactory(factory)).build();
        chatModel = DashScopeChatModel.builder().dashScopeApi(api)
                .retryTemplate(RetryTemplate.builder().maxAttempts(1).build()).build();
    }

    @Override
    public boolean isConfigured() { return chatModel != null; }

    @Override
    public AssistantMessage chat(List<Message> messages, List<ToolCallback> tools) {
        if (!isConfigured()) throw new IllegalStateException("DashScope is not configured");
        var options = DashScopeChatOptions.builder().model(model).temperature(0.2).maxToken(1200)
                .enableThinking(false).toolCallbacks(tools).internalToolExecutionEnabled(false).build();
        var response = chatModel.call(new Prompt(messages, options));
        if (response == null || response.getResult() == null) {
            throw new IllegalStateException("Empty model response");
        }
        return response.getResult().getOutput();
    }
}
