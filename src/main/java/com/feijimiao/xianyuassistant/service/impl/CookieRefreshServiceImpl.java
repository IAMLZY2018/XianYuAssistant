package com.feijimiao.xianyuassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.WaitUntilState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feijimiao.xianyuassistant.constants.OperationConstants;
import com.feijimiao.xianyuassistant.entity.XianyuAccount;
import com.feijimiao.xianyuassistant.entity.XianyuCookie;
import com.feijimiao.xianyuassistant.mapper.XianyuAccountMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuCookieMapper;
import com.feijimiao.xianyuassistant.service.CookieRefreshService;
import com.feijimiao.xianyuassistant.service.OperationLogService;
import com.feijimiao.xianyuassistant.utils.SessionCookieJar;
import com.feijimiao.xianyuassistant.utils.XianyuSignUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cookie刷新服务实现
 * 参考Python代码的Cookie刷新逻辑
 *
 * 核心改进（对齐Python的requests.Session行为）：
 * 1. 使用SessionCookieJar自动管理Cookie（模拟requests.Session）
 *    - OkHttp自动解析响应Set-Cookie并合并到jar
 *    - 后续请求自动携带最新Cookie，无需手动设置Cookie头
 * 2. 保留最新策略：新Cookie值覆盖旧Cookie
 * 3. 添加并发锁，防止多线程同时刷新同一账号的Cookie
 */
@Slf4j
@Service
public class CookieRefreshServiceImpl implements CookieRefreshService {
    private static final String HAS_LOGIN_URL = "https://passport.goofish.com/newlogin/hasLogin.do";
    private static final String GOOFISH_IM_URL = "https://www.goofish.com/im";
    private static final String GOOFISH_COOKIE_DOMAIN = ".goofish.com";
    private static final String TAOBAO_COOKIE_DOMAIN = ".taobao.com";

    @Autowired
    private XianyuCookieMapper cookieMapper;

    @Autowired
    private XianyuAccountMapper accountMapper;

    @Autowired
    private OperationLogService operationLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
     * 核心改造：使用SessionCookieJar自动管理Cookie
     * - OkHttp自动从CookieJar加载Cookie，无需手动设置Cookie头
     * - OkHttp自动解析响应Set-Cookie并回调saveFromResponse
     * - hasLogin成功后从jar获取更新后的Cookie持久化到数据库
     */
    private boolean doCheckLoginStatus(Long accountId) {
        try {
            log.info("【账号{}】开始检查登录状态...", accountId);

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

            String oldCookieStr = cookie.getCookieText();
            Map<String, String> cookies = XianyuSignUtils.parseCookies(oldCookieStr);

            SessionCookieJar cookieJar = new SessionCookieJar(oldCookieStr);

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

            Request request = new Request.Builder()
                    .url(HAS_LOGIN_URL)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Referer", "https://passport.goofish.com/")
                    .header("Origin", "https://passport.goofish.com")
                    .post(formBuilder.build())
                    .build();

            OkHttpClient httpClient = cookieJar.createHttpClient();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("【账号{}】检查登录状态失败: HTTP {}", accountId, response.code());
                    return false;
                }

                String responseBody = response.body().string();
                log.debug("【账号{}】hasLogin响应: {}", accountId, responseBody);

                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

                @SuppressWarnings("unchecked")
                Map<String, Object> content = (Map<String, Object>) responseMap.get("content");

                if (content != null && Boolean.TRUE.equals(content.get("success"))) {
                    log.info("【账号{}】✅ 登录状态有效", accountId);

                    String newCookieStr = cookieJar.getCookieString();
                    boolean cookieChanged = !newCookieStr.equals(oldCookieStr);

                    if (cookieChanged) {
                        Map<String, String> oldCookieMap = XianyuSignUtils.parseCookies(oldCookieStr);
                        Map<String, String> newCookieMap = XianyuSignUtils.parseCookies(newCookieStr);
                        String oldMh5tk = oldCookieMap.get("_m_h5_tk");
                        String newMh5tk = newCookieMap.get("_m_h5_tk");
                        boolean mh5tkUpdated = (newMh5tk != null && !newMh5tk.equals(oldMh5tk));

                        String updatedTime = java.time.LocalDateTime.now().format(
                                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
                        cookieMapper.update(null,
                                new LambdaUpdateWrapper<XianyuCookie>()
                                        .eq(XianyuCookie::getXianyuAccountId, accountId)
                                        .set(XianyuCookie::getCookieText, newCookieStr)
                                        .set(XianyuCookie::getCookieStatus, 1)
                                        .set(XianyuCookie::getUpdatedTime, updatedTime)
                                        .set(mh5tkUpdated && newMh5tk != null, XianyuCookie::getMH5Tk, newMh5tk)
                        );

                        if (mh5tkUpdated) {
                            log.info("【账号{}】✅ _m_h5_tk已从hasLogin响应中更新: {} -> {}",
                                    accountId,
                                    oldMh5tk != null ? oldMh5tk.substring(0, Math.min(20, oldMh5tk.length())) + "..." : "null",
                                    newMh5tk.substring(0, Math.min(20, newMh5tk.length())) + "...");
                        }
                        log.info("【账号{}】✅ Cookie已通过SessionCookieJar自动更新到数据库", accountId);
                    } else {
                        if (cookie.getCookieStatus() == null || cookie.getCookieStatus() != 1) {
                            String updatedTime = java.time.LocalDateTime.now().format(
                                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
                            cookieMapper.update(null,
                                    new LambdaUpdateWrapper<XianyuCookie>()
                                            .eq(XianyuCookie::getXianyuAccountId, accountId)
                                            .set(XianyuCookie::getCookieStatus, 1)
                                            .set(XianyuCookie::getUpdatedTime, updatedTime)
                            );
                            log.info("【账号{}】✅ Cookie状态已更新为有效", accountId);
                        }
                        log.info("【账号{}】Cookie无变化，登录态仍然有效", accountId);
                    }

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
                if (!success) {
                    log.warn("【账号{}】hasLogin刷新失败，开始触发浏览器兜底刷新Cookie", accountId);
                    operationLogService.log(accountId,
                            OperationConstants.Type.REFRESH,
                            OperationConstants.Module.COOKIE,
                            "hasLogin刷新失败，开始触发浏览器兜底刷新Cookie",
                            OperationConstants.Status.PARTIAL,
                            OperationConstants.TargetType.COOKIE,
                            String.valueOf(accountId),
                            null, null, null, null);
                    success = refreshCookieWithBrowser(accountId);
                }

                if (success) {
                    log.info("【账号{}】✅ Cookie刷新成功", accountId);
                    updateAccountStatusToNormal(accountId, "Cookie刷新成功，账号状态恢复正常");

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
                    markAccountAsCookieRefreshAbnormal(accountId, "hasLogin和浏览器兜底刷新均失败，需要手动处理Cookie");

                    // 记录操作日志
                    operationLogService.log(accountId,
                            OperationConstants.Type.REFRESH,
                            OperationConstants.Module.COOKIE,
                            "Cookie刷新失败，需要手动更新，账号已标记为异常待处理",
                            OperationConstants.Status.FAIL,
                            OperationConstants.TargetType.COOKIE,
                            String.valueOf(accountId),
                            null, null, "hasLogin和浏览器兜底刷新均失败", null);
                }

                return success;

            } catch (Exception e) {
                log.error("【账号{}】刷新Cookie失败", accountId, e);
                markAccountAsCookieRefreshAbnormal(accountId, "刷新Cookie异常: " + e.getMessage());

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

    private boolean refreshCookieWithBrowser(Long accountId) {
        XianyuCookie cookie = cookieMapper.selectOne(
                new LambdaQueryWrapper<XianyuCookie>()
                        .eq(XianyuCookie::getXianyuAccountId, accountId)
                        .orderByDesc(XianyuCookie::getCreatedTime)
                        .last("LIMIT 1")
        );
        if (cookie == null || cookie.getCookieText() == null || cookie.getCookieText().isBlank()) {
            log.warn("【账号{}】浏览器兜底刷新失败，未找到可用Cookie", accountId);
            markAccountAsCookieRefreshAbnormal(accountId, "浏览器兜底刷新失败：未找到可用Cookie");
            operationLogService.log(accountId,
                    OperationConstants.Type.REFRESH,
                    OperationConstants.Module.COOKIE,
                    "浏览器兜底刷新Cookie失败，账号已标记为异常待处理",
                    OperationConstants.Status.FAIL,
                    OperationConstants.TargetType.COOKIE,
                    String.valueOf(accountId),
                    null, null, "未找到可用Cookie", null);
            return false;
        }

        Map<String, String> existingCookies = XianyuSignUtils.parseCookies(cookie.getCookieText());
        if (existingCookies.isEmpty()) {
            log.warn("【账号{}】浏览器兜底刷新失败，Cookie内容为空", accountId);
            markAccountAsCookieRefreshAbnormal(accountId, "浏览器兜底刷新失败：Cookie内容为空");
            operationLogService.log(accountId,
                    OperationConstants.Type.REFRESH,
                    OperationConstants.Module.COOKIE,
                    "浏览器兜底刷新Cookie失败，账号已标记为异常待处理",
                    OperationConstants.Status.FAIL,
                    OperationConstants.TargetType.COOKIE,
                    String.valueOf(accountId),
                    null, null, "Cookie内容为空", null);
            return false;
        }

        operationLogService.log(accountId,
                OperationConstants.Type.REFRESH,
                OperationConstants.Module.COOKIE,
                "开始浏览器兜底刷新Cookie",
                OperationConstants.Status.SUCCESS,
                OperationConstants.TargetType.COOKIE,
                String.valueOf(accountId),
                null, null, null, null);

        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(true);
            try (Browser browser = playwright.chromium().launch(launchOptions)) {
                Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                try (BrowserContext context = browser.newContext(contextOptions)) {
                    List<Cookie> browserCookies = buildBrowserCookies(existingCookies);
                    context.addCookies(browserCookies);

                    Page page = context.newPage();
                    log.info("【账号{}】浏览器兜底刷新Cookie，开始访问 {}", accountId, GOOFISH_IM_URL);
                    page.navigate(GOOFISH_IM_URL,
                            new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                    page.reload(new Page.ReloadOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

                    List<Cookie> refreshedCookies = context.cookies(List.of(
                            GOOFISH_IM_URL,
                            "https://passport.goofish.com",
                            "https://h5api.m.goofish.com",
                            "https://www.taobao.com"
                    ));
                    String refreshedCookieText = buildCookieText(refreshedCookies);
                    if (refreshedCookieText.isBlank()) {
                        log.warn("【账号{}】浏览器兜底刷新未获取到新的Cookie", accountId);
                        markAccountAsCookieRefreshAbnormal(accountId, "浏览器兜底刷新失败：浏览器未返回Cookie");
                        operationLogService.log(accountId,
                                OperationConstants.Type.REFRESH,
                                OperationConstants.Module.COOKIE,
                                "浏览器兜底刷新Cookie失败，账号已标记为异常待处理",
                                OperationConstants.Status.FAIL,
                                OperationConstants.TargetType.COOKIE,
                                String.valueOf(accountId),
                                null, null, "浏览器未返回Cookie", null);
                        return false;
                    }

                    Map<String, String> refreshedCookieMap = XianyuSignUtils.parseCookies(refreshedCookieText);
                    String newMh5Tk = refreshedCookieMap.get("_m_h5_tk");

                    cookieMapper.update(null,
                            new LambdaUpdateWrapper<XianyuCookie>()
                                    .eq(XianyuCookie::getXianyuAccountId, accountId)
                                    .set(XianyuCookie::getCookieText, refreshedCookieText)
                                    .set(XianyuCookie::getCookieStatus, 1)
                                    .set(newMh5Tk != null && !newMh5Tk.isBlank(), XianyuCookie::getMH5Tk, newMh5Tk)
                    );

                    log.info("【账号{}】浏览器兜底刷新Cookie成功，Cookie长度: {}", accountId, refreshedCookieText.length());
                    operationLogService.log(accountId,
                            OperationConstants.Type.REFRESH,
                            OperationConstants.Module.COOKIE,
                            "浏览器兜底刷新Cookie成功",
                            OperationConstants.Status.SUCCESS,
                            OperationConstants.TargetType.COOKIE,
                            String.valueOf(accountId),
                            null, null, null, null);
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("【账号{}】浏览器兜底刷新Cookie失败", accountId, e);
            markAccountAsCookieRefreshAbnormal(accountId, "浏览器兜底刷新异常: " + e.getMessage());
            operationLogService.log(accountId,
                    OperationConstants.Type.REFRESH,
                    OperationConstants.Module.COOKIE,
                    "浏览器兜底刷新Cookie失败，账号已标记为异常待处理",
                    OperationConstants.Status.FAIL,
                    OperationConstants.TargetType.COOKIE,
                    String.valueOf(accountId),
                    null, null, e.getMessage(), null);
            return false;
        }
    }

    private void markAccountAsCookieRefreshAbnormal(Long accountId, String reason) {
        try {
            XianyuAccount account = accountMapper.selectById(accountId);
            if (account == null) {
                operationLogService.log(accountId,
                        OperationConstants.Type.UPDATE,
                        OperationConstants.Module.ACCOUNT,
                        "Cookie刷新失败后更新账号状态失败",
                        OperationConstants.Status.FAIL,
                        OperationConstants.TargetType.ACCOUNT,
                        String.valueOf(accountId),
                        null, null, "账号不存在，原因: " + reason, null);
                return;
            }
            if (Objects.equals(account.getStatus(), -2)) {
                operationLogService.log(accountId,
                        OperationConstants.Type.UPDATE,
                        OperationConstants.Module.ACCOUNT,
                        "Cookie刷新失败，账号已处于异常待处理状态",
                        OperationConstants.Status.PARTIAL,
                        OperationConstants.TargetType.ACCOUNT,
                        String.valueOf(accountId),
                        null, null, reason, null);
                return;
            }

            account.setStatus(-2);
            accountMapper.updateById(account);
            log.warn("【账号{}】浏览器兜底刷新失败后，账号状态已更新为-2（异常待处理）", accountId);
            operationLogService.log(accountId,
                    OperationConstants.Type.UPDATE,
                    OperationConstants.Module.ACCOUNT,
                    "Cookie刷新失败，账号状态已标记为异常待处理(-2)",
                    OperationConstants.Status.SUCCESS,
                    OperationConstants.TargetType.ACCOUNT,
                    String.valueOf(accountId),
                    null, null, reason, null);
        } catch (Exception e) {
            log.error("【账号{}】Cookie刷新失败后更新账号状态异常", accountId, e);
            operationLogService.log(accountId,
                    OperationConstants.Type.UPDATE,
                    OperationConstants.Module.ACCOUNT,
                    "Cookie刷新失败后更新账号状态异常",
                    OperationConstants.Status.FAIL,
                    OperationConstants.TargetType.ACCOUNT,
                    String.valueOf(accountId),
                    null, null, e.getMessage(), null);
        }
    }

    private void updateAccountStatusToNormal(Long accountId, String reason) {
        try {
            XianyuAccount account = accountMapper.selectById(accountId);
            if (account != null && Objects.equals(account.getStatus(), -2)) {
                account.setStatus(1);
                accountMapper.updateById(account);
                log.info("【账号{}】Cookie刷新成功后，账号状态已恢复为1（正常）", accountId);
                operationLogService.log(accountId,
                        OperationConstants.Type.UPDATE,
                        OperationConstants.Module.ACCOUNT,
                        "Cookie刷新成功，账号状态已恢复正常",
                        OperationConstants.Status.SUCCESS,
                        OperationConstants.TargetType.ACCOUNT,
                        String.valueOf(accountId),
                        null, null, reason, null);
            }
        } catch (Exception e) {
            log.error("【账号{}】Cookie刷新成功后恢复账号状态失败", accountId, e);
            operationLogService.log(accountId,
                    OperationConstants.Type.UPDATE,
                    OperationConstants.Module.ACCOUNT,
                    "Cookie刷新成功后恢复账号状态失败",
                    OperationConstants.Status.FAIL,
                    OperationConstants.TargetType.ACCOUNT,
                    String.valueOf(accountId),
                    null, null, e.getMessage(), null);
        }
    }

    private List<Cookie> buildBrowserCookies(Map<String, String> cookieMap) {
        List<Cookie> browserCookies = new ArrayList<>();
        for (Map.Entry<String, String> entry : cookieMap.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if (name == null || name.isBlank() || value == null || value.isBlank()) {
                continue;
            }
            browserCookies.add(new Cookie(name, value).setDomain(GOOFISH_COOKIE_DOMAIN).setPath("/"));
            browserCookies.add(new Cookie(name, value).setDomain(TAOBAO_COOKIE_DOMAIN).setPath("/"));
        }
        return browserCookies;
    }

    private String buildCookieText(List<Cookie> cookies) {
        Map<String, String> cookieMap = new LinkedHashMap<>();
        for (Cookie cookie : cookies) {
            if (cookie.name == null || cookie.name.isBlank() || cookie.value == null || cookie.value.isBlank()) {
                continue;
            }
            cookieMap.put(cookie.name, cookie.value);
        }
        return clearDuplicateCookies(XianyuSignUtils.formatCookies(cookieMap));
    }

    @Override
    public String clearDuplicateCookies(String cookieStr) {
        if (cookieStr == null || cookieStr.isEmpty()) {
            return cookieStr;
        }

        Map<String, String> cookies = new LinkedHashMap<>();
        String[] parts = cookieStr.split(";\\s*");

        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i];
            int idx = part.indexOf('=');
            if (idx > 0) {
                String key = part.substring(0, idx);
                String value = part.substring(idx + 1);
                cookies.putIfAbsent(key, value);
            }
        }

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
