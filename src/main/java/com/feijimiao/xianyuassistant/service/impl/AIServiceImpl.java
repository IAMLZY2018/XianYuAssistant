package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.service.AIService;
import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author IAMLZY
 * @date 2026/4/10 22:26
 * @description AI服务实现，仅在ai.enabled=true时加载
 */

@Service
@Slf4j
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class AIServiceImpl implements AIService {

    @Autowired
    @Qualifier("XianYuChatClient")
    ChatClient chatClient;

    @Autowired
    private VectorStore vectorStore;

    @Override
    public Flux<String> chatByRAG(String prompt,String goodsId) {
        long startTime = System.currentTimeMillis();
        log.info("[AI Chat] 收到请求, prompt={}, goodsId={}", prompt, goodsId);

        // 1. 先从向量库搜索相关内容
        long searchStart = System.currentTimeMillis();
        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(prompt)
                        .topK(5)
                        .similarityThreshold(0.1)
                        .filterExpression(String.format("goodsId == '%s'",goodsId))
                        .build()
        );
        long searchCost = System.currentTimeMillis() - searchStart;
        log.info("[AI Chat] 向量搜索耗时: {}ms, 命中文档数: {}", searchCost, documents.size());

        // 2. 把搜索结果拼成上下文
        String context = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        // 3. 构建带上下文的 prompt
        String ragPrompt = String.format("""
                你是一个闲鱼卖家，不要回复的像AI。
                参考相关信息回答,不要乱回答,不知道就根据自身理解简单回答，不要犯错
            
                参考资料：
                %s
               
                用户问题：%s
                """, context, prompt);

        long llmStart = System.currentTimeMillis();
        log.info("[AI Chat] 准备调用LLM, 总预处理耗时: {}ms", llmStart - startTime);
        log.info("[AI Chat] LLM请求参数 - model: deepseek-v3, temperature: 0.7, goodsId: {}", goodsId);
        log.info("[AI Chat] LLM完整prompt:\n{}", ragPrompt);

        // 4. 请求大模型，流式返回
        AtomicBoolean firstTokenLogged = new AtomicBoolean(false);
        return chatClient.prompt()
                .user(ragPrompt)
                .stream()
                .content()
                .doOnNext(token -> {
                    if (firstTokenLogged.compareAndSet(false, true)) {
                        long firstTokenCost = System.currentTimeMillis() - llmStart;
                        log.info("[AI Chat] 首 token 耗时: {}ms (从请求到LLM开始输出)", firstTokenCost);
                    }
                });
    }

    @Override
    public void putDataToRAG(String content, String goodsId) {
        log.info("[AI RAG] 写入Chroma, goodsId={}, 内容长度={}字符, 内容={}", goodsId, content.length(), content);

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
        List<Document> chunks = splitter.apply(List.of(document));
        log.info("[AI RAG] 切片完成, 切片数={}, 写入Chroma...", chunks.size());
        vectorStore.add(chunks);
        log.info("[AI RAG] 写入Chroma完成");
    }
}
