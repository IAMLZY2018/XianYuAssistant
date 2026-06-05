package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.entity.XianyuOperationLog;
import com.feijimiao.xianyuassistant.mapper.XianyuOperationLogMapper;
import com.feijimiao.xianyuassistant.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作记录服务实现
 */
@Slf4j
@Service
public class OperationLogServiceImpl implements OperationLogService {
    
    @Autowired
    private XianyuOperationLogMapper operationLogMapper;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    @Async
    public void log(XianyuOperationLog operationLog) {
        try {
            // 设置创建时间
            if (operationLog.getCreateTime() == null) {
                operationLog.setCreateTime(System.currentTimeMillis());
            }
            
            operationLogMapper.insert(operationLog);
            log.debug("操作日志已记录: accountId={}, type={}, module={}", 
                    operationLog.getXianyuAccountId(), 
                    operationLog.getOperationType(), 
                    operationLog.getOperationModule());
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }
    
    @Override
    @Async
    public void log(Long accountId, String operationType, String operationDesc, Integer status) {
        log(accountId, operationType, null, operationDesc, status, 
            null, null, null, null, null, null);
    }
    
    @Override
    @Async
    public void log(Long accountId, String operationType, String operationModule, 
                   String operationDesc, Integer status, String targetType, String targetId,
                   String requestParams, String responseResult, String errorMessage, Integer durationMs) {
        try {
            XianyuOperationLog operationLog = new XianyuOperationLog();
            operationLog.setXianyuAccountId(accountId);
            operationLog.setOperationType(operationType);
            operationLog.setOperationModule(operationModule);
            operationLog.setOperationDesc(operationDesc);
            operationLog.setOperationStatus(status);
            operationLog.setTargetType(targetType);
            operationLog.setTargetId(targetId);
            operationLog.setRequestParams(requestParams);
            operationLog.setResponseResult(responseResult);
            operationLog.setErrorMessage(errorMessage);
            operationLog.setDurationMs(durationMs);
            operationLog.setCreateTime(System.currentTimeMillis());
            
            operationLogMapper.insert(operationLog);
            log.debug("操作日志已记录: accountId={}, type={}, module={}, status={}", 
                    accountId, operationType, operationModule, status);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }
    
    @Override
    public Map<String, Object> queryLogs(Long accountId, String operationType, String operationModule,
                                        Integer operationStatus, Integer page, Integer pageSize) {
        try {
            // 参数校验
            if (page == null || page < 1) {
                page = 1;
            }
            if (pageSize == null || pageSize < 1) {
                pageSize = 20;
            }
            
            int offset = (page - 1) * pageSize;
            
            // 查询列表
            List<XianyuOperationLog> logs = operationLogMapper.selectByPage(
                    accountId, operationType, operationModule, operationStatus, pageSize, offset);
            
            // 查询总数
            Integer total = operationLogMapper.countByCondition(
                    accountId, operationType, operationModule, operationStatus);
            
            // 构建返回结果（注意：前端期望的字段名是logs，不是list）
            Map<String, Object> result = new HashMap<>();
            result.put("logs", logs);  // 修改为logs
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("totalPages", (int) Math.ceil((double) total / pageSize));
            
            return result;
        } catch (Exception e) {
            log.error("查询操作日志失败", e);
            return new HashMap<>();
        }
    }
    
    @Override
    public int deleteOldLogs(int days) {
        try {
            long threshold = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000);
            
            LambdaQueryWrapper<XianyuOperationLog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.lt(XianyuOperationLog::getCreateTime, threshold);
            
            int deleted = operationLogMapper.delete(queryWrapper);
            log.info("已删除{}天前的操作日志: {}条", days, deleted);
            return deleted;
        } catch (Exception e) {
            log.error("删除旧日志失败", e);
            return 0;
        }
    }
}
