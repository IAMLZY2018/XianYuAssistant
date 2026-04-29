package com.feijimiao.xianyuassistant.controller;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryConfigReqDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryConfigRespDTO;
import com.feijimiao.xianyuassistant.controller.dto.AutoDeliveryConfigQueryReqDTO;
import com.feijimiao.xianyuassistant.service.AutoDeliveryConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 自动发货配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auto-delivery-config")
@CrossOrigin(origins = "*")
public class AutoDeliveryConfigController {

    @Autowired
    private AutoDeliveryConfigService autoDeliveryConfigService;

    /**
     * 保存或更新自动发货配置
     *
     * @param reqDTO 配置请求DTO
     * @return 配置信息
     */
    @PostMapping("/save")
    public ResultObject<AutoDeliveryConfigRespDTO> saveOrUpdateConfig(@Valid @RequestBody AutoDeliveryConfigReqDTO reqDTO) {
        try {
            log.info("保存自动发货配置请求: xianyuAccountId={}, xyGoodsId={}, deliveryMode={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId(), reqDTO.getDeliveryMode());
            return autoDeliveryConfigService.saveOrUpdateConfig(reqDTO);
        } catch (Exception e) {
            log.error("保存自动发货配置失败", e);
            return ResultObject.failed("保存自动发货配置失败: " + e.getMessage());
        }
    }

    /**
     * 查询自动发货配置
     *
     * @param reqDTO 查询请求DTO
     * @return 配置信息
     */
    @PostMapping("/get")
    public ResultObject<AutoDeliveryConfigRespDTO> getConfig(@Valid @RequestBody AutoDeliveryConfigQueryReqDTO reqDTO) {
        try {
            log.info("查询自动发货配置请求: xianyuAccountId={}, xyGoodsId={}", 
                    reqDTO.getXianyuAccountId(), reqDTO.getXyGoodsId());
            return autoDeliveryConfigService.getConfig(reqDTO);
        } catch (Exception e) {
            log.error("查询自动发货配置失败", e);
            return ResultObject.failed("查询自动发货配置失败: " + e.getMessage());
        }
    }

    /**
     * 根据账号ID查询所有配置
     *
     * @param xianyuAccountId 闲鱼账号ID
     * @return 配置列表
     */
    @PostMapping("/list")
    public ResultObject<List<AutoDeliveryConfigRespDTO>> getConfigsByAccountId(@RequestParam("xianyuAccountId") Long xianyuAccountId) {
        try {
            log.info("查询账号自动发货配置列表请求: xianyuAccountId={}", xianyuAccountId);
            return autoDeliveryConfigService.getConfigsByAccountId(xianyuAccountId);
        } catch (Exception e) {
            log.error("查询账号自动发货配置列表失败", e);
            return ResultObject.failed("查询账号自动发货配置列表失败: " + e.getMessage());
        }
    }

    /**
     * 删除自动发货配置
     *
     * @param xianyuAccountId 闲鱼账号ID
     * @param xyGoodsId 闲鱼商品ID
     * @return 操作结果
     */
    @PostMapping("/delete")
    public ResultObject<Void> deleteConfig(@RequestParam("xianyuAccountId") Long xianyuAccountId,
                                          @RequestParam("xyGoodsId") String xyGoodsId) {
        try {
            log.info("删除自动发货配置请求: xianyuAccountId={}, xyGoodsId={}", xianyuAccountId, xyGoodsId);
            return autoDeliveryConfigService.deleteConfig(xianyuAccountId, xyGoodsId);
        } catch (Exception e) {
            log.error("删除自动发货配置失败", e);
            return ResultObject.failed("删除自动发货配置失败: " + e.getMessage());
        }
    }
}