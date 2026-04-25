package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.constants.OperationConstants;
import com.feijimiao.xianyuassistant.entity.XianyuCookie;
import com.feijimiao.xianyuassistant.mapper.XianyuCookieMapper;
import com.feijimiao.xianyuassistant.service.CookieRefreshService;
import com.feijimiao.xianyuassistant.service.OperationLogService;
import com.feijimiao.xianyuassistant.utils.XianyuSignUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cookie刷新服务实现
 * 参考Python代码的Cookie刷新逻辑
 *
 * 核心改进（对齐Python的requests.Session行为）：
 * 1. hasLogin成功后，从响应Set-Cookie中提取并更新_m_h5_tk到数据库
 * 2. 使用OkHttp的CookieJar自动管理Cookie（模拟requests.Session）
 * 3. 添加并发锁，防止多线程同时刷新同一账号的Cookie
 */
@Slf4j
@Service
public class CookieRefreshServiceImpl implements CookieRefreshService {

    @Autowired
    private XianyuCookieMapper cookieMapper;

    @Autowired
    private OperationLogService operationLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String HAS_LOGIN_URL = "https://passport.goofish.com/newlogin/hasLogin.do";

    /**
     * 每个账号的刷新锁，防止并发刷新
     */
    private final Map<Long, Object> refreshLocks = new ConcurrentHashMap<>();

    public CookieRefreshServiceImpl() {
    }

    /**
     * 获取账号级别的锁对象
     */
    private Object getRefreshLock(Long accountId) {
        return refreshLocks.computeIfAbsent(accountId, k -> new Object());
    }

    @Override
    public boolean checkLoginStatus(Long accountId) {
        synchronized (getRefreshLock(accountId)) {
            return doCheckLoginStatus(accountId);
        }
    }

    /**
     * 执行hasLogin检查
     * 参考Python XianyuApis.hasLogin方法
     *
     * 关键：Python的requests.Session会自动处理Set-Cookie，Java需要手动处理
     * hasLogin成功后，响应中的Set-Cookie可能包含新的_m_h5_tk等关键Cookie
     */
    private boolean doCheckLoginStatus(Long accountId) {
        try {
            log.info("【账号{}】开始检查登录状态...", accountId);

            // 1. 获取Cookie
            XianyuCookie cookie = cookieMapper.selectOne(
                    new LambdaQueryWrapper<XianyuCookie>()
                            .eq(XianyuCookie::getXianyuAccountId, accountId)
                            .orderByDesc(XianyuCookie::getCreatedTime)
                            .last("LIMIT 1")
            );

            if (cookie == null || cookie.getCookieText() == null) {
                log.warn("【账号{}】未找到Cookie", accountId);
                return false;
            }

            // 2. 解析Cookie
            Map<String, String> cookies = XianyuSignUtils.parseCookies(cookie.getCookieText());

            // 3. 构建请求参数（对齐Python的hasLogin）
            FormBody.Builder formBuilder = new FormBody.Builder();
            formBuilder.add("appName", "xianyu");
            formBuilder.add("fromSite", "77");
            formBuilder.add("hid", cookies.getOrDefault("unb", ""));
            formBuilder.add("ltl", "true");
            formBuilder.add("appEntrance", "web");
            formBuilder.add("_csrf_token", cookies.getOrDefault("XSRF-TOKEN", ""));
            formBuilder.add("umidToken", "");
            formBuilder.add("hsiz", cookies.getOrDefault("cookie2", ""));
            formBuilder.add("bizParams", "taobaoBizLoginFrom=web");
            formBuilder.add("mainPage", "false");
            formBuilder.add("isMobile", "false");
            formBuilder.add("lang", "zh_CN");
            formBuilder.add("returnUrl", "");
            formBuilder.add("isIframe", "true");
            formBuilder.add("documentReferer", "https://www.goofish.com/");
            formBuilder.add("defaultView", "hasLogin");
            formBuilder.add("umidTag", "SERVER");
            formBuilder.add("deviceId", cookies.getOrDefault("cna", ""));

            // 4. 构建请求头
            Headers.Builder headersBuilder = new Headers.Builder();
            headersBuilder.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headersBuilder.add("Accept", "application/json, text/plain, */*");
            headersBuilder.add("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headersBuilder.add("Referer", "https://passport.goofish.com/");
            headersBuilder.add("Origin", "https://passport.goofish.com");
            headersBuilder.add("Cookie", cookie.getCookieText());

            // 5. 发送请求（每次创建新的OkHttpClient，确保拿到完整响应头）
            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .build();

            Request request = new Request.Builder()
                    .url(HAS_LOGIN_URL)
                    .headers(headersBuilder.build())
                    .post(formBuilder.build())
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("【账号{}】检查登录状态失败: HTTP {}", accountId, response.code());
                    return false;
                }

                String responseBody = response.body().string();
                log.debug("【账号{}】hasLogin响应: {}", accountId, responseBody);

                // 6. 解析响应
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

                @SuppressWarnings("unchecked")
                Map<String, Object> content = (Map<String, Object>) responseMap.get("content");

                if (content != null && Boolean.TRUE.equals(content.get("success"))) {
                    log.info("【账号{}】✅ 登录状态有效", accountId);

                    // 7. 【关键】处理响应中的Set-Cookie（模拟Python requests.Session的自动行为）
                    Headers responseHeaders = response.headers();
                    List<String> setCookieHeaders = responseHeaders.values("Set-Cookie");

                    log.info("【账号{}】hasLogin响应Set-Cookie数量: {}", accountId, setCookieHeaders.size());

                    // 【关键修复】参考Python：无论是否有Set-Cookie，都要清理重复Cookie
                    String oldCookieStr = cookie.getCookieText();
                    String newCookieStr = oldCookieStr;

                    if (!setCookieHeaders.isEmpty()) {
                        // 打印包含_m_h5_tk的Set-Cookie
                        for (String sc : setCookieHeaders) {
                            if (sc.contains("_m_h5_tk")) {
                                log.info("【账号{}】hasLogin Set-Cookie包含_m_h5_tk: {}", accountId,
                                        sc.length() > 80 ? sc.substring(0, 80) + "..." : sc);
                            }
                        }

                        // 合并Cookie
                        newCookieStr = mergeCookies(oldCookieStr, setCookieHeaders);
                    }

                    // 【关键】参考Python：无论是否有Set-Cookie，都要清理重复Cookie
                    newCookieStr = clearDuplicateCookies(newCookieStr);

                    // 更新Cookie到数据库
                    boolean cookieChanged = !newCookieStr.equals(oldCookieStr);
                    if (cookieChanged) {
                        // 检查_m_h5_tk是否更新了
                        Map<String, String> oldCookieMap = XianyuSignUtils.parseCookies(oldCookieStr);
                        Map<String, String> newCookieMap = XianyuSignUtils.parseCookies(newCookieStr);
                        String oldMh5tk = oldCookieMap.get("_m_h5_tk");
                        String newMh5tk = newCookieMap.get("_m_h5_tk");
                        boolean mh5tkUpdated = (newMh5tk != null && !newMh5tk.equals(oldMh5tk));

                        // 更新数据库
                        cookieMapper.update(null,
                                new LambdaUpdateWrapper<XianyuCookie>()
                                        .eq(XianyuCookie::getXianyuAccountId, accountId)
                                        .set(XianyuCookie::getCookieText, newCookieStr)
                                        .set(XianyuCookie::getCookieStatus, 1)
                                        .set(mh5tkUpdated && newMh5tk != null, XianyuCookie::getMH5Tk, newMh5tk)
                        );

                        if (mh5tkUpdated) {
                            log.info("【账号{}】✅ _m_h5_tk已从hasLogin响应中更新: {} -> {}",
                                    accountId,
                                    oldMh5tk != null ? oldMh5tk.substring(0, Math.min(20, oldMh5tk.length())) + "..." : "null",
                                    newMh5tk.substring(0, Math.min(20, newMh5tk.length())) + "...");
                        }

                        log.info("【账号{}】✅ Cookie已从hasLogin响应Set-Cookie更新到数据库", accountId);
                    } else {
                        // 即使Cookie内容未变，也要确保Cookie状态为有效
                        if (cookie.getCookieStatus() != 1) {
                            cookieMapper.update(null,
                                    new LambdaUpdateWrapper<XianyuCookie>()
                                            .eq(XianyuCookie::getXianyuAccountId, accountId)
                                    .set(XianyuCookie::getCookieStatus, 1)
                            );
                            log.info("【账号{}】✅ Cookie状态已更新为有效", accountId);
                        }
                        log.info("【账号{}】本次hasLogin未返回新的Set-Cookie，仅确认登录态仍然有效", accountId);
                    }

                    // 记录操作日志
                    operationLogService.log(accountId,
                            OperationConstants.Type.UPDATE,
                            OperationConstants.Module.COOKIE,
                            "Cookie自动刷新成功",
                            OperationConstants.Status.SUCCESS,
                            OperationConstants.TargetType.COOKIE,
                            String.valueOf(accountId),
                            null, null, null, null);

                    return true;
                } else {
                    log.warn("【账号{}】⚠️ 登录状态无效", accountId);

                    // 记录操作日志
                    operationLogService.log(accountId,
                            OperationConstants.Type.VERIFY,
                            OperationConstants.Module.COOKIE,
                            "登录状态检查失败",
                            OperationConstants.Status.FAIL,
                            OperationConstants.TargetType.COOKIE,
                            String.valueOf(accountId),
                            null, null, "登录状态无效", null);

                    return false;
                }
            }

        } catch (Exception e) {
            log.error("【账号{}】检查登录状态失败", accountId, e);

            // 记录操作日志
            operationLogService.log(accountId,
                    OperationConstants.Type.VERIFY,
                    OperationConstants.Module.COOKIE,
                    "检查登录状态异常: " + e.getMessage(),
                    OperationConstants.Status.FAIL,
                    OperationConstants.TargetType.COOKIE,
                    String.valueOf(accountId),
                    null, null, e.getMessage(), null);

            return false;
        }
    }

    @Override
    public boolean refreshCookie(Long accountId) {
        synchronized (getRefreshLock(accountId)) {
            try {
                log.info("【账号{}】开始刷新Cookie...", accountId);

                // 通过hasLogin接口刷新Cookie
                boolean success = doCheckLoginStatus(accountId);

                if (success) {
                    log.info("【账号{}】✅ Cookie刷新成功", accountId);

                    // 记录操作日志
                    operationLogService.log(accountId,
                            OperationConstants.Type.REFRESH,
                            OperationConstants.Module.COOKIE,
                            "Cookie刷新成功",
                            OperationConstants.Status.SUCCESS,
                            OperationConstants.TargetType.COOKIE,
                            String.valueOf(accountId),
                            null, null, null, null);
                } else {
                    log.error("【账号{}】❌ Cookie刷新失败，需要手动更新", accountId);

                    // 记录操作日志
                    operationLogService.log(accountId,
                            OperationConstants.Type.REFRESH,
                            OperationConstants.Module.COOKIE,
                            "Cookie刷新失败，需要手动更新",
                            OperationConstants.Status.FAIL,
                            OperationConstants.TargetType.COOKIE,
                            String.valueOf(accountId),
                            null, null, null, null);
                }

                return success;

            } catch (Exception e) {
                log.error("【账号{}】刷新Cookie失败", accountId, e);

                // 记录操作日志
                operationLogService.log(accountId,
                        OperationConstants.Type.REFRESH,
                        OperationConstants.Module.COOKIE,
                        "刷新Cookie异常: " + e.getMessage(),
                        OperationConstants.Status.FAIL,
                        OperationConstants.TargetType.COOKIE,
                        String.valueOf(accountId),
                        null, null, e.getMessage(), null);

                return false;
            }
        }
    }

    @Override
    public String clearDuplicateCookies(String cookieStr) {
        if (cookieStr == null || cookieStr.isEmpty()) {
            return cookieStr;
        }

        // 解析Cookie（后面的覆盖前面的，保留最后一次出现的值）
        Map<String, String> cookies = new LinkedHashMap<>();
        String[] parts = cookieStr.split(";\\s*");

        for (String part : parts) {
            int idx = part.indexOf('=');
            if (idx > 0) {
                String key = part.substring(0, idx);
                String value = part.substring(idx + 1);
                cookies.put(key, value);
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

    /**
     * 合并Cookie（新Cookie覆盖旧Cookie）
     * 模拟Python requests.Session自动处理Set-Cookie的行为
     *
     * @param oldCookieStr 旧Cookie字符串（分号分隔的name=value格式）
     * @param newCookies   新Cookie列表（Set-Cookie格式：name=value; Path=/; Domain=...）
     * @return 合并后的Cookie字符串
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
                // 跳过删除Cookie（值为空或包含过期日期的）
                if (!value.isEmpty()) {
                    cookies.put(key, value);
                } else {
                    // 如果值为空，可能是服务器要删除这个Cookie
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
}
