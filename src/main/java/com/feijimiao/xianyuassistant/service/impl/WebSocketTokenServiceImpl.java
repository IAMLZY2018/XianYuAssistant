package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.entity.XianyuCookie;
import com.feijimiao.xianyuassistant.exception.CaptchaRequiredException;
import com.feijimiao.xianyuassistant.mapper.XianyuCookieMapper;

import com.feijimiao.xianyuassistant.service.AccountService;
import com.feijimiao.xianyuassistant.service.CookieRefreshService;
import com.feijimiao.xianyuassistant.service.OperationLogService;
import com.feijimiao.xianyuassistant.service.WebSocketTokenService;
import com.feijimiao.xianyuassistant.utils.XianyuSignUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WebSocket Token服务实现
 * 参考Python XianyuAutoAgent的get_token/hasLogin方法
 *
 * 核心逻辑（与Python完全对齐）：
 * 1. 使用OkHttp发送token请求（而非RestTemplate），确保能正确获取Set-Cookie响应头
 * 2. 模拟Python requests.Session的有状态Cookie管理：
 *    - 每次请求前，从数据库读取Cookie构建请求头
 *    - 每次请求后，从响应Set-Cookie中更新Cookie到数据库
 *    - 重试时使用更新后的Cookie（新_m_h5_tk）
 * 3. hasLogin刷新后，必须从数据库读取新Cookie，确保签名使用新_m_h5_tk
 *
 * 与Python的关键对应关系：
 * - Python self.session.cookies → Java 从数据库读取/更新Cookie
 * - Python self.session.post → Java OkHttp发送请求+手动管理Cookie
 * - Python self.clear_duplicate_cookies → Java mergeCookies + clearDuplicateCookies
 */
@Slf4j
@Service
public class WebSocketTokenServiceImpl implements WebSocketTokenService {

    @Autowired
    private XianyuCookieMapper xianyuCookieMapper;

    @Autowired
    private com.feijimiao.xianyuassistant.mapper.XianyuAccountMapper xianyuAccountMapper;

    @Autowired
    private CookieRefreshService cookieRefreshService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private OperationLogService operationLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Token API地址
     */
    private static final String TOKEN_API_URL = "https://h5api.m.goofish.com/h5/mtop.taobao.idlemessage.pc.login.token/1.0/";

    /**
     * Token 有效期（20小时，参考 Python 的 TOKEN_REFRESH_INTERVAL）
     */
    private static final long TOKEN_VALID_DURATION = 20 * 60 * 60 * 1000; // 20小时

    /**
     * 记录正在等待验证的账号和验证URL
     * Key: accountId, Value: captchaUrl
     */
    private final Map<Long, String> pendingCaptchaAccounts = new ConcurrentHashMap<>();

    /**
     * 记录验证URL的创建时间，用于超时清理
     * Key: accountId, Value: timestamp
     */
    private final Map<Long, Long> captchaTimestamps = new ConcurrentHashMap<>();

    /**
     * 验证URL有效期（5分钟）
     */
    private static final long CAPTCHA_TIMEOUT = 5 * 60 * 1000;

    /**
     * Token获取失败重试最大次数（参考Python: retry_count >= 2）
     */
    private static final int MAX_TOKEN_RETRY_COUNT = 2;

    /**
     * Cookie过期时hasLogin重试最大次数
     */
    private static final int MAX_COOKIE_RETRY_COUNT = 2;

    /**
     * 重试间隔（毫秒）
     */
    private static final long RETRY_INTERVAL = 500;

    /**
     * 每个账号的Token获取锁，防止并发获取
     */
    private final Map<Long, Object> tokenLocks = new ConcurrentHashMap<>();

    /**
     * 共享的OkHttpClient（用于发送token API请求）
     */
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .build();

    /**
     * 获取账号级别的锁对象
     */
    private Object getTokenLock(Long accountId) {
        return tokenLocks.computeIfAbsent(accountId, k -> new Object());
    }

    @Override
    public String getAccessToken(Long accountId) {
        synchronized (getTokenLock(accountId)) {
            return getAccessTokenWithRetry(accountId, 0);
        }
    }

    /**
     * 从数据库获取最新的Cookie字符串
     */
    private String getLatestCookieFromDb(Long accountId) {
        try {
            XianyuCookie cookie = xianyuCookieMapper.selectOne(
                    new LambdaQueryWrapper<XianyuCookie>()
                            .eq(XianyuCookie::getXianyuAccountId, accountId)
                            .orderByDesc(XianyuCookie::getCreatedTime)
                            .last("LIMIT 1")
            );
            if (cookie != null && cookie.getCookieText() != null) {
                return cookie.getCookieText();
            }
        } catch (Exception e) {
            log.error("【账号{}】从数据库获取最新Cookie失败", accountId, e);
        }
        return null;
    }

    /**
     * 获取AccessToken（带重试机制）
     * 参考Python XianyuApis.get_token方法
     *
     * 核心改进（与Python对齐）：
     * 1. 使用OkHttp发送请求，确保能正确获取Set-Cookie响应头
     * 2. 每次重试都从数据库重新读取最新Cookie（可能已被Set-Cookie更新）
     * 3. API失败后从响应Set-Cookie更新数据库Cookie，再重试（模拟Python session行为）
     *
     * @param accountId 账号ID
     * @param retryCount 当前重试次数
     * @return accessToken
     */
    private String getAccessTokenWithRetry(Long accountId, int retryCount) {
        try {
            // 0. 检查是否正在等待验证
            if (pendingCaptchaAccounts.containsKey(accountId)) {
                Long timestamp = captchaTimestamps.get(accountId);
                if (timestamp != null && System.currentTimeMillis() - timestamp < CAPTCHA_TIMEOUT) {
                    String captchaUrl = pendingCaptchaAccounts.get(accountId);
                    log.debug("【账号{}】正在等待滑块验证，跳过重复请求", accountId);
                    throw new CaptchaRequiredException(captchaUrl);
                } else {
                    log.info("【账号{}】验证超时，清除等待状态", accountId);
                    pendingCaptchaAccounts.remove(accountId);
                    captchaTimestamps.remove(accountId);
                }
            }

            // 1. 【关键】每次都从数据库重新读取最新Cookie
            String cookiesStr = getLatestCookieFromDb(accountId);
            if (cookiesStr == null || cookiesStr.isEmpty()) {
                log.error("【账号{}】获取Cookie失败，无法获取Token", accountId);
                return null;
            }

            // 2. 先从数据库检查是否有有效的 Token
            XianyuCookie cookieEntity = xianyuCookieMapper.selectOne(
                    new LambdaQueryWrapper<XianyuCookie>()
                            .eq(XianyuCookie::getXianyuAccountId, accountId)
            );

            if (cookieEntity != null && cookieEntity.getWebsocketToken() != null
                    && cookieEntity.getTokenExpireTime() != null) {
                long now = System.currentTimeMillis();
                if (cookieEntity.getTokenExpireTime() > now) {
                    long remainingHours = (cookieEntity.getTokenExpireTime() - now) / (60 * 60 * 1000);
                    log.info("【账号{}】使用数据库中的accessToken（剩余有效期: {}小时）",
                            accountId, remainingHours);
                    pendingCaptchaAccounts.remove(accountId);
                    captchaTimestamps.remove(accountId);
                    return cookieEntity.getWebsocketToken();
                } else {
                    log.info("【账号{}】数据库中的Token已过期，需要重新获取", accountId);
                }
            }

            log.info("【账号{}】开始获取新的accessToken... (重试次数: {})", accountId, retryCount);

            // 3. 生成时间戳
            String timestamp = String.valueOf(System.currentTimeMillis());

            // 4. 使用数据库中最新的Cookie来解析_m_h5_tk
            Map<String, String> cookies = XianyuSignUtils.parseCookies(cookiesStr);
            String mh5tk = cookies.get("_m_h5_tk");
            String token = "";
            if (mh5tk != null && mh5tk.contains("_")) {
                token = mh5tk.split("_")[0];
            }
            log.info("【账号{}】签名使用的_m_h5_tk前缀: {}", accountId,
                    token.isEmpty() ? "空" : token.substring(0, Math.min(10, token.length())) + "...");

            // 5. 构建data参数
            String deviceId = getDeviceId(accountId, cookies);
            String dataVal = String.format("{\"appKey\":\"444e9908a51d1cb236a27862abc769c9\",\"deviceId\":\"%s\"}", deviceId);

            // 6. 生成签名
            String sign = XianyuSignUtils.generateSign(timestamp, token, dataVal);

            // 7. 构建URL参数
            StringBuilder urlBuilder = new StringBuilder(TOKEN_API_URL);
            urlBuilder.append("?");
            appendUrlParam(urlBuilder, "jsv", "2.7.2");
            appendUrlParam(urlBuilder, "appKey", "34839810");
            appendUrlParam(urlBuilder, "t", timestamp);
            appendUrlParam(urlBuilder, "sign", sign);
            appendUrlParam(urlBuilder, "v", "1.0");
            appendUrlParam(urlBuilder, "type", "originaljson");
            appendUrlParam(urlBuilder, "accountSite", "xianyu");
            appendUrlParam(urlBuilder, "dataType", "json");
            appendUrlParam(urlBuilder, "timeout", "20000");
            appendUrlParam(urlBuilder, "api", "mtop.taobao.idlemessage.pc.login.token");
            appendUrlParam(urlBuilder, "sessionOption", "AutoLoginOnly");
            appendUrlParam(urlBuilder, "spm_cnt", "a21ybx.im.0.0");
            appendUrlParam(urlBuilder, "spm_pre", "a21ybx.item.want.1.14ad3da6ALVq3n");
            appendUrlParam(urlBuilder, "log_id", "14ad3da6ALVq3n");
            String fullUrl = urlBuilder.toString();
            if (fullUrl.endsWith("&")) {
                fullUrl = fullUrl.substring(0, fullUrl.length() - 1);
            }

            // 8. 构建请求体（application/x-www-form-urlencoded）
            String formData = "data=" + URLEncoder.encode(dataVal, "UTF-8");

            // 9. 构建请求头
            Request.Builder requestBuilder = new Request.Builder()
                    .url(fullUrl)
                    .post(RequestBody.create(formData, MediaType.parse("application/x-www-form-urlencoded")))
                    .header("Host", "h5api.m.goofish.com")
                    .header("sec-ch-ua-platform", "\"Windows\"")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36")
                    .header("accept", "application/json")
                    .header("sec-ch-ua", "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\"")
                    .header("sec-ch-ua-mobile", "?0")
                    .header("origin", "https://www.goofish.com")
                    .header("sec-fetch-site", "same-site")
                    .header("sec-fetch-mode", "cors")
                    .header("sec-fetch-dest", "empty")
                    .header("referer", "https://www.goofish.com/")
                    .header("accept-language", "en,zh-CN;q=0.9,zh;q=0.8,zh-TW;q=0.7,ja;q=0.6")
                    .header("priority", "u=1, i")
                    .header("Cookie", cookiesStr);

            log.info("【账号{}】============", accountId);
            log.info("【账号{}】1、请求体: data={}", accountId, dataVal);
            log.info("【账号{}】2、发送POST请求: {}", accountId, fullUrl);

            // 10. 发送请求（OkHttp能正确返回Set-Cookie头）
            try (Response httpResponse = httpClient.newCall(requestBuilder.build()).execute()) {
                if (!httpResponse.isSuccessful()) {
                    log.error("【账号{}】获取accessToken失败：HTTP {}", accountId, httpResponse.code());
                    return handleTokenFailure(accountId, retryCount, null, "HTTP " + httpResponse.code());
                }

                String responseBody = httpResponse.body() != null ? httpResponse.body().string() : "";

                // 【关键改进】处理响应中的Set-Cookie（参考Python: session自动处理 + clear_duplicate_cookies）
                // OkHttp能正确返回Set-Cookie头，这是与RestTemplate的关键区别
                Headers responseHeaders = httpResponse.headers();
                List<String> setCookieHeaders = responseHeaders.values("Set-Cookie");

                if (!setCookieHeaders.isEmpty()) {
                    log.info("【账号{}】检测到响应中的Set-Cookie，数量: {}", accountId, setCookieHeaders.size());
                    String updatedCookieStr = updateCookiesFromResponse(accountId, cookiesStr, setCookieHeaders);
                    if (updatedCookieStr != null && !updatedCookieStr.equals(cookiesStr)) {
                        log.info("【账号{}】Cookie已从响应Set-Cookie中更新，_m_h5_tk可能已更新", accountId);
                    } else {
                        log.info("【账号{}】Set-Cookie未改变Cookie内容", accountId);
                    }
                } else {
                    log.info("【账号{}】响应中无Set-Cookie", accountId);
                }

                log.info("【账号{}】3、响应内容: {}", accountId, responseBody);
                log.info("【账号{}】============", accountId);

                if (responseBody == null || responseBody.isEmpty()) {
                    log.error("【账号{}】获取accessToken失败：响应为空", accountId);
                    return handleTokenFailure(accountId, retryCount, null, "响应为空");
                }

                // 11. 解析响应
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

                // 检查ret字段
                Object retObj = responseMap.get("ret");
                if (retObj instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<String> retList = (java.util.List<String>) retObj;
                    log.info("【账号{}】ret字段内容: {}", accountId, retList);

                    boolean success = retList.stream().anyMatch(ret -> ret.contains("SUCCESS::调用成功"));

                    if (success) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
                        if (dataMap != null && dataMap.containsKey("accessToken")) {
                            String accessToken = (String) dataMap.get("accessToken");

                            // 保存 token 到数据库
                            saveTokenToDatabase(accountId, accessToken);

                            // 更新账号状态为正常（1）
                            updateAccountStatusToNormal(accountId);

                            log.info("【账号{}】accessToken获取成功并已保存到数据库", accountId);
                            log.debug("【账号{}】accessToken: {}...", accountId,
                                    accessToken.substring(0, Math.min(20, accessToken.length())));

                            operationLogService.log(accountId,
                                com.feijimiao.xianyuassistant.constants.OperationConstants.Type.REFRESH,
                                com.feijimiao.xianyuassistant.constants.OperationConstants.Module.TOKEN,
                                "WebSocket Token获取成功",
                                com.feijimiao.xianyuassistant.constants.OperationConstants.Status.SUCCESS,
                                com.feijimiao.xianyuassistant.constants.OperationConstants.TargetType.TOKEN,
                                String.valueOf(accountId),
                                null, null, null, null);

                            return accessToken;
                        }
                    }

                    // 检查是否需要滑块验证
                    boolean needCaptcha = retList.stream().anyMatch(ret -> ret.contains("FAIL_SYS_USER_VALIDATE"));
                    log.info("【账号{}】是否需要滑块验证: {}", accountId, needCaptcha);

                    if (needCaptcha) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
                        log.info("【账号{}】data字段内容: {}", accountId, dataMap);

                        if (dataMap != null && dataMap.containsKey("url")) {
                            String captchaUrl = (String) dataMap.get("url");

                            pendingCaptchaAccounts.put(accountId, captchaUrl);
                            captchaTimestamps.put(accountId, System.currentTimeMillis());

                            updateAccountStatusToCaptchaRequired(accountId);

                            log.warn("【账号{}】检测到滑块验证，URL: {}", accountId, captchaUrl);
                            log.warn("【账号{}】需要人工完成滑块验证，请访问: http://localhost:8080/websocket-manual-captcha.html", accountId);
                            log.warn("【账号{}】账号状态已更新为-2（需要验证）", accountId);

                            throw new CaptchaRequiredException(captchaUrl);
                        } else {
                            log.error("【账号{}】需要滑块验证但未找到URL", accountId);
                        }
                    }

                    // 检查是否触发风控（RGV587_ERROR）
                    boolean needRiskControl = retList.stream().anyMatch(ret -> ret.contains("RGV587_ERROR") || ret.contains("被挤爆啦"));
                    if (needRiskControl) {
                        log.error("【账号{}】❌ 触发风控: {}", accountId, retList);
                        log.error("【账号{}】系统目前无法自动解决，请进入闲鱼网页版-点击消息-过滑块-复制最新的Cookie", accountId);
                        updateCookieStatus(accountId, 3);
                        throw new com.feijimiao.xianyuassistant.exception.CookieExpiredException(
                                "触发风控，请进入闲鱼网页版过滑块后更新Cookie");
                    }
                }

                log.error("【账号{}】获取accessToken失败：{}", accountId, responseBody);

                // Token获取失败，进入失败处理流程
                return handleTokenFailure(accountId, retryCount, responseBody, "Token API调用失败");
            }

        } catch (CaptchaRequiredException e) {
            throw e;
        } catch (com.feijimiao.xianyuassistant.exception.CookieExpiredException e) {
            throw e;
        } catch (Exception e) {
            log.error("【账号{}】获取accessToken异常", accountId, e);
            return null;
        }
    }

    /**
     * URL参数追加辅助方法
     */
    private void appendUrlParam(StringBuilder sb, String key, String value) {
        try {
            sb.append(key).append("=").append(URLEncoder.encode(value, "UTF-8")).append("&");
        } catch (Exception e) {
            log.error("URL编码失败: key={}", key, e);
        }
    }

    /**
     * 获取设备ID
     */
    private String getDeviceId(Long accountId, Map<String, String> cookies) {
        com.feijimiao.xianyuassistant.entity.XianyuAccount account = xianyuAccountMapper.selectById(accountId);
        if (account != null && account.getDeviceId() != null) {
            return account.getDeviceId();
        }
        String unb = cookies.get("unb");
        if (unb != null) {
            return accountService.getOrGenerateDeviceId(accountId, unb);
        }
        return "device_" + accountId;
    }

    /**
     * 处理Token获取失败的情况
     * 参考Python XianyuApis.get_token的失败处理逻辑：
     *
     * Python逻辑：
     * 1. 非SUCCESS时，先检查响应Set-Cookie并更新cookies（clear_duplicate_cookies）
     * 2. retry_count < 2时，直接重试（此时session已有新cookie）
     * 3. retry_count >= 2时，调用hasLogin刷新Cookie，成功后重置retry_count重新获取token
     */
    private String handleTokenFailure(Long accountId, int retryCount, String response, String reason) {

        boolean isCookieExpired = response != null && (
            response.contains("FAIL_SYS_SESSION_EXPIRED") ||
            response.contains("FAIL_SYS_TOKEN_EXOIRED") ||
            response.contains("FAIL_SYS_TOKEN_EXPIRED") ||
            response.contains("令牌过期"));

        if (isCookieExpired) {
            log.warn("【账号{}】检测到令牌过期/Cookie过期", accountId);
        }

        // 参考Python: retry_count < 2 时直接重试（此时Cookie已从Set-Cookie更新）
        if (retryCount < MAX_TOKEN_RETRY_COUNT) {
            log.warn("【账号{}】Token获取失败({})，准备重试... (重试次数: {}/{})",
                    accountId, reason, retryCount + 1, MAX_TOKEN_RETRY_COUNT);

            try {
                Thread.sleep(RETRY_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 【关键】重试时从数据库重新读取最新Cookie（可能已被Set-Cookie更新）
            return getAccessTokenWithRetry(accountId, retryCount + 1);
        }

        // 参考Python: retry_count >= 2 时，调用hasLogin刷新Cookie后重试
        log.warn("【账号{}】Token获取重试已达上限，尝试通过hasLogin刷新Cookie...", accountId);
        return refreshTokenViaHasLogin(accountId, 0);
    }

    /**
     * 通过hasLogin刷新Cookie后重新获取Token
     * 参考Python: get_token中retry_count >= 2时的逻辑
     *
     * Python逻辑：
     * if retry_count >= 2:
     *     if self.hasLogin():  # hasLogin会自动更新session cookies
     *         return self.get_token(device_id, 0)  # 重置重试次数
     *     else:
     *         sys.exit(1)  # Cookie彻底失效
     */
    private String refreshTokenViaHasLogin(Long accountId, int hasLoginRetryCount) {
        if (hasLoginRetryCount >= MAX_COOKIE_RETRY_COUNT) {
            log.error("【账号{}】hasLogin刷新重试次数已达上限，Cookie已彻底过期", accountId);
            updateCookieStatus(accountId, 2);

            operationLogService.log(accountId,
                com.feijimiao.xianyuassistant.constants.OperationConstants.Type.REFRESH,
                com.feijimiao.xianyuassistant.constants.OperationConstants.Module.TOKEN,
                "WebSocket Token获取失败：Cookie过期且自动刷新失败",
                com.feijimiao.xianyuassistant.constants.OperationConstants.Status.FAIL,
                com.feijimiao.xianyuassistant.constants.OperationConstants.TargetType.TOKEN,
                String.valueOf(accountId),
                null, null, "Cookie过期且自动刷新失败", null);

            throw new com.feijimiao.xianyuassistant.exception.CookieExpiredException(
                    "Cookie已过期且自动刷新失败，请手动更新Cookie后重试");
        }

        log.info("【账号{}】开始通过hasLogin刷新Cookie... (重试次数: {}/{})",
                accountId, hasLoginRetryCount, MAX_COOKIE_RETRY_COUNT);

        try {
            // 调用hasLogin刷新Cookie（参考Python的hasLogin方法）
            boolean refreshSuccess = cookieRefreshService.refreshCookie(accountId);

            if (refreshSuccess) {
                log.info("【账号{}】hasLogin成功，Cookie已刷新，重新获取Token（重置重试计数）", accountId);

                try {
                    Thread.sleep(RETRY_INTERVAL);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // hasLogin成功后从数据库读取最新Cookie
                String newCookieStr = getLatestCookieFromDb(accountId);
                if (newCookieStr != null && !newCookieStr.isEmpty()) {
                    Map<String, String> newCookies = XianyuSignUtils.parseCookies(newCookieStr);
                    String newMh5tk = newCookies.get("_m_h5_tk");
                    log.info("【账号{}】hasLogin后从数据库获取到最新Cookie，长度: {}，_m_h5_tk前缀: {}",
                            accountId, newCookieStr.length(),
                            newMh5tk != null && newMh5tk.contains("_")
                                    ? newMh5tk.split("_")[0].substring(0, Math.min(10, newMh5tk.split("_")[0].length())) + "..."
                                    : "空");
                    // 重置retryCount为0，重新开始获取token流程
                    return getAccessTokenWithRetry(accountId, 0);
                } else {
                    log.error("【账号{}】hasLogin后获取刷新后的Cookie失败", accountId);
                }
            } else {
                log.warn("【账号{}】hasLogin失败", accountId);
            }
        } catch (Exception e) {
            log.error("【账号{}】hasLogin刷新过程发生异常", accountId, e);
        }

        // hasLogin失败，重试
        return refreshTokenViaHasLogin(accountId, hasLoginRetryCount + 1);
    }

    /**
     * 从响应的Set-Cookie中更新Cookie
     * 参考Python: requests.Session自动处理Set-Cookie + clear_duplicate_cookies
     *
     * 关键：token API在返回FAIL_SYS_SESSION_EXPIRED时，响应的Set-Cookie中会包含新的_m_h5_tk
     * Python的requests.Session会自动保存这些Cookie，所以下次请求时签名是正确的
     * Java需要手动处理，并且必须确保Set-Cookie头能被正确读取（OkHttp可以）
     *
     * @param accountId 账号ID
     * @param currentCookieStr 当前Cookie字符串
     * @param setCookieHeaders 响应中的Set-Cookie列表
     * @return 更新后的Cookie字符串
     */
    private String updateCookiesFromResponse(Long accountId, String currentCookieStr, List<String> setCookieHeaders) {
        try {
            // 打印所有Set-Cookie内容（调试用，确认Set-Cookie中是否包含_m_h5_tk）
            for (int i = 0; i < setCookieHeaders.size(); i++) {
                String setCookie = setCookieHeaders.get(i);
                // 只打印name=value部分，不打印Path等属性
                if (setCookie.contains("_m_h5_tk")) {
                    log.info("【账号{}】Set-Cookie中包含_m_h5_tk: {}", accountId,
                            setCookie.length() > 80 ? setCookie.substring(0, 80) + "..." : setCookie);
                } else {
                    log.debug("【账号{}】Set-Cookie[{}]: {}", accountId, i,
                            setCookie.length() > 80 ? setCookie.substring(0, 80) + "..." : setCookie);
                }
            }

            String newCookieStr = mergeCookies(currentCookieStr, setCookieHeaders);

            // 清理重复Cookie
            newCookieStr = cookieRefreshService.clearDuplicateCookies(newCookieStr);

            // 检查是否有新的_m_h5_tk
            Map<String, String> oldCookies = XianyuSignUtils.parseCookies(currentCookieStr);
            Map<String, String> newCookies = XianyuSignUtils.parseCookies(newCookieStr);

            String oldMh5tk = oldCookies.get("_m_h5_tk");
            String newMh5tk = newCookies.get("_m_h5_tk");

            boolean mh5tkUpdated = (newMh5tk != null && !newMh5tk.equals(oldMh5tk));
            if (mh5tkUpdated) {
                log.info("【账号{}】✅ _m_h5_tk已从响应中更新: {} -> {}", accountId,
                        oldMh5tk != null ? oldMh5tk.substring(0, Math.min(20, oldMh5tk.length())) + "..." : "null",
                        newMh5tk.substring(0, Math.min(20, newMh5tk.length())) + "...");
            } else {
                log.info("【账号{}】_m_h5_tk未变化（可能Set-Cookie中没有新的_m_h5_tk）", accountId);
            }

            // 更新数据库中的Cookie
            if (!newCookieStr.equals(currentCookieStr)) {
                xianyuCookieMapper.update(null,
                        new LambdaUpdateWrapper<XianyuCookie>()
                                .eq(XianyuCookie::getXianyuAccountId, accountId)
                                .set(XianyuCookie::getCookieText, newCookieStr)
                                .set(XianyuCookie::getCookieStatus, 1)
                );

                // 如果_m_h5_tk更新了，也更新mH5Tk字段
                if (mh5tkUpdated && newMh5tk != null) {
                    xianyuCookieMapper.update(null,
                            new LambdaUpdateWrapper<XianyuCookie>()
                                    .eq(XianyuCookie::getXianyuAccountId, accountId)
                                    .set(XianyuCookie::getMH5Tk, newMh5tk)
                    );
                }

                log.info("【账号{}】Cookie已从响应Set-Cookie更新到数据库", accountId);
            }

            return newCookieStr;
        } catch (Exception e) {
            log.error("【账号{}】处理响应Set-Cookie失败", accountId, e);
            return currentCookieStr;
        }
    }

    /**
     * 合并Cookie（新Cookie覆盖旧Cookie）
     * 模拟Python requests.Session自动处理Set-Cookie的行为
     */
    private String mergeCookies(String oldCookieStr, List<String> newCookies) {
        Map<String, String> cookies = new LinkedHashMap<>();

        // 解析旧Cookie
        if (oldCookieStr != null && !oldCookieStr.isEmpty()) {
            String[] parts = oldCookieStr.split(";\\s*");
            for (String part : parts) {
                int idx = part.indexOf('=');
                if (idx > 0) {
                    String key = part.substring(0, idx);
                    String value = part.substring(idx + 1);
                    cookies.put(key, value);
                }
            }
        }

        // 解析新Cookie（Set-Cookie格式: name=value; Path=/; Domain=.goofish.com; ...）
        for (String newCookie : newCookies) {
            // 只提取第一个name=value对（Set-Cookie头中后面的属性如Path、Domain等不是Cookie值）
            Pattern pattern = Pattern.compile("^\\s*([^=;\\s]+)=([^;]*)");
            Matcher matcher = pattern.matcher(newCookie);
            if (matcher.find()) {
                String key = matcher.group(1).trim();
                String value = matcher.group(2).trim();
                // 跳过删除Cookie（值为空）
                if (!value.isEmpty()) {
                    cookies.put(key, value);
                } else {
                    cookies.remove(key);
                }
            }
        }

        // 重新构建Cookie字符串
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }

        return sb.toString();
    }

    @Override
    public void saveToken(Long accountId, String token) {
        saveTokenToDatabase(accountId, token);
    }

    @Override
    public void clearToken(Long accountId) {
        try {
            log.info("【账号{}】清除数据库中的Token缓存", accountId);

            xianyuCookieMapper.update(null,
                    new LambdaUpdateWrapper<XianyuCookie>()
                            .eq(XianyuCookie::getXianyuAccountId, accountId)
                            .set(XianyuCookie::getTokenExpireTime, 0L)
            );

            log.info("【账号{}】Token缓存已清除", accountId);
        } catch (Exception e) {
            log.error("【账号{}】清除Token缓存失败", accountId, e);
        }
    }

    @Override
    public void clearCaptchaWait(Long accountId) {
        log.info("【账号{}】清除验证等待状态", accountId);
        pendingCaptchaAccounts.remove(accountId);
        captchaTimestamps.remove(accountId);
        log.info("【账号{}】验证等待状态已清除", accountId);
    }

    /**
     * 刷新WebSocket token
     */
    @Override
    public String refreshToken(Long accountId) {
        synchronized (getTokenLock(accountId)) {
            try {
                log.info("【账号{}】开始刷新WebSocket token...", accountId);

                // 1. 清除旧token，强制重新获取
                clearToken(accountId);

                // 2. 获取新token
                String newToken = getAccessTokenWithRetry(accountId, 0);

                if (newToken != null && !newToken.isEmpty()) {
                    log.info("【账号{}】✅ WebSocket token刷新成功", accountId);
                    return newToken;
                } else {
                    log.warn("【账号{}】⚠️ WebSocket token刷新失败", accountId);
                    return null;
                }

            } catch (Exception e) {
                log.error("【账号{}】刷新WebSocket token异常", accountId, e);
                return null;
            }
        }
    }

    /**
     * 更新账号状态为需要验证（-2）
     */
    private void updateAccountStatusToCaptchaRequired(Long accountId) {
        try {
            com.feijimiao.xianyuassistant.entity.XianyuAccount account = xianyuAccountMapper.selectById(accountId);
            if (account != null) {
                account.setStatus(-2);
                xianyuAccountMapper.updateById(account);
                log.info("【账号{}】账号状态已更新为-2（需要验证）", accountId);
            }
        } catch (Exception e) {
            log.error("【账号{}】更新账号状态失败", accountId, e);
        }
    }

    /**
     * 更新账号状态为正常（1）
     */
    private void updateAccountStatusToNormal(Long accountId) {
        try {
            com.feijimiao.xianyuassistant.entity.XianyuAccount account = xianyuAccountMapper.selectById(accountId);
            if (account != null && account.getStatus() == -2) {
                account.setStatus(1);
                xianyuAccountMapper.updateById(account);
                log.info("【账号{}】账号状态已恢复为1（正常）", accountId);
            }
        } catch (Exception e) {
            log.error("【账号{}】更新账号状态失败", accountId, e);
        }
    }

    /**
     * 更新Cookie状态
     */
    private void updateCookieStatus(Long accountId, Integer status) {
        try {
            xianyuCookieMapper.update(null,
                    new LambdaUpdateWrapper<XianyuCookie>()
                            .eq(XianyuCookie::getXianyuAccountId, accountId)
                            .set(XianyuCookie::getCookieStatus, status)
            );
            String statusText = status == 2 ? "过期" : status == 3 ? "失效" : "未知";
            log.info("【账号{}】Cookie状态已更新为{}({})", accountId, status, statusText);
        } catch (Exception e) {
            log.error("【账号{}】更新Cookie状态失败", accountId, e);
        }
    }

    /**
     * 保存 Token 到数据库
     */
    private void saveTokenToDatabase(Long accountId, String token) {
        try {
            long expireTime = System.currentTimeMillis() + TOKEN_VALID_DURATION;

            int updated = xianyuCookieMapper.update(null,
                    new LambdaUpdateWrapper<XianyuCookie>()
                            .eq(XianyuCookie::getXianyuAccountId, accountId)
                            .set(XianyuCookie::getWebsocketToken, token)
                            .set(XianyuCookie::getTokenExpireTime, expireTime)
            );

            if (updated > 0) {
                log.info("【账号{}】Token已保存到数据库，过期时间: {}", accountId,
                        new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                .format(new java.util.Date(expireTime)));
            } else {
                log.warn("【账号{}】Token保存失败，未找到对应的Cookie记录", accountId);
            }
        } catch (Exception e) {
            log.error("【账号{}】保存Token到数据库失败", accountId, e);
        }
    }
}
