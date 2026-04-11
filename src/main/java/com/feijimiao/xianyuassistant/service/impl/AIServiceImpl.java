package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.service.AIService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author IAMLZY
 * @date 2026/4/10 22:26
 * @description
 */


@Service
public class AIServiceImpl implements AIService {


    @Autowired
    @Qualifier("XianYuChatClient")
    ChatClient chatClient;

    @Autowired
    private VectorStore vectorStore;


    @Override
    public Flux<String> chatByRAG(String prompt) {
        // 1. 先从向量库搜索相关内容
        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(prompt)
                        .topK(5)
                        .similarityThreshold(0.6)
                        .build()
        );

        // 2. 把搜索结果拼成上下文
        String context = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        // 3. 构建带上下文的 prompt
        String ragPrompt = String.format("""
                你是一个智能助手，请根据以下参考内容回答用户问题。
                如果参考内容中没有相关信息，请根据自身知识回答。
                
                参考内容：
                %s
               
                用户问题：%s
                """, context, prompt);

        // 4. 请求大模型，流式返回
        return chatClient.prompt()
                .user(ragPrompt)
                .stream()
                .content();
    }
}
