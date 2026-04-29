package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.feijimiao.xianyuassistant.config.rag.DynamicAIChatClientManager;
import com.feijimiao.xianyuassistant.entity.XianyuSysSetting;
import com.feijimiao.xianyuassistant.mapper.XianyuSysSettingMapper;
import com.feijimiao.xianyuassistant.service.SysSettingService;
import com.feijimiao.xianyuassistant.service.bo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 系统配置服务实现
 * @author IAMLZY
 * @date 2026/4/22
 */
@Slf4j
@Service
public class SysSettingServiceImpl implements SysSettingService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /** AI相关配置键，变更时需要触发ChatClient重建 */
    private static final Set<String> AI_RELATED_KEYS = Set.of("ai_api_key", "ai_base_url", "ai_model");

    @Autowired
    private XianyuSysSettingMapper sysSettingMapper;

    @Autowired
    @Lazy
    private DynamicAIChatClientManager dynamicAIChatClientManager;

    @Override
    public String getSettingValue(String settingKey) {
        if (settingKey == null || settingKey.trim().isEmpty()) {
            return null;
        }

        LambdaQueryWrapper<XianyuSysSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XianyuSysSetting::getSettingKey, settingKey.trim());
        XianyuSysSetting setting = sysSettingMapper.selectOne(wrapper);

        return setting != null ? setting.getSettingValue() : null;
    }

    @Override
    public GetSettingRespBO getSetting(GetSettingReqBO reqBO) {
        if (reqBO == null || reqBO.getSettingKey() == null || reqBO.getSettingKey().trim().isEmpty()) {
            return null;
        }

        LambdaQueryWrapper<XianyuSysSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XianyuSysSetting::getSettingKey, reqBO.getSettingKey().trim());
        XianyuSysSetting setting = sysSettingMapper.selectOne(wrapper);

        if (setting == null) {
            return null;
        }

        GetSettingRespBO respBO = new GetSettingRespBO();
        respBO.setSettingKey(setting.getSettingKey());
        respBO.setSettingValue(setting.getSettingValue());
        respBO.setSettingDesc(setting.getSettingDesc());
        return respBO;
    }

    @Override
    public List<GetSettingRespBO> getAllSettings() {
        List<XianyuSysSetting> settings = sysSettingMapper.selectList(null);
        List<GetSettingRespBO> result = new ArrayList<>();

        for (XianyuSysSetting setting : settings) {
            GetSettingRespBO respBO = new GetSettingRespBO();
            respBO.setSettingKey(setting.getSettingKey());
            respBO.setSettingValue(setting.getSettingValue());
            respBO.setSettingDesc(setting.getSettingDesc());
            result.add(respBO);
        }

        return result;
    }

    @Override
    public void saveSetting(SaveSettingReqBO reqBO) {
        if (reqBO == null || reqBO.getSettingKey() == null || reqBO.getSettingKey().trim().isEmpty()) {
            throw new RuntimeException("配置键不能为空");
        }

        String now = LocalDateTime.now().format(FORMATTER);

        // 查询是否已存在
        LambdaQueryWrapper<XianyuSysSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XianyuSysSetting::getSettingKey, reqBO.getSettingKey().trim());
        XianyuSysSetting existing = sysSettingMapper.selectOne(wrapper);

        if (existing != null) {
            // 更新
            existing.setSettingValue(reqBO.getSettingValue());
            existing.setSettingDesc(reqBO.getSettingDesc());
            existing.setUpdatedTime(now);
            sysSettingMapper.updateById(existing);
            log.info("[SysSetting] 更新配置成功: key={}", reqBO.getSettingKey());
        } else {
            // 新增
            XianyuSysSetting setting = new XianyuSysSetting();
            setting.setSettingKey(reqBO.getSettingKey().trim());
            setting.setSettingValue(reqBO.getSettingValue());
            setting.setSettingDesc(reqBO.getSettingDesc());
            setting.setCreatedTime(now);
            setting.setUpdatedTime(now);
            sysSettingMapper.insert(setting);
            log.info("[SysSetting] 新增配置成功: key={}", reqBO.getSettingKey());
        }

        // 如果是AI相关配置，触发ChatClient重建
        if (AI_RELATED_KEYS.contains(reqBO.getSettingKey().trim())) {
            log.info("[SysSetting] AI配置变更，触发ChatClient重建: key={}", reqBO.getSettingKey());
            dynamicAIChatClientManager.forceRebuild();
        }
    }

    @Override
    public void deleteSetting(String settingKey) {
        if (settingKey == null || settingKey.trim().isEmpty()) {
            throw new RuntimeException("配置键不能为空");
        }

        LambdaQueryWrapper<XianyuSysSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(XianyuSysSetting::getSettingKey, settingKey.trim());
        sysSettingMapper.delete(wrapper);
        log.info("[SysSetting] 删除配置成功: key={}", settingKey);

        // 如果是AI相关配置，触发ChatClient重建
        if (AI_RELATED_KEYS.contains(settingKey.trim())) {
            log.info("[SysSetting] AI配置删除，触发ChatClient重建: key={}", settingKey);
            dynamicAIChatClientManager.forceRebuild();
        }
    }
}
