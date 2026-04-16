package com.feijimiao.xianyuassistant.config.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreAutoConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author IAMLZY
 * @date 2026/4/10 22:43
 * @description AI配置，仅在ai.enabled=true时加载，并引入Spring AI自动配置
 */
@Configuration
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
@Import({
    OpenAiChatAutoConfiguration.class,
    OpenAiEmbeddingAutoConfiguration.class,
    ChromaVectorStoreAutoConfiguration.class
})
public class AIConfig {
    @Bean
    @Qualifier("XianYuChatClient")
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("你是一个闲鱼智能客服助手")
                .build();
    }
}
