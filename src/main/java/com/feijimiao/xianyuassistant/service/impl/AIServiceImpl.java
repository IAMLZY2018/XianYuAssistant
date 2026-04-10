package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.service.AIService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * @author IAMLZY
 * @date 2026/4/10 22:26
 * @description
 */


@Service
public class AIServiceImpl implements AIService {


    @Override
    public Flux<String> chatByRAG(String prompt) {
        return null;
    }
}
