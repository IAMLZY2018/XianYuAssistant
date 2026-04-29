package com.feijimiao.xianyuassistant.service.bo;

import lombok.Data;

/**
 * 保存配置请求BO
 * @author IAMLZY
 * @date 2026/4/22
 */
@Data
public class SaveSettingReqBO {
    private String settingKey;
    private String settingValue;
    private String settingDesc;
}
