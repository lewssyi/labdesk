package dev.labdesk.integration;

import java.util.Optional;

/** 外部模型适配器，不参与数据库访问或业务权限判断。 */
public interface ChatModelClient {
    boolean isConfigured();
    Optional<String> answer(String question, String context);
}
