package com.feijimiao.xianyuassistant.service.bo;

import lombok.Data;

/**
 * 获取配置响应BO
 * @author IAMLZY
 * @date 2026/4/22
 */
@Data
public class GetSettingRespBO {
    private String settingKey;
    private String settingValue;
    private String settingDesc;
}
