package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

/**
 * RAG自动回复配置响应DTO
 * @author IAMLZY
 * @date 2026/4/22
 */
@Data
public class RagAutoReplyConfigRespDTO {
    /** RAG回复延时秒数 */
    private Integer ragDelaySeconds;
}
