package com.feijimiao.xianyuassistant.controller;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.controller.dto.*;
import com.feijimiao.xianyuassistant.service.SysSettingService;
import com.feijimiao.xianyuassistant.service.EmailNotifyService;
import com.feijimiao.xianyuassistant.service.bo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统配置控制器
 * @author IAMLZY
 * @date 2026/4/22
 */
@Slf4j
@RestController
@RequestMapping("/api/setting")
@CrossOrigin(origins = "*")
public class SysSettingController {

    @Autowired
    private SysSettingService sysSettingService;

    @Autowired(required = false)
    private EmailNotifyService emailNotifyService;

    /**
     * 获取配置
     */
    @PostMapping("/get")
    public ResultObject<GetSettingRespDTO> getSetting(@RequestBody GetSettingReqDTO reqDTO) {
        try {
            if (reqDTO == null || reqDTO.getSettingKey() == null || reqDTO.getSettingKey().trim().isEmpty()) {
                return ResultObject.validateFailed("配置键不能为空");
            }

            GetSettingReqBO reqBO = new GetSettingReqBO();
            reqBO.setSettingKey(reqDTO.getSettingKey());

            GetSettingRespBO respBO = sysSettingService.getSetting(reqBO);

            GetSettingRespDTO respDTO = new GetSettingRespDTO();
            if (respBO != null) {
                respDTO.setSettingKey(respBO.getSettingKey());
                respDTO.setSettingValue(respBO.getSettingValue());
                respDTO.setSettingDesc(respBO.getSettingDesc());
            } else {
                respDTO.setSettingKey(reqDTO.getSettingKey());
                respDTO.setSettingValue(null);
                respDTO.setSettingDesc(null);
            }
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("获取配置失败", e);
            return ResultObject.failed("获取配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有配置
     */
    @PostMapping("/list")
    public ResultObject<List<GetSettingRespDTO>> getAllSettings() {
        try {
            List<GetSettingRespBO> respBOList = sysSettingService.getAllSettings();
            List<GetSettingRespDTO> result = new ArrayList<>();

            for (GetSettingRespBO respBO : respBOList) {
                GetSettingRespDTO respDTO = new GetSettingRespDTO();
                respDTO.setSettingKey(respBO.getSettingKey());
                respDTO.setSettingValue(respBO.getSettingValue());
                respDTO.setSettingDesc(respBO.getSettingDesc());
                result.add(respDTO);
            }

            return ResultObject.success(result);
        } catch (Exception e) {
            log.error("获取所有配置失败", e);
            return ResultObject.failed("获取所有配置失败: " + e.getMessage());
        }
    }

    /**
     * 保存配置
     */
    @PostMapping("/save")
    public ResultObject<?> saveSetting(@RequestBody SaveSettingReqDTO reqDTO) {
        try {
            if (reqDTO == null || reqDTO.getSettingKey() == null || reqDTO.getSettingKey().trim().isEmpty()) {
                return ResultObject.validateFailed("配置键不能为空");
            }

            SaveSettingReqBO reqBO = new SaveSettingReqBO();
            reqBO.setSettingKey(reqDTO.getSettingKey());
            reqBO.setSettingValue(reqDTO.getSettingValue());
            reqBO.setSettingDesc(reqDTO.getSettingDesc());

            sysSettingService.saveSetting(reqBO);
            return ResultObject.success(null);
        } catch (Exception e) {
            log.error("保存配置失败", e);
            return ResultObject.failed("保存配置失败: " + e.getMessage());
        }
    }

    /**
     * 删除配置
     */
    @PostMapping("/delete")
    public ResultObject<?> deleteSetting(@RequestBody GetSettingReqDTO reqDTO) {
        try {
            if (reqDTO == null || reqDTO.getSettingKey() == null || reqDTO.getSettingKey().trim().isEmpty()) {
                return ResultObject.validateFailed("配置键不能为空");
            }

            sysSettingService.deleteSetting(reqDTO.getSettingKey());
            return ResultObject.success(null);
        } catch (Exception e) {
            log.error("删除配置失败", e);
            return ResultObject.failed("删除配置失败: " + e.getMessage());
        }
    }

    /**
     * 测试邮箱配置
     */
    @PostMapping("/testEmail")
    public ResultObject<?> testEmail() {
        try {
            if (emailNotifyService == null) {
                return ResultObject.failed("邮件服务未初始化");
            }
            if (!emailNotifyService.isEmailConfigured()) {
                return ResultObject.failed("邮箱配置不完整，请先配置SMTP信息");
            }
            String error = emailNotifyService.sendTestEmail();
            if (error == null) {
                return ResultObject.success("测试邮件发送成功，请检查收件箱");
            } else {
                return ResultObject.failed(error);
            }
        } catch (Exception e) {
            log.error("测试邮箱失败", e);
            return ResultObject.failed("测试邮箱失败: " + e.getMessage());
        }
    }
}
