package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.service.AIService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author IAMLZY
 * @date 2026/4/10 22:26
 * @description AI服务实现，仅在ai.enabled=true时加载
 */

@Service
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class AIServiceImpl implements AIService {

    @Autowired
    @Qualifier("XianYuChatClient")
    ChatClient chatClient;

    @Autowired
    private VectorStore vectorStore;

    @Override
    public Flux<String> chatByRAG(String prompt,String goodsId) {
        // 1. 先从向量库搜索相关内容
        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(prompt)
                        .topK(5)
                        .similarityThreshold(0.6)
                        .filterExpression(String.format("goodsId == '%s'",goodsId))
                        .build()
        );

        // 2. 把搜索结果拼成上下文
        String context = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        // 3. 构建带上下文的 prompt
        String ragPrompt = String.format("""
                你是一个智能助手，请根据以下内部参考资料回答用户问题。
                如果参考内容中没有相关信息，请根据自身知识回答,但是不要乱回答,返回json格式{"goodName":"商品名称","":""}
            
                参考资料：
                %s
               
                用户问题：%s
                """, context, prompt);

        // 4. 请求大模型，流式返回
        return chatClient.prompt()
                .user(ragPrompt)
                .stream()
                .content();
    }

    @Override
    public void putDataToRAG(String content, String goodsId) {
        Map<String,Object> metadata = new HashMap<>();
        metadata.put("goodsId",goodsId);
        Document document = new Document(content,metadata);

        // 自动切片，默认配置参数：
        //         800,    // defaultChunkSize     每个 chunk 的 token 数
        //        350,    // minChunkSizeChars    最小 chunk 字符数
        //        5,      // minChunkLengthToEmbed 低于此 token 数的 chunk 会被丢弃
        //        10000,  // maxNumChunks         最大 chunk 数量
        //        true    // keepSeparator        是否保留分隔符
        TokenTextSplitter splitter = new TokenTextSplitter();
        vectorStore.add(splitter.apply(List.of(document)));
    }
}
