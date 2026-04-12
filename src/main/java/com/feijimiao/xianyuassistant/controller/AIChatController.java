package com.feijimiao.xianyuassistant.controller;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.controller.dto.ChatWithAIReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.PutNewDataToRAGReqDTO;
import com.feijimiao.xianyuassistant.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * @author IAMLZY
 * @date 2026/4/12 00:16
 * @description
 */
@RestController
@RequestMapping("/ai")
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

}
