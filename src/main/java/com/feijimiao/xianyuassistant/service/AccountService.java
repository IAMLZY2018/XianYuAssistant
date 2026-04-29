package com.feijimiao.xianyuassistant.service;

/**
 * 账号服务接口
 */
public interface AccountService {
    
    /**
     * 保存账号和Cookie信息
     *
     * @param accountNote 账号备注
     * @param unb UNB标识
     * @param cookieText Cookie字符串
     * @return 账号ID
     */
    Long saveAccountAndCookie(String accountNote, String unb, String cookieText);
    
    /**
     * 保存账号和Cookie信息（包含m_h5_tk）
     *
     * @param accountNote 账号备注
     * @param unb UNB标识
     * @param cookieText Cookie字符串
     * @param mH5Tk _m_h5_tk token
     * @return 账号ID
     */
    Long saveAccountAndCookie(String accountNote, String unb, String cookieText, String mH5Tk);
    
    /**
     * 根据账号ID获取Cookie
     *
     * @param accountId 账号ID
     * @return Cookie字符串
     */
    String getCookieByAccountId(Long accountId);
    
    /**
     * 根据UNB获取Cookie
     *
     * @param unb UNB标识
     * @return Cookie字符串
     */
    String getCookieByUnb(String unb);
    
    /**
     * 根据账号备注获取Cookie
     *
     * @param accountNote 账号备注
     * @return Cookie字符串
     */
    String getCookieByAccountNote(String accountNote);
    
    /**
     * 更新Cookie
     *
     * @param accountId 账号ID
     * @param cookieText 新的Cookie字符串
     * @return 是否成功
     */
    boolean updateCookie(Long accountId, String cookieText);
    
    /**
     * 根据账号ID获取m_h5_tk
     *
     * @param accountId 账号ID
     * @return m_h5_tk token
     */
    String getMh5tkByAccountId(Long accountId);
    
    /**
     * 根据账号备注获取账号ID
     *
     * @param accountNote 账号备注
     * @return 账号ID
     */
    Long getAccountIdByAccountNote(String accountNote);
    
    /**
     * 根据UNB获取账号ID
     *
     * @param unb UNB标识
     * @return 账号ID
     */
    Long getAccountIdByUnb(String unb);
    
    /**
     * 删除账号及其所有关联数据
     *
     * @param accountId 账号ID
     * @return 是否删除成功
     */
    boolean deleteAccountAndRelatedData(Long accountId);
    
    /**
     * 更新Cookie状态
     *
     * @param accountId 账号ID
     * @param cookieStatus Cookie状态 1:有效 2:过期 3:失效
     * @return 是否更新成功
     */
    boolean updateCookieStatus(Long accountId, Integer cookieStatus);
    
    /**
     * 更新账号Cookie（包含UNB更新）
     *
     * @param accountId 账号ID
     * @param unb UNB标识
     * @param cookieText Cookie字符串
     * @return 是否更新成功
     */
    boolean updateAccountCookie(Long accountId, String unb, String cookieText);
    
    /**
     * 获取或生成设备ID
     * 如果数据库中已有设备ID，直接返回；否则生成新的设备ID并保存
     *
     * @param accountId 账号ID
     * @param unb UNB标识（用于生成设备ID）
     * @return 设备ID
     */
    String getOrGenerateDeviceId(Long accountId, String unb);
    
    /**
     * 更新设备ID
     *
     * @param accountId 账号ID
     * @param deviceId 设备ID
     * @return 是否更新成功
     */
    boolean updateDeviceId(Long accountId, String deviceId);
    
    /**
     * 获取闲鱼用户ID（即UNB）
     *
     * @param accountId 账号ID
     * @return 闲鱼用户ID（UNB），如果不存在则返回null
     */
    String getXianyuUserId(Long accountId);
}
