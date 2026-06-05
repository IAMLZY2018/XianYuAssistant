package com.feijimiao.xianyuassistant.controller.dto;

import lombok.Data;

/**
 * 保存配置请求DTO
 * @author IAMLZY
 * @date 2026/4/22
 */
@Data
public class SaveSettingReqDTO {
    private String settingKey;
    private String settingValue;
    private String settingDesc;
}
