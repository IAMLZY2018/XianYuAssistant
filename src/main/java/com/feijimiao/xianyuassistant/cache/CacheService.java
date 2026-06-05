package com.feijimiao.xianyuassistant.cache;

import java.util.concurrent.TimeUnit;

/**
 * 通用缓存服务接口（策略模式）
 * 默认使用Map本地缓存，后续可切换为Redis实现
 * @author IAMLZY
 * @date 2026/4/22
 */
public interface CacheService {

    /**
     * 存入缓存
     */
    void put(String key, Object value);

    /**
     * 存入缓存（带过期时间）
     */
    void put(String key, Object value, long timeout, TimeUnit unit);

    /**
     * 获取缓存
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * 获取缓存（Object类型）
     */
    Object get(String key);

    /**
     * 删除缓存
     */
    void remove(String key);

    /**
     * 是否存在
     */
    boolean containsKey(String key);

    /**
     * 递增（用于计数器场景，如登录错误次数）
     * @return 递增后的值
     */
    long increment(String key);

    /**
     * 设置过期时间
     */
    void expire(String key, long timeout, TimeUnit unit);

    /**
     * 获取剩余过期时间（毫秒），-1表示永不过期，-2表示不存在
     */
    long getExpire(String key);
}
