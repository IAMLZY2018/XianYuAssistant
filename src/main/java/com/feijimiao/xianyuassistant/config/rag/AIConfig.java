package com.feijimiao.xianyuassistant.config.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author IAMLZY
 * @date 2026/4/10 22:43
 * @description
 */
@Configuration
public class AIConfig {
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
