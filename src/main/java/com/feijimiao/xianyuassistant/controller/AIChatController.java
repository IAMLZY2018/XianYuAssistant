package com.feijimiao.xianyuassistant.controller;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.controller.dto.ChatWithAIReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.PutNewDataToRAGReqDTO;
import com.feijimiao.xianyuassistant.service.AIService;
import com.feijimiao.xianyuassistant.service.bo.RAGDataRespBO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * @author IAMLZY
 * @date 2026/4/12 00:16
 * @description AI对话控制器，仅在ai.enabled=true时加载
 */
@RestController
@RequestMapping("/ai")
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class AIChatController {
    @Autowired
    private AIService  aiService;

    @PostMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatWithAi(@RequestBody ChatWithAIReqDTO chatWithAIReqDTO){
        return aiService.chatByRAG(chatWithAIReqDTO.getMsg(),chatWithAIReqDTO.getGoodsId());
    }


    @PostMapping("/putNewData")
    public ResultObject<?> putNewData(@RequestBody PutNewDataToRAGReqDTO putNewDataToRAGReqDTO) {
        aiService.putDataToRAG(putNewDataToRAGReqDTO.getContent(), putNewDataToRAGReqDTO.getGoodsId());
        return ResultObject.success(null);
    }

    @PostMapping("/queryRAGData")
    public ResultObject<List<RAGDataRespBO>> queryRAGData(@RequestBody PutNewDataToRAGReqDTO req) {
        List<RAGDataRespBO> data = aiService.queryRAGDataBygoodsId(req.getGoodsId());
        return ResultObject.success(data);
    }

}
