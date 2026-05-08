package com.feijimiao.xianyuassistant.controller;

import com.feijimiao.xianyuassistant.common.ResultObject;
import com.feijimiao.xianyuassistant.controller.dto.*;
import com.feijimiao.xianyuassistant.service.DataBackupService;
import com.feijimiao.xianyuassistant.service.bo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/backup")
@CrossOrigin(origins = "*")
public class DataBackupController {

    @Autowired
    private DataBackupService dataBackupService;

    @PostMapping("/modules")
    public ResultObject<List<BackupModuleRespDTO>> getModules() {
        try {
            List<BackupModuleRespBO> boList = dataBackupService.getModules();
            List<BackupModuleRespDTO> result = new ArrayList<>();
            for (BackupModuleRespBO bo : boList) {
                BackupModuleRespDTO dto = new BackupModuleRespDTO();
                dto.setModuleKey(bo.getModuleKey());
                dto.setModuleName(bo.getModuleName());
                result.add(dto);
            }
            return ResultObject.success(result);
        } catch (Exception e) {
            log.error("获取备份模块列表失败", e);
            return ResultObject.failed("获取备份模块列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/export")
    public ResultObject<BackupExportRespDTO> exportData(@RequestBody BackupExportReqDTO reqDTO) {
        try {
            BackupExportReqBO reqBO = new BackupExportReqBO();
            reqBO.setModules(reqDTO.getModules());

            BackupExportRespBO respBO = dataBackupService.exportData(reqBO);

            BackupExportRespDTO respDTO = new BackupExportRespDTO();
            respDTO.setJsonData(respBO.getJsonData());
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("导出备份数据失败", e);
            return ResultObject.failed("导出备份数据失败: " + e.getMessage());
        }
    }

    @PostMapping("/import")
    public ResultObject<BackupImportRespDTO> importData(@RequestBody BackupImportReqDTO reqDTO) {
        try {
            if (reqDTO == null || reqDTO.getJsonData() == null || reqDTO.getJsonData().trim().isEmpty()) {
                return ResultObject.validateFailed("备份数据不能为空");
            }

            BackupImportReqBO reqBO = new BackupImportReqBO();
            reqBO.setJsonData(reqDTO.getJsonData());
            reqBO.setModules(reqDTO.getModules());

            BackupImportRespBO respBO = dataBackupService.importData(reqBO);

            BackupImportRespDTO respDTO = new BackupImportRespDTO();
            respDTO.setTotalCount(respBO.getTotalCount());
            respDTO.setSuccessCount(respBO.getSuccessCount());
            respDTO.setFailedModules(respBO.getFailedModules());
            return ResultObject.success(respDTO);
        } catch (Exception e) {
            log.error("导入备份数据失败", e);
            return ResultObject.failed("导入备份数据失败: " + e.getMessage());
        }
    }
}
