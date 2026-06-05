package com.feijimiao.xianyuassistant.utils;

import com.feijimiao.xianyuassistant.entity.XianyuAccount;
import com.feijimiao.xianyuassistant.mapper.XianyuAccountMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 账号显示名称工具类
 * 用于在日志中显示账号的备注和ID
 */
@Slf4j
@Component
public class AccountDisplayNameUtils {

    @Autowired
    private XianyuAccountMapper accountMapper;
    
    // 账号信息缓存（accountId -> accountNote）
    private final Map<Long, String> accountNoteCache = new ConcurrentHashMap<>();
    
    /**
     * 获取账号显示名称
     * 格式：备注（ID）
     * 
     * @param accountId 账号ID
     * @return 显示名称
     */
    public String getDisplayName(Long accountId) {
        if (accountId == null) {
            return "未知账号";
        }
        
        // 尝试从缓存获取备注
        String accountNote = accountNoteCache.get(accountId);
        
        if (accountNote == null) {
            // 缓存未命中，从数据库查询
            try {
                XianyuAccount account = accountMapper.selectById(accountId);
                if (account != null) {
                    accountNote = account.getAccountNote();
                    // 更新缓存
                    if (accountNote != null) {
                        accountNoteCache.put(accountId, accountNote);
                    }
                }
            } catch (Exception e) {
                log.debug("获取账号备注失败: accountId={}, error={}", accountId, e.getMessage());
            }
        }
        
        // 构建显示名称
        if (accountNote != null && !accountNote.isEmpty()) {
            return String.format("%s（%d）", accountNote, accountId);
        } else {
            return String.format("账号%d", accountId);
        }
    }
    
    /**
     * 获取账号显示名称（String类型的accountId）
     * 
     * @param accountId 账号ID（字符串）
     * @return 显示名称
     */
    public String getDisplayName(String accountId) {
        try {
            Long id = Long.parseLong(accountId);
            return getDisplayName(id);
        } catch (NumberFormatException e) {
            return "账号" + accountId;
        }
    }
    
    /**
     * 更新账号备注缓存
     * 
     * @param accountId 账号ID
     * @param accountNote 账号备注
     */
    public void updateCache(Long accountId, String accountNote) {
        if (accountId != null && accountNote != null) {
            accountNoteCache.put(accountId, accountNote);
        }
    }
    
    /**
     * 清除账号备注缓存
     * 
     * @param accountId 账号ID
     */
    public void clearCache(Long accountId) {
        if (accountId != null) {
            accountNoteCache.remove(accountId);
        }
    }
    
    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        accountNoteCache.clear();
    }
}
