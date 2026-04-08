package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryConfig;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoDeliveryConfigMapper;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryConfigReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryConfigRespDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryConfigQueryReqDTO;
import com.feijimiao.xianyuassistant.service.AutoDeliveryConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 自动发货配置服务实现
 */
@Slf4j
@Service
public class AutoDeliveryConfigServiceImpl implements AutoDeliveryConfigService {
    
    @Autowired
    private XianyuGoodsAutoDeliveryConfigMapper autoDeliveryConfigMapper;
    
    @Override
    public ResultObject<AutoDeliveryConfigRespDTO> saveOrUpdateConfig(AutoDeliveryConfigReqDTO reqDTO) {
        try {
            // 检查是否已存在配置
            XianyuGoodsAutoDeliveryConfig existingConfig = autoDeliveryConfigMapper
                    .findByAccountIdAndGoodsId(reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());
            
            XianyuGoodsAutoDeliveryConfig config;
            if (existingConfig != null) {
                // 更新现有配置
                config = existingConfig;
                config.setType(reqDTO.getType());
                config.setAutoDeliveryContent(reqDTO.getAutoDeliveryContent());
                config.setXianyuGoodsId(reqDTO.getXianyuGoodsId());
                config.setAutoConfirmShipment(reqDTO.getAutoConfirmShipment());
                
                autoDeliveryConfigMapper.updateById(config);
                log.info("更新自动发货配置成功，ID: {}", config.getId());
            } else {
                // 创建新配置
                config = new XianyuGoodsAutoDeliveryConfig();
                BeanUtils.copyProperties(reqDTO, config);
                
                autoDeliveryConfigMapper.insert(config);
                log.info("创建自动发货配置成功，ID: {}", config.getId());
            }
            
            AutoDeliveryConfigRespDTO respDTO = new AutoDeliveryConfigRespDTO();
            BeanUtils.copyProperties(config, respDTO);
            
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("保存自动发货配置失败", e);
            return ResultObject.failed("保存自动发货配置失败: " + e.getMessage());
        }
    }
    
    @Override
    public ResultObject<AutoDeliveryConfigRespDTO> getConfig(AutoDeliveryConfigQueryReqDTO reqDTO) {
        try {
            log.info("开始查询自动发货配置: xianyuAccountId={}, xyGoodsId={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());
            
            XianyuGoodsAutoDeliveryConfig config;
            
            if (reqDTO.getXyGoodsId() != null && !reqDTO.getXyGoodsId().trim().isEmpty()) {
                // 根据账号ID和商品ID查询
                config = autoDeliveryConfigMapper.findByAccountIdAndGoodsId(
                        reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());
                log.info("根据账号ID和商品ID查询结果: {}", config != null ? "找到配置" : "未找到配置");
            } else {
                // 只根据账号ID查询第一个配置（用于页面初始化）
                List<XianyuGoodsAutoDeliveryConfig> configs = autoDeliveryConfigMapper
                        .findByAccountId(reqDTO.getXianyuAccountId());
                config = configs.isEmpty() ? null : configs.get(0);
                log.info("根据账号ID查询结果: 找到{}条配置", configs.size());
            }
            
            if (config == null) {
                log.info("未找到匹配的配置，返回null");
                return ResultObject.success(null);
            }
            
            // 检查时间字段是否为null
            if (config.getCreateTime() == null) {
                log.warn("配置记录的创建时间为空: id={}", config.getId());
            }
            if (config.getUpdateTime() == null) {
                log.warn("配置记录的更新时间为空: id={}", config.getId());
            }
            
            AutoDeliveryConfigRespDTO respDTO = new AutoDeliveryConfigRespDTO();
            BeanUtils.copyProperties(config, respDTO);
            
            log.info("查询自动发货配置成功: id={}, type={}, hasContent={}", 
                    respDTO.getId(), respDTO.getType(), 
                    respDTO.getAutoDeliveryContent() != null && !respDTO.getAutoDeliveryContent().isEmpty());
            
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("查询自动发货配置失败", e);
            return ResultObject.failed("查询自动发货配置失败: " + e.getMessage());
        }
    }
    
    @Override
    public ResultObject<List<AutoDeliveryConfigRespDTO>> getConfigsByAccountId(Long xianyuAccountId) {
        try {
            List<XianyuGoodsAutoDeliveryConfig> configs = autoDeliveryConfigMapper
                    .findByAccountId(xianyuAccountId);
            
            List<AutoDeliveryConfigRespDTO> respDTOs = configs.stream()
                    .map(config -> {
                        AutoDeliveryConfigRespDTO respDTO = new AutoDeliveryConfigRespDTO();
                        BeanUtils.copyProperties(config, respDTO);
                        return respDTO;
                    })
                    .collect(Collectors.toList());
            
            return ResultObject.success(respDTOs);
        } catch (Exception e) {
            log.error("查询账号自动发货配置列表失败", e);
            return ResultObject.failed("查询账号自动发货配置列表失败: " + e.getMessage());
        }
    }
    
    @Override
    public ResultObject<Void> deleteConfig(Long xianyuAccountId, String xyGoodsId) {
        try {
            LambdaQueryWrapper<XianyuGoodsAutoDeliveryConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(XianyuGoodsAutoDeliveryConfig::getXianyuAccountId, xianyuAccountId)
                   .eq(XianyuGoodsAutoDeliveryConfig::getXyGoodsId, xyGoodsId);
            
            int deletedCount = autoDeliveryConfigMapper.delete(wrapper);
            
            if (deletedCount > 0) {
                log.info("删除自动发货配置成功，账号ID: {}, 商品ID: {}", xianyuAccountId, xyGoodsId);
                return ResultObject.success(null);
            } else {
                return ResultObject.failed("未找到对应的自动发货配置");
            }
        } catch (Exception e) {
            log.error("删除自动发货配置失败", e);
            return ResultObject.failed("删除自动发货配置失败: " + e.getMessage());
        }
    }
}