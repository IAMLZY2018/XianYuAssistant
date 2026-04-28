package com.feijimiao.xianyuassistant.controller;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.config.rag.DynamicAIChatClientManager;
import com.feijimiao.xianyuassistant.controller.dto.ChatWithAIReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.DeleteRAGDataReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.PutNewDataToRAGReqDTO;
import com.feijimiao.xianyuassistant.service.AIService;
import com.feijimiao.xianyuassistant.service.GoodsInfoService;
import com.feijimiao.xianyuassistant.service.bo.RAGDataRespBO;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsConfigMapper;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI对话控制器
 * 始终加载，AI功能未配置时自动降级
 *
 * @author IAMLZY
 * @date 2026/4/12 00:16
 */
@RestController
@RequestMapping("/ai")
public class AIChatController {
    @Autowired
    private AIService aiService;

    @Autowired
    private DynamicAIChatClientManager dynamicAIChatClientManager;
    
    @Autowired
    private GoodsInfoService goodsInfoService;
    
    @Autowired
    private XianyuGoodsConfigMapper goodsConfigMapper;

    /**
     * AI对话（流式返回）
     * 未配置API Key时返回降级提示
     */
    @PostMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatWithAi(@RequestBody ChatWithAIReqDTO chatWithAIReqDTO) {
        return aiService.chatByRAG(chatWithAIReqDTO.getMsg(), chatWithAIReqDTO.getGoodsId());
    }

    /**
     * AI对话测试接口（流式）- 与自动回复流程一致
     * 携带固定资料和商品详情，用于测试提示词与资料效果
     */
    @PostMapping(path = "/chatTest", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatTestWithAi(@RequestBody ChatTestReqDTO req) {
        String fixedMaterial = null;
        String goodsDetail = null;
        
        if (req.getAccountId() != null && req.getGoodsId() != null) {
            XianyuGoodsConfig config = goodsConfigMapper.selectByAccountAndGoodsId(req.getAccountId(), req.getGoodsId());
            if (config != null) {
                fixedMaterial = config.getFixedMaterial();
            }
            
            String detailInfo = goodsInfoService.getDetailInfoByGoodsId(req.getGoodsId());
            if (detailInfo != null && !detailInfo.isEmpty()) {
                goodsDetail = detailInfo;
            }
        }
        
        return aiService.chatByRAGWithFixedMaterialStream(req.getMsg(), req.getGoodsId(), fixedMaterial, goodsDetail);
    }

    /**
     * AI状态检测接口
     * 返回AI服务是否可用、配置状态等信息
     */
    @PostMapping("/status")
    public ResultObject<AIStatusRespDTO> getAIStatus() {
        DynamicAIChatClientManager.AIStatusInfo statusInfo = dynamicAIChatClientManager.getStatusInfo();

        AIStatusRespDTO respDTO = new AIStatusRespDTO();
        respDTO.setEnabled(statusInfo.isEnabled());
        respDTO.setAvailable(statusInfo.isAvailable());
        respDTO.setApiKeyConfigured(statusInfo.isApiKeyConfigured());
        respDTO.setMessage(statusInfo.getMessage());
        respDTO.setBaseUrl(statusInfo.getBaseUrl());
        respDTO.setModel(statusInfo.getModel());

        return ResultObject.success(respDTO);
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

    @PostMapping("/deleteRAGData")
    public ResultObject<?> deleteRAGData(@RequestBody DeleteRAGDataReqDTO req) {
        aiService.deleteRAGDataByDocumentId(req.getDocumentId());
        return ResultObject.success(null);
    }

    @PostMapping("/saveFixedMaterial")
    public ResultObject<?> saveFixedMaterial(@RequestBody FixedMaterialReqDTO req) {
        goodsConfigMapper.updateFixedMaterial(req.getAccountId(), req.getGoodsId(), req.getFixedMaterial());
        return ResultObject.success(null);
    }

    @PostMapping("/getFixedMaterial")
    public ResultObject<FixedMaterialRespDTO> getFixedMaterial(@RequestBody FixedMaterialReqDTO req) {
        XianyuGoodsConfig config = goodsConfigMapper.selectByAccountAndGoodsId(req.getAccountId(), req.getGoodsId());
        FixedMaterialRespDTO resp = new FixedMaterialRespDTO();
        if (config != null) {
            resp.setFixedMaterial(config.getFixedMaterial());
        }
        return ResultObject.success(resp);
    }

    @PostMapping("/syncDetailToFixedMaterial")
    public ResultObject<?> syncDetailToFixedMaterial(@RequestBody FixedMaterialReqDTO req) {
        String detailInfo = goodsInfoService.getDetailInfoByGoodsId(req.getGoodsId());
        if (detailInfo != null && !detailInfo.isEmpty()) {
            goodsConfigMapper.updateFixedMaterial(req.getAccountId(), req.getGoodsId(), detailInfo);
            return ResultObject.success(null);
        } else {
            return ResultObject.failed("商品详情为空，无法同步");
        }
    }

    public static class FixedMaterialReqDTO {
        private Long accountId;
        private String goodsId;
        private String fixedMaterial;

        public Long getAccountId() { return accountId; }
        public void setAccountId(Long accountId) { this.accountId = accountId; }
        public String getGoodsId() { return goodsId; }
        public void setGoodsId(String goodsId) { this.goodsId = goodsId; }
        public String getFixedMaterial() { return fixedMaterial; }
        public void setFixedMaterial(String fixedMaterial) { this.fixedMaterial = fixedMaterial; }
    }

    public static class FixedMaterialRespDTO {
        private String fixedMaterial;

        public String getFixedMaterial() { return fixedMaterial; }
        public void setFixedMaterial(String fixedMaterial) { this.fixedMaterial = fixedMaterial; }
    }

    public static class ChatTestReqDTO {
        private Long accountId;
        private String goodsId;
        private String msg;

        public Long getAccountId() { return accountId; }
        public void setAccountId(Long accountId) { this.accountId = accountId; }
        public String getGoodsId() { return goodsId; }
        public void setGoodsId(String goodsId) { this.goodsId = goodsId; }
        public String getMsg() { return msg; }
        public void setMsg(String msg) { this.msg = msg; }
    }

    /**
     * AI状态响应DTO
     */
    public static class AIStatusRespDTO {
        private boolean enabled;
        private boolean available;
        private boolean apiKeyConfigured;
        private String message;
        private String baseUrl;
        private String model;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
        public boolean isApiKeyConfigured() { return apiKeyConfigured; }
        public void setApiKeyConfigured(boolean apiKeyConfigured) { this.apiKeyConfigured = apiKeyConfigured; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }
}
