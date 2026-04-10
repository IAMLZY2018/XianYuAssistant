package com.feijimiao.xianyuassistant.service.impl;

import com.feijimiao.xianyuassistant.entity.XianyuAccount;
import com.feijimiao.xianyuassistant.entity.XianyuCookie;
import com.feijimiao.xianyuassistant.mapper.XianyuAccountMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuCookieMapper;
import com.feijimiao.xianyuassistant.service.CookieRefreshService;
import com.feijimiao.xianyuassistant.service.OperationLogService;
import com.feijimiao.xianyuassistant.service.TokenRefreshService;
import com.feijimiao.xianyuassistant.service.WebSocketTokenService;
import com.feijimiao.xianyuassistant.utils.XianyuSignUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Token刷新服务实现
 * 
 * <p>功能：</p>
 * <ul>
 *   <li>定期刷新_m_h5_tk token（每2小时）</li>
 *   <li>定期刷新websocket_token（每12小时）</li>
 *   <li>监控token过期时间</li>
 *   <li>自动重新获取过期的token</li>
 * </ul>
 */
@Slf4j
@Service
public class TokenRefreshServiceImpl implements TokenRefreshService {
    
    @Autowired
    private XianyuAccountMapper accountMapper;
    
    @Autowired
    private XianyuCookieMapper cookieMapper;
    
    @Autowired
    private WebSocketTokenService webSocketTokenService;
    
    @Autowired
    private OperationLogService operationLogService;
    
    @Autowired
    private CookieRefreshService cookieRefreshService;
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    /**
     * 闲鱼API地址（用于刷新_m_h5_tk）
     */
    private static final String API_H5_TK = "https://h5api.m.goofish.com/h5/mtop.gaia.nodejs.gaia.idle.data.gw.v2.index.get/1.0/";
    
    /**
     * 刷新_m_h5_tk token
     * 通过调用闲鱼API，服务器会返回新的_m_h5_tk
     * 
     * 参考Python逻辑：
     * 1. 调用H5 API获取新的_m_h5_tk
     * 2. 如果失败，重试最多2次
     * 3. 重试失败后，调用hasLogin刷新Cookie
     * 4. hasLogin成功后，重新尝试获取_m_h5_tk
     */
    @Override
    public boolean refreshMh5tkToken(Long accountId) {
        return refreshMh5tkTokenWithRetry(accountId, 0);
    }
    
    /**
     * 刷新_m_h5_tk token（带重试机制）
     * 参考Python XianyuApis.get_token的重试逻辑
     * 
     * @param accountId 账号ID
     * @param retryCount 当前重试次数
     * @return 是否成功
     */
    private boolean refreshMh5tkTokenWithRetry(Long accountId, int retryCount) {
        try {
            log.info("【账号{}】开始刷新_m_h5_tk token... (重试次数: {})", accountId, retryCount);
            
            // 1. 获取当前Cookie
            XianyuCookie cookie = cookieMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<XianyuCookie>()
                            .eq(XianyuCookie::getXianyuAccountId, accountId)
            );
            if (cookie == null || cookie.getCookieText() == null) {
                log.warn("【账号{}】未找到Cookie，无法刷新token", accountId);
                return false;
            }
            
            String cookieStr = cookie.getCookieText();
            Map<String, String> cookies = XianyuSignUtils.parseCookies(cookieStr);
            
            // 2. 构建请求：获取新的_m_h5_tk
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_H5_TK))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Referer", "https://market.m.goofish.com/")
                    .header("Cookie", cookieStr)
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // 3. 提取新的_m_h5_tk
            List<String> setCookieHeaders = response.headers().allValues("Set-Cookie");
            boolean updated = false;
            
            for (String setCookie : setCookieHeaders) {
                String[] parts = setCookie.split(";")[0].split("=", 2);
                if (parts.length == 2 && "_m_h5_tk".equals(parts[0])) {
                    String newMh5tk = parts[1];
                    cookies.put("_m_h5_tk", newMh5tk);
                    
                    // 更新数据库
                    String newCookieStr = XianyuSignUtils.formatCookies(cookies);
                    cookie.setCookieText(newCookieStr);
                    cookie.setMH5Tk(newMh5tk);
                    cookieMapper.updateById(cookie);
                    
                    log.info("【账号{}】✅ _m_h5_tk token刷新成功: {}", accountId, 
                            newMh5tk.substring(0, Math.min(20, newMh5tk.length())));
                    
                    // 记录操作日志
                    operationLogService.log(accountId,
                        com.feijimiao.xianyuassistant.constants.OperationConstants.Type.REFRESH,
                        com.feijimiao.xianyuassistant.constants.OperationConstants.Module.TOKEN,
                        "_m_h5_tk Token刷新成功",
                        com.feijimiao.xianyuassistant.constants.OperationConstants.Status.SUCCESS,
                        com.feijimiao.xianyuassistant.constants.OperationConstants.TargetType.TOKEN,
                        String.valueOf(accountId),
                        null, null, null, null);
                    
                    updated = true;
                    break;
                }
            }
            
            if (updated) {
                return true;
            }
            
            // 4. 响应中未包含新的_m_h5_tk，进入失败处理
            log.warn("【账号{}】⚠️ 响应中未包含新的_m_h5_tk", accountId);
            return handleMh5tkRefreshFailure(accountId, retryCount, "响应中未包含新Token");
            
        } catch (Exception e) {
            log.error("【账号{}】刷新_m_h5_tk token失败", accountId, e);
            return handleMh5tkRefreshFailure(accountId, retryCount, "异常: " + e.getMessage());
        }
    }
    
    /**
     * 处理_m_h5_tk刷新失败的情况
     * 参考Python XianyuApis.get_token的失败处理逻辑
     * 
     * @param accountId 账号ID
     * @param retryCount 当前重试次数
     * @param reason 失败原因
     * @return 是否成功
     */
    private boolean handleMh5tkRefreshFailure(Long accountId, int retryCount, String reason) {
        // 参考Python: retry_count < 2 时直接重试
        if (retryCount < 2) {
            log.warn("【账号{}】_m_h5_tk刷新失败({})，准备重试... (重试次数: {}/2)", 
                    accountId, reason, retryCount + 1);
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return refreshMh5tkTokenWithRetry(accountId, retryCount + 1);
        }
        
        // 参考Python: retry_count >= 2 时，调用hasLogin刷新Cookie后重试
        log.warn("【账号{}】_m_h5_tk刷新重试已达上限，尝试通过hasLogin刷新Cookie...", accountId);
        return refreshMh5tkViaHasLogin(accountId, 0);
    }
    
    /**
     * 通过hasLogin刷新Cookie后重新获取_m_h5_tk
     * 参考Python: get_token中retry_count >= 2时的逻辑
     * 
     * @param accountId 账号ID
     * @param hasLoginRetryCount hasLogin重试次数
     * @return 是否成功
     */
    private boolean refreshMh5tkViaHasLogin(Long accountId, int hasLoginRetryCount) {
        if (hasLoginRetryCount >= 2) {
            log.error("【账号{}】hasLogin刷新重试次数已达上限，Cookie已彻底过期", accountId);
            
            // 更新Cookie状态为过期
            cookieMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<XianyuCookie>()
                            .eq(XianyuCookie::getXianyuAccountId, accountId)
                            .set(XianyuCookie::getCookieStatus, 2)
            );
            
            // 记录操作日志
            operationLogService.log(accountId,
                com.feijimiao.xianyuassistant.constants.OperationConstants.Type.REFRESH,
                com.feijimiao.xianyuassistant.constants.OperationConstants.Module.TOKEN,
                "_m_h5_tk Token刷新失败：Cookie过期且自动刷新失败",
                com.feijimiao.xianyuassistant.constants.OperationConstants.Status.FAIL,
                com.feijimiao.xianyuassistant.constants.OperationConstants.TargetType.TOKEN,
                String.valueOf(accountId),
                null, null, "Cookie过期且自动刷新失败", null);
            
            return false;
        }
        
        log.info("【账号{}】开始通过hasLogin刷新Cookie... (重试次数: {}/2)", 
                accountId, hasLoginRetryCount);
        
        try {
            // 调用CookieRefreshService的checkLoginStatus方法（即hasLogin）
            boolean refreshSuccess = cookieRefreshService.checkLoginStatus(accountId);
            
            if (refreshSuccess) {
                log.info("【账号{}】hasLogin成功，Cookie已刷新，重新获取_m_h5_tk（重置重试计数）", accountId);
                
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // 重置retryCount为0，重新开始获取_m_h5_tk流程
                return refreshMh5tkTokenWithRetry(accountId, 0);
            } else {
                log.warn("【账号{}】hasLogin失败", accountId);
            }
        } catch (Exception e) {
            log.error("【账号{}】hasLogin刷新过程发生异常", accountId, e);
        }
        
        // hasLogin失败，重试
        return refreshMh5tkViaHasLogin(accountId, hasLoginRetryCount + 1);
    }
    
    /**
     * 刷新WebSocket token
     */
    @Override
    public boolean refreshWebSocketToken(Long accountId) {
        try {
            log.info("【账号{}】开始刷新WebSocket token...", accountId);
            
            // 调用WebSocketTokenService重新获取token
            String newToken = webSocketTokenService.refreshToken(accountId);
            
            if (newToken != null && !newToken.isEmpty()) {
                log.info("【账号{}】✅ WebSocket token刷新成功", accountId);
                return true;
            } else {
                log.warn("【账号{}】⚠️ WebSocket token刷新失败", accountId);
                return false;
            }
            
        } catch (Exception e) {
            log.error("【账号{}】刷新WebSocket token失败", accountId, e);
            return false;
        }
    }
    
    /**
     * 检查token是否需要刷新
     */
    @Override
    public boolean needsRefresh(Long accountId) {
        try {
            XianyuCookie cookie = cookieMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<XianyuCookie>()
                            .eq(XianyuCookie::getXianyuAccountId, accountId)
            );
            if (cookie == null) {
                return false;
            }
            
            // 检查WebSocket token是否即将过期（提前1小时刷新）
            if (cookie.getTokenExpireTime() != null) {
                long currentTime = System.currentTimeMillis();
                long expireTime = cookie.getTokenExpireTime();
                long oneHour = 60 * 60 * 1000;
                
                if (expireTime - currentTime < oneHour) {
                    log.info("【账号{}】WebSocket token即将过期，需要刷新", accountId);
                    return true;
                }
            }
            
            // _m_h5_tk没有明确的过期时间，建议每2小时刷新一次
            // 这里可以通过记录上次刷新时间来判断
            
            return false;
            
        } catch (Exception e) {
            log.error("【账号{}】检查token状态失败", accountId, e);
            return false;
        }
    }
    
    /**
     * 定时任务：刷新所有账号的_m_h5_tk token
     * 与Python保持一致：Python没有单独的_m_h5_tk定时刷新，而是在API调用时处理
     * 这里保留定时刷新作为兜底机制，间隔设置为较长的时间
     * 基础间隔2小时（120分钟）
     */
    @Scheduled(fixedDelay = 120 * 60 * 1000, initialDelay = 10 * 60 * 1000)
    public void scheduledRefreshMh5tk() {
        try {
            log.info("🔄 开始刷新所有账号的_m_h5_tk token...");
            refreshAllAccountsTokens();

        } catch (Exception e) {
            log.error("定时刷新_m_h5_tk token失败", e);
        }
    }

    /**
     * 定时任务：检查并刷新WebSocket token
     * 与Python完全一致：每分钟检查一次，1小时刷新一次
     */
    @Scheduled(fixedDelay = 60 * 1000, initialDelay = 60 * 1000)
    public void scheduledRefreshWebSocketToken() {
        try {
            // 与Python完全一致：每分钟检查一次，判断是否需要刷新（1小时）
            log.debug("🔄 检查WebSocket token是否需要刷新...");

            List<XianyuAccount> accounts = accountMapper.selectList(null);

            for (XianyuAccount account : accounts) {
                if (account.getStatus() == 1) { // 只刷新正常状态的账号
                    // 检查是否需要刷新（提前1小时刷新，与Python一致）
                    if (needsRefresh(account.getId())) {
                        log.info("🔄 账号{}的WebSocket token即将过期，开始刷新...", account.getId());
                        boolean success = refreshWebSocketToken(account.getId());

                        if (success) {
                            log.info("✅ 账号{}的WebSocket token刷新成功", account.getId());
                        } else {
                            log.warn("⚠️ 账号{}的WebSocket token刷新失败，将在下次检查时重试", account.getId());
                        }

                        // 间隔2-5秒，避免频繁请求
                        int randomInterval = 2000 + new java.util.Random().nextInt(3001);
                        Thread.sleep(randomInterval);
                    }
                }
            }

        } catch (Exception e) {
            log.error("定时检查WebSocket token失败", e);
        }
    }
    
    /**
     * 刷新所有账号的token
     */
    @Override
    public void refreshAllAccountsTokens() {
        try {
            List<XianyuAccount> accounts = accountMapper.selectList(null);
            
            int successCount = 0;
            int failCount = 0;
            
            for (XianyuAccount account : accounts) {
                if (account.getStatus() == 1) { // 只刷新正常状态的账号
                    boolean success = refreshMh5tkToken(account.getId());
                    if (success) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                    
                    // 随机间隔2-5秒，避免频繁请求被检测
                    int randomInterval = 2000 + new java.util.Random().nextInt(3001);
                    Thread.sleep(randomInterval);
                }
            }
            
            log.info("✅ _m_h5_tk token刷新完成: 成功{}个, 失败{}个", successCount, failCount);
            
        } catch (Exception e) {
            log.error("刷新所有账号token失败", e);
        }
    }
}
