package com.feijimiao.xianyuassistant.service;

import com.feijimiao.xianyuassistant.service.bo.*;

import java.util.List;

/**
 * 系统配置服务接口
 * @author IAMLZY
 * @date 2026/4/22
 */
public interface SysSettingService {

    /**
     * 获取配置值
     */
    String getSettingValue(String settingKey);

    /**
     * 获取配置详情
     */
    GetSettingRespBO getSetting(GetSettingReqBO reqBO);

    /**
     * 获取所有配置
     */
    List<GetSettingRespBO> getAllSettings();

    /**
     * 保存配置
     */
    void saveSetting(SaveSettingReqBO reqBO);

    /**
     * 删除配置
     */
    void deleteSetting(String settingKey);
}
