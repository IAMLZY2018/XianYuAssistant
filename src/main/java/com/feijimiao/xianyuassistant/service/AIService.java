package com.feijimiao.xianyuassistant.service;

import com.feijimiao.xianyuassistant.service.bo.RAGDataRespBO;
import com.feijimiao.xianyuassistant.service.bo.RAGReplyResult;
import org.antlr.v4.runtime.TokenStream;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * @author IAMLZY
 * @date 2026/4/10 22:26
 * @description
 */

public interface AIService {

    Flux<String> chatByRAG(String msg,String goodsId);

    /**
     * RAG聊天并返回命中资料详情
     */
    RAGReplyResult chatByRAGWithDetails(String msg, String goodsId);

    void putDataToRAG(String content,String goodsId);

    List<RAGDataRespBO> queryRAGDataBygoodsId(String goodsId);

    void deleteRAGDataByDocumentId(String documentId);
}
