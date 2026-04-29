package com.feijimiao.xianyuassistant.utils;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 模拟Python requests.Session的Cookie管理行为
 *
 * <p>核心特性（对齐Python demo的Cookie管理）：</p>
 * <ul>
 *   <li><b>自动管理Set-Cookie</b>：OkHttp自动解析响应头中的Set-Cookie，
 *       通过{@link #saveFromResponse}自动合并，无需手动解析和拼接</li>
 *   <li><b>保留最新策略</b>：新Cookie值覆盖旧值，对齐Python的
 *       cookie_list.reverse() + keep first occurrence</li>
 *   <li>线程安全（synchronized）</li>
 * </ul>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * SessionCookieJar jar = new SessionCookieJar(cookieStr);
 * OkHttpClient client = jar.createHttpClient();
 * // 发送请求——Cookie自动携带，Set-Cookie自动吸收
 * Response response = client.newCall(request).execute();
 * // 获取更新后的Cookie
 * String updatedCookieStr = jar.getCookieString();
 * }</pre>
 */
public class SessionCookieJar implements CookieJar {

    private static final String GOOFISH_DOMAIN = ".goofish.com";

    private final LinkedHashMap<String, String> cookieMap = new LinkedHashMap<>();

    public SessionCookieJar() {
    }

    /**
     * 从Cookie字符串初始化
     * 对齐Python: session.cookies.update(self.cookies)
     */
    public SessionCookieJar(String cookieStr) {
        initFromCookieString(cookieStr);
    }

    /**
     * 从Cookie Map初始化
     */
    public SessionCookieJar(Map<String, String> cookies) {
        if (cookies != null) {
            cookieMap.putAll(cookies);
        }
    }

    /**
     * 从Cookie字符串加载初始Cookie
     */
    public synchronized void initFromCookieString(String cookieStr) {
        cookieMap.clear();
        if (cookieStr != null && !cookieStr.isEmpty()) {
            String[] parts = cookieStr.split(";\\s*");
            for (String part : parts) {
                int idx = part.indexOf('=');
                if (idx > 0) {
                    cookieMap.put(part.substring(0, idx), part.substring(idx + 1));
                }
            }
        }
    }

    /**
     * 导出为Cookie字符串（用于持久化到数据库）
     */
    public synchronized String getCookieString() {
        if (cookieMap.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : cookieMap.entrySet()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * 获取指定Cookie值
     */
    public synchronized String getCookie(String name) {
        return cookieMap.get(name);
    }

    /**
     * 获取所有Cookie的Map（只读视图副本）
     */
    public synchronized Map<String, String> getCookieMap() {
        return new LinkedHashMap<>(cookieMap);
    }

    /**
     * 获取_m_h5_tk的token部分（用于签名）
     */
    public synchronized String getMh5tkToken() {
        String mh5tk = cookieMap.get("_m_h5_tk");
        if (mh5tk != null && mh5tk.contains("_")) {
            return mh5tk.split("_")[0];
        }
        return "";
    }

    // ========== CookieJar 接口实现 ==========

    /**
     * OkHttp在收到响应后自动调用，处理Set-Cookie。
     * <p>
     * 对齐Python: requests.Session自动处理响应中的Set-Cookie头。
     * 保留最新策略：新Cookie直接覆盖旧Cookie（LinkedHashMap的put行为）。
     */
    @Override
    public synchronized void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        for (Cookie cookie : cookies) {
            if (cookie.name() != null && !cookie.name().isEmpty()) {
                cookieMap.put(cookie.name(), cookie.value());
            }
        }
    }

    /**
     * OkHttp在发送请求前自动调用，加载Cookie。
     * <p>
     * 对齐Python: requests.Session自动在请求中携带Cookie。
     * 根据请求URL动态设置domain，确保Cookie能被正确发送。
     * 
     * 修复说明：
     * - OkHttp的Cookie.Builder.domain()会验证domain是否与URL匹配
     * - 直接使用URL的host作为domain，避免验证失败
     * - 对于.goofish.com的子域名（如passport.goofish.com），使用实际的host
     */
    @Override
    public synchronized List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> result = new ArrayList<>();
        String host = url.host();
        
        // 确定Cookie的domain
        // 如果是goofish.com的子域名，使用实际的host
        String cookieDomain = host;
        
        for (Map.Entry<String, String> entry : cookieMap.entrySet()) {
            try {
                result.add(new Cookie.Builder()
                        .name(entry.getKey())
                        .value(entry.getValue())
                        .domain(cookieDomain)
                        .path("/")
                        .build());
            } catch (IllegalArgumentException e) {
                // 如果domain验证失败，尝试使用顶级域名
                try {
                    // 提取顶级域名（如 passport.goofish.com -> goofish.com）
                    String topDomain = extractTopDomain(host);
                    result.add(new Cookie.Builder()
                            .name(entry.getKey())
                            .value(entry.getValue())
                            .domain(topDomain)
                            .path("/")
                            .build());
                } catch (Exception ex) {
                    // 如果还是失败，跳过这个Cookie
                    // 不影响其他Cookie的加载
                }
            }
        }
        return result;
    }
    
    /**
     * 提取顶级域名
     * 例如：passport.goofish.com -> goofish.com
     */
    private String extractTopDomain(String host) {
        if (host == null || host.isEmpty()) {
            return host;
        }
        
        String[] parts = host.split("\\.");
        if (parts.length >= 2) {
            // 返回最后两部分（如 goofish.com）
            return parts[parts.length - 2] + "." + parts[parts.length - 1];
        }
        return host;
    }

    /**
     * 创建预配置的OkHttpClient（绑定此CookieJar）
     * <p>
     * 每次调用创建新的OkHttpClient实例，确保Cookie隔离。
     */
    public OkHttpClient createHttpClient() {
        return new OkHttpClient.Builder()
                .cookieJar(this)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .build();
    }
}
