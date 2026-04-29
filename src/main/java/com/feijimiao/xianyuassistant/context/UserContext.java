package com.feijimiao.xianyuassistant.context;

/**
 * 用户上下文工具类
 * 使用ThreadLocal存储当前登录用户信息，供任意位置获取
 * 
 * <p>由AuthInterceptor在请求进入时设置，请求结束后清理</p>
 */
public class UserContext {
    
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    
    /**
     * 设置当前用户信息
     */
    public static void set(Long userId, String username) {
        USER_ID.set(userId);
        USERNAME.set(username);
    }
    
    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        return USER_ID.get();
    }
    
    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        return USERNAME.get();
    }
    
    /**
     * 清理当前用户信息（请求结束后调用）
     */
    public static void clear() {
        USER_ID.remove();
        USERNAME.remove();
    }
}
