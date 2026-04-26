package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.config.rag.DynamicAIChatClientManager;
import com.feijimiao.xianyuassistant.config.rag.DynamicVectorStoreManager;
import com.feijimiao.xianyuassistant.service.AIService;
import com.feijimiao.xianyuassistant.service.SysSettingService;
import com.feijimiao.xianyuassistant.service.bo.RAGDataRespBO;
import com.feijimiao.xianyuassistant.service.bo.RAGReplyResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * AI服务实现
 * 使用 DynamicAIChatClientManager 动态获取 ChatClient
 * 使用 DynamicVectorStoreManager 动态获取 VectorStore
 * API Key 未配置时自动降级，返回提示信息
 *
 * @author IAMLZY
 * @date 2026/4/10 22:26
 */

@Service
@Slf4j
public class AIServiceImpl implements AIService {

    /** 系统提示词配置键 */
    private static final String SYS_PROMPT_KEY = "sys_prompt";

    /** 相似度阈值配置键 */
    private static final String SIMILARITY_THRESHOLD_KEY = "similarity_threshold";

    /** 默认系统提示词 */
    private static final String DEFAULT_SYS_PROMPT = "你是一个闲鱼卖家，你叫肥极喵，不要回复的像AI，简短回答\n参考相关信息回答,不要乱回答,不知道就换不同语气回复提示用户详细点询问";

    /** 默认相似度阈值 */
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.1;

    /** AI不可用时的降级提示 */
    private static final String AI_NOT_AVAILABLE_MSG = "AI服务暂未配置，请在系统设置中配置API Key后再试";

    @Autowired
    private DynamicAIChatClientManager dynamicAIChatClientManager;

    @Autowired
    private DynamicVectorStoreManager dynamicVectorStoreManager;

    @Autowired
    private SysSettingService sysSettingService;

    @Override
    public Flux<String> chatByRAG(String prompt, String goodsId) {
        long startTime = System.currentTimeMillis();
        log.info("[AI Chat] 收到请求, prompt={}, goodsId={}", prompt, goodsId);

        // 1. 检查AI是否可用
        ChatClient chatClient = dynamicAIChatClientManager.getChatClient();
        if (chatClient == null) {
            log.warn("[AI Chat] AI服务不可用，返回降级提示");
            return Flux.just(AI_NOT_AVAILABLE_MSG);
        }

        // 2. 获取向量库（动态初始化）
        VectorStore vectorStore = dynamicVectorStoreManager.getVectorStore();
        if (vectorStore == null) {
            log.warn("[AI Chat] 向量库未初始化，使用无上下文模式");
            return chatWithoutContext(chatClient, prompt, startTime);
        }

        // 3. 先从向量库搜索相关内容
        long searchStart = System.currentTimeMillis();
        List<Document> documents;
        try {
            // 从系统配置中获取相似度阈值
            double similarityThreshold = getSimilarityThreshold();
            
            documents = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(prompt)
                            .topK(5)
                            .similarityThreshold(similarityThreshold)
                            .filterExpression(String.format("goodsId == '%s'", goodsId))
                            .build()
            );
        } catch (Exception e) {
            log.warn("[AI Chat] 向量搜索失败，使用无上下文模式: {}", e.getMessage());
            return chatWithoutContext(chatClient, prompt, startTime);
        }
        long searchCost = System.currentTimeMillis() - searchStart;
        log.info("[AI Chat] 向量搜索耗时: {}ms, 命中文档数: {}", searchCost, documents.size());

        // 4. 把搜索结果拼成上下文
        String context = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        // 5. 从系统配置中获取系统提示词
        String sysPrompt = sysSettingService.getSettingValue(SYS_PROMPT_KEY);
        if (sysPrompt == null || sysPrompt.trim().isEmpty()) {
            sysPrompt = DEFAULT_SYS_PROMPT;
            log.info("[AI Chat] 使用默认系统提示词");
        } else {
            log.info("[AI Chat] 使用自定义系统提示词");
        }

        // 6. 构建用户消息（参考资料 + 用户问题）
        String userMessage = String.format("""
                参考资料：
                %s

                用户问题：%s
                """, context, prompt);

        long llmStart = System.currentTimeMillis();
        log.info("[AI Chat] 准备调用LLM, 总预处理耗时: {}ms", llmStart - startTime);

        // 7. 请求大模型，流式返回
        AtomicBoolean firstTokenLogged = new AtomicBoolean(false);
        return chatClient.prompt()
                .system(sysPrompt)
                .user(userMessage)
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
    public RAGReplyResult chatByRAGWithDetails(String msg, String goodsId) {
        RAGReplyResult result = new RAGReplyResult();
        
        // 1. 检查AI是否可用
        ChatClient chatClient = dynamicAIChatClientManager.getChatClient();
        if (chatClient == null) {
            result.setReplyContent(AI_NOT_AVAILABLE_MSG);
            return result;
        }

        // 2. 获取向量库
        VectorStore vectorStore = dynamicVectorStoreManager.getVectorStore();
        
        // 3. 搜索RAG命中资料
        List<RAGReplyResult.RAGHitDetail> hitDetails = new java.util.ArrayList<>();
        String context = "";
        
        if (vectorStore != null) {
            try {
                double similarityThreshold = getSimilarityThreshold();
                List<Document> documents = vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(msg)
                                .topK(5)
                                .similarityThreshold(similarityThreshold)
                                .filterExpression(String.format("goodsId == '%s'", goodsId))
                                .build()
                );
                
                // 构建命中详情
                for (Document doc : documents) {
                    RAGReplyResult.RAGHitDetail detail = new RAGReplyResult.RAGHitDetail();
                    detail.setDocumentId(doc.getId());
                    detail.setContent(doc.getText());
                    // 尝试获取score
                    Object score = doc.getMetadata().get("distance");
                    if (score instanceof Number) {
                        detail.setScore(((Number) score).doubleValue());
                    }
                    hitDetails.add(detail);
                }
                
                context = documents.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n---\n"));
                
                log.info("[AI Chat WithDetails] 向量搜索命中文档数: {}", documents.size());
            } catch (Exception e) {
                log.warn("[AI Chat WithDetails] 向量搜索失败，使用无上下文模式: {}", e.getMessage());
            }
        }

        // 4. 获取系统提示词
        String sysPrompt = sysSettingService.getSettingValue(SYS_PROMPT_KEY);
        if (sysPrompt == null || sysPrompt.trim().isEmpty()) {
            sysPrompt = DEFAULT_SYS_PROMPT;
        }

        // 5. 构建用户消息
        String userMessage;
        if (!context.isEmpty()) {
            userMessage = String.format("""
                    参考资料：
                    %s

                    用户问题：%s
                    """, context, msg);
        } else {
            userMessage = msg;
        }

        // 6. 请求大模型，阻塞等待完整响应
        try {
            String replyContent = chatClient.prompt()
                    .system(sysPrompt)
                    .user(userMessage)
                    .call()
                    .content();
            result.setReplyContent(replyContent);
        } catch (Exception e) {
            log.error("[AI Chat WithDetails] 调用LLM失败: {}", e.getMessage());
            result.setReplyContent("AI回复生成失败：" + e.getMessage());
        }

        result.setHitDetails(hitDetails);
        return result;
    }

    @Override
    public RAGReplyResult chatByRAGWithDetails(String msg, String goodsId, String contextMessages) {
        RAGReplyResult result = new RAGReplyResult();
        
        ChatClient chatClient = dynamicAIChatClientManager.getChatClient();
        if (chatClient == null) {
            result.setReplyContent(AI_NOT_AVAILABLE_MSG);
            return result;
        }

        VectorStore vectorStore = dynamicVectorStoreManager.getVectorStore();
        
        List<RAGReplyResult.RAGHitDetail> hitDetails = new java.util.ArrayList<>();
        String context = "";
        
        if (vectorStore != null) {
            try {
                double similarityThreshold = getSimilarityThreshold();
                List<Document> documents = vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(msg)
                                .topK(5)
                                .similarityThreshold(similarityThreshold)
                                .filterExpression(String.format("goodsId == '%s'", goodsId))
                                .build()
                );
                
                for (Document doc : documents) {
                    RAGReplyResult.RAGHitDetail detail = new RAGReplyResult.RAGHitDetail();
                    detail.setDocumentId(doc.getId());
                    detail.setContent(doc.getText());
                    Object score = doc.getMetadata().get("distance");
                    if (score instanceof Number) {
                        detail.setScore(((Number) score).doubleValue());
                    }
                    hitDetails.add(detail);
                }
                
                context = documents.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n---\n"));
                
                log.info("[AI Chat WithDetails+Context] 向量搜索命中文档数: {}", documents.size());
            } catch (Exception e) {
                log.warn("[AI Chat WithDetails+Context] 向量搜索失败，使用无上下文模式: {}", e.getMessage());
            }
        }

        String sysPrompt = sysSettingService.getSettingValue(SYS_PROMPT_KEY);
        if (sysPrompt == null || sysPrompt.trim().isEmpty()) {
            sysPrompt = DEFAULT_SYS_PROMPT;
        }

        String userMessage;
        StringBuilder userMsgBuilder = new StringBuilder();
        
        if (!context.isEmpty()) {
            userMsgBuilder.append("参考资料：\n").append(context).append("\n\n");
        }
        
        if (contextMessages != null && !contextMessages.isEmpty()) {
            userMsgBuilder.append("历史对话：\n").append(contextMessages).append("\n\n");
        }
        
        userMsgBuilder.append("用户问题：").append(msg);
        userMessage = userMsgBuilder.toString();

        try {
            String replyContent = chatClient.prompt()
                    .system(sysPrompt)
                    .user(userMessage)
                    .call()
                    .content();
            result.setReplyContent(replyContent);
        } catch (Exception e) {
            log.error("[AI Chat WithDetails+Context] 调用LLM失败: {}", e.getMessage());
            result.setReplyContent("AI回复生成失败：" + e.getMessage());
        }

        result.setHitDetails(hitDetails);
        return result;
    }

    /**
     * 无上下文模式聊天（向量库不可用时的降级方案）
     */
    private Flux<String> chatWithoutContext(ChatClient chatClient, String prompt, long startTime) {
        String sysPrompt = sysSettingService.getSettingValue(SYS_PROMPT_KEY);
        if (sysPrompt == null || sysPrompt.trim().isEmpty()) {
            sysPrompt = DEFAULT_SYS_PROMPT;
        }

        long llmStart = System.currentTimeMillis();
        log.info("[AI Chat] 使用无上下文模式, 预处理耗时: {}ms", llmStart - startTime);

        AtomicBoolean firstTokenLogged = new AtomicBoolean(false);
        return chatClient.prompt()
                .system(sysPrompt)
                .user(prompt)
                .stream()
                .content()
                .doOnNext(token -> {
                    if (firstTokenLogged.compareAndSet(false, true)) {
                        long firstTokenCost = System.currentTimeMillis() - llmStart;
                        log.info("[AI Chat] 首 token 耗时: {}ms", firstTokenCost);
                    }
                });
    }

    @Override
    public void putDataToRAG(String content, String goodsId) {
        VectorStore vectorStore = dynamicVectorStoreManager.getVectorStore();
        if (vectorStore == null) {
            log.warn("[AI RAG] 向量库未初始化，无法写入数据");
            throw new RuntimeException("向量库未初始化，请检查AI配置是否正确");
        }

        log.info("[AI RAG] 写入SimpleVectorStore, goodsId={}, 内容长度={}字符", goodsId, content.length());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("goodsId", goodsId);
        metadata.put("createTime", new Date());

        Document document = new Document(content, metadata);

        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(List.of(document));
        log.info("[AI RAG] 切片完成, 切片数={}, 写入SimpleVectorStore...", chunks.size());
        vectorStore.add(chunks);

        // 持久化保存到文件
        dynamicVectorStoreManager.saveToFile();

        log.info("[AI RAG] 写入SimpleVectorStore完成");
    }

    @Override
    public List<RAGDataRespBO> queryRAGDataBygoodsId(String goodsId) {
        VectorStore vectorStore = dynamicVectorStoreManager.getVectorStore();
        if (vectorStore == null) {
            log.warn("[AI RAG] 向量库未初始化，无法查询数据");
            throw new RuntimeException("向量库未初始化，请检查AI配置是否正确");
        }

        log.info("[AI RAG] 查询SimpleVectorStore数据, goodsId={}", goodsId);

        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("")
                        .topK(100)
                        .similarityThreshold(0.0)
                        .filterExpression(String.format("goodsId == '%s'", goodsId))
                        .build()
        );

        List<RAGDataRespBO> result = documents.stream().map(doc -> {
            RAGDataRespBO bo = new RAGDataRespBO();
            bo.setDocumentId(doc.getId());
            bo.setGoodsID(goodsId);
            bo.setContent(doc.getText());
            Object createTime = doc.getMetadata().get("createTime");
            bo.setCreateTime(createTime != null ? createTime.toString() : null);
            return bo;
        }).collect(Collectors.toList());

        log.info("[AI RAG] 查询SimpleVectorStore数据完成, goodsId={}, 结果数={}", goodsId, result.size());
        return result;
    }

    @Override
    public void deleteRAGDataByDocumentId(String documentId) {
        VectorStore vectorStore = dynamicVectorStoreManager.getVectorStore();
        if (vectorStore == null) {
            log.warn("[AI RAG] 向量库未初始化，无法删除数据");
            throw new RuntimeException("向量库未初始化，请检查AI配置是否正确");
        }

        log.info("[AI RAG] 删除SimpleVectorStore数据, documentId={}", documentId);
        vectorStore.delete(List.of(documentId));

        // 持久化保存到文件
        dynamicVectorStoreManager.saveToFile();

        log.info("[AI RAG] 删除SimpleVectorStore数据完成, documentId={}", documentId);
    }

    /**
     * 获取相似度阈值
     * 从系统配置中读取，如果未配置则使用默认值
     */
    private double getSimilarityThreshold() {
        try {
            String thresholdStr = sysSettingService.getSettingValue(SIMILARITY_THRESHOLD_KEY);
            if (thresholdStr != null && !thresholdStr.isEmpty()) {
                double threshold = Double.parseDouble(thresholdStr);
                log.debug("[AI Chat] 使用配置的相似度阈值: {}", threshold);
                return threshold;
            }
        } catch (NumberFormatException e) {
            log.warn("[AI Chat] 相似度阈值配置格式错误，使用默认值: {}", DEFAULT_SIMILARITY_THRESHOLD);
        }
        log.debug("[AI Chat] 使用默认相似度阈值: {}", DEFAULT_SIMILARITY_THRESHOLD);
        return DEFAULT_SIMILARITY_THRESHOLD;
    }
}
