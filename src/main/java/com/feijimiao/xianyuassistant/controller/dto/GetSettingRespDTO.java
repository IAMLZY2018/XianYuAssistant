package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

/**
 * 获取配置响应DTO
 * @author IAMLZY
 * @date 2026/4/22
 */
@Data
public class GetSettingRespDTO {
    private String settingKey;
    private String settingValue;
    private String settingDesc;
}
