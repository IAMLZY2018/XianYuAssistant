package com.feijimiao.xianyuassistant.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

/**
 * 数据库初始化监听器
 * 自动解析schema.sql文件并迁移数据库
 */
@Slf4j
@Component
public class DatabaseInitListener implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private SqlSchemaParser schemaParser;
    
    @Autowired
    private Environment environment;
    
    private static final String SCHEMA_FILE = "sql/schema.sql";

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 打印数据库文件路径
        try (Connection conn = dataSource.getConnection()) {
            String url = conn.getMetaData().getURL();
            String dbPath = url.replace("jdbc:sqlite:", "");
            File dbFile = new File(dbPath);
            log.info("数据库文件路径: {}", dbFile.getCanonicalPath());
        } catch (Exception e) {
            log.warn("获取数据库文件路径失败: {}", e.getMessage());
        }
        
        log.info("=".repeat(60));
        log.info("开始检查数据库表和字段（基于schema.sql）...");
        log.info("=".repeat(60));
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // 1. 解析schema.sql文件
            log.info("📄 解析Schema文件: {}", SCHEMA_FILE);
            SqlSchemaParser.SchemaDefinition schema = schemaParser.parseSchemaFile(SCHEMA_FILE);
            
            // 2. 检查并创建缺失的表
            checkAndCreateTables(stmt, schema);
            
            // 3. 检查并添加缺失的字段
            checkAndAddColumns(stmt, schema);
            
            // 4. 检查并创建缺失的索引
            checkAndCreateIndexes(stmt, schema);
            
            // 5. 检查并创建缺失的触发器
            checkAndCreateTriggers(stmt, schema);
            
            log.info("=".repeat(60));
            log.info("数据库表和字段已完善，开始检查数据库完整性...");
            log.info("=".repeat(60));
            
            // 验证数据库状态
            verifyDatabase(stmt);
            
            log.info("=".repeat(60));
            log.info("✅ 数据库完整性检查完成，系统就绪！");
            log.info("=".repeat(60));
            
            // 打印访问地址
            printAccessUrl();
            
        } catch (Exception e) {
            log.error("数据库初始化失败", e);
        }
    }
    
    /**
     * 检查并创建缺失的表
     */
    private void checkAndCreateTables(Statement stmt, SqlSchemaParser.SchemaDefinition schema) throws Exception {
        log.info("🔍 检查数据库表...");
        
        // 获取现有表列表
        Set<String> existingTables = new HashSet<>();
        ResultSet tables = stmt.executeQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"
        );
        while (tables.next()) {
            existingTables.add(tables.getString("name"));
        }
        tables.close();
        
        // 检查并创建缺失的表
        int createdCount = 0;
        for (Map.Entry<String, SqlSchemaParser.TableDefinition> entry : schema.getTables().entrySet()) {
            String tableName = entry.getKey();
            SqlSchemaParser.TableDefinition table = entry.getValue();
            
            if (!existingTables.contains(tableName)) {
                log.info("  ➕ 创建表: {}", tableName);
                stmt.execute(table.getCreateSql());
                createdCount++;
            } else {
                log.info("  ✓ 表已存在: {}", tableName);
            }
        }
        
        if (createdCount > 0) {
            log.info("✅ 创建了 {} 个新表", createdCount);
        }
    }
    
    /**
     * 检查并添加缺失的字段
     */
    private void checkAndAddColumns(Statement stmt, SqlSchemaParser.SchemaDefinition schema) throws Exception {
        log.info("🔍 检查表字段...");
        
        int addedCount = 0;
        int checkedCount = 0;
        
        for (Map.Entry<String, SqlSchemaParser.TableDefinition> entry : schema.getTables().entrySet()) {
            String tableName = entry.getKey();
            SqlSchemaParser.TableDefinition table = entry.getValue();
            
            // 检查表是否存在
            if (!tableExists(stmt, tableName)) {
                log.debug("  ⏭️ 表不存在，跳过字段检查: {}", tableName);
                continue;
            }
            
            // 获取表的现有字段
            Set<String> existingColumns = getTableColumns(stmt, tableName);
            log.debug("  📋 表 {} 现有字段: {}", tableName, existingColumns);
            
            // 获取schema中定义的字段
            List<SqlSchemaParser.ColumnDefinition> schemaColumns = table.getColumns();
            log.debug("  📄 表 {} Schema定义字段数: {}", tableName, schemaColumns.size());
            
            // 检查并添加缺失的字段
            for (SqlSchemaParser.ColumnDefinition column : schemaColumns) {
                checkedCount++;
                String columnName = column.getName().toLowerCase();
                
                if (!existingColumns.contains(columnName)) {
                    log.info("  ➕ 添加字段: {}.{}", tableName, column.getName());
                    
                    // 构建ALTER TABLE语句
                    StringBuilder alterSql = new StringBuilder();
                    alterSql.append("ALTER TABLE ").append(tableName);
                    alterSql.append(" ADD COLUMN ").append(column.getName());
                    alterSql.append(" ").append(column.getType());
                    
                    // 添加约束（如果有）
                    if (column.getConstraints() != null && !column.getConstraints().isEmpty()) {
                        alterSql.append(" ").append(column.getConstraints());
                    }
                    
                    String sql = alterSql.toString().trim();
                    log.debug("  📝 执行SQL: {}", sql);
                    
                    try {
                        stmt.execute(sql);
                        addedCount++;
                        log.info("  ✅ 字段添加成功: {}.{}", tableName, column.getName());
                    } catch (Exception e) {
                        log.error("  ❌ 添加字段失败: {}.{}, SQL: {}", tableName, column.getName(), sql, e);
                    }
                } else {
                    log.debug("  ✓ 字段已存在: {}.{}", tableName, column.getName());
                }
            }
        }
        
        log.info("📊 字段检查统计: 检查 {} 个字段, 添加 {} 个新字段", checkedCount, addedCount);
        if (addedCount > 0) {
            log.info("✅ 添加了 {} 个新字段", addedCount);
        } else {
            log.info("✓ 所有字段都已存在");
        }
    }
    
    /**
     * 检查并创建缺失的索引
     */
    private void checkAndCreateIndexes(Statement stmt, SqlSchemaParser.SchemaDefinition schema) throws Exception {
        log.info("🔍 检查数据库索引...");
        
        // 获取现有索引
        Set<String> existingIndexes = new HashSet<>();
        ResultSet indexes = stmt.executeQuery(
            "SELECT name FROM sqlite_master WHERE type='index' AND name NOT LIKE 'sqlite_%'"
        );
        while (indexes.next()) {
            existingIndexes.add(indexes.getString("name"));
        }
        indexes.close();
        
        // 检查并创建缺失的索引
        int createdCount = 0;
        for (Map.Entry<String, SqlSchemaParser.IndexDefinition> entry : schema.getIndexes().entrySet()) {
            String indexName = entry.getKey();
            SqlSchemaParser.IndexDefinition index = entry.getValue();
            
            if (!existingIndexes.contains(indexName)) {
                log.info("  ➕ 创建索引: {}", indexName);
                stmt.execute(index.getCreateSql());
                createdCount++;
            } else {
                log.debug("  ✓ 索引已存在: {}", indexName);
            }
        }
        
        if (createdCount > 0) {
            log.info("✅ 创建了 {} 个新索引", createdCount);
        } else {
            log.info("✓ 所有索引都已存在");
        }
    }
    
    /**
     * 检查并创建缺失的触发器
     */
    private void checkAndCreateTriggers(Statement stmt, SqlSchemaParser.SchemaDefinition schema) throws Exception {
        log.info("🔍 检查数据库触发器...");
        
        // 获取现有触发器
        Set<String> existingTriggers = new HashSet<>();
        ResultSet triggers = stmt.executeQuery(
            "SELECT name FROM sqlite_master WHERE type='trigger'"
        );
        while (triggers.next()) {
            existingTriggers.add(triggers.getString("name"));
        }
        triggers.close();
        
        // 检查并创建缺失的触发器
        int createdCount = 0;
        for (Map.Entry<String, SqlSchemaParser.TriggerDefinition> entry : schema.getTriggers().entrySet()) {
            String triggerName = entry.getKey();
            SqlSchemaParser.TriggerDefinition trigger = entry.getValue();
            
            if (!existingTriggers.contains(triggerName)) {
                log.info("  ➕ 创建触发器: {}", triggerName);
                stmt.execute(trigger.getCreateSql());
                createdCount++;
            } else {
                log.debug("  ✓ 触发器已存在: {}", triggerName);
            }
        }
        
        if (createdCount > 0) {
            log.info("✅ 创建了 {} 个新触发器", createdCount);
        } else {
            log.info("✓ 所有触发器都已存在");
        }
    }
    
    /**
     * 验证数据库完整性
     * 检查所有表是否存在，并统计记录数
     */
    private void verifyDatabase(Statement stmt) throws Exception {
        // 查询表信息
        ResultSet tables = stmt.executeQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name"
        );
        
        log.info("📊 数据库表列表:");
        while (tables.next()) {
            String tableName = tables.getString("name");
            
            // 查询表的记录数
            ResultSet count = stmt.executeQuery("SELECT COUNT(*) as cnt FROM " + tableName);
            int recordCount = 0;
            if (count.next()) {
                recordCount = count.getInt("cnt");
            }
            count.close();
            
            log.info("  ✓ {} (记录数: {})", tableName, recordCount);
        }
        tables.close();
    }
    
    /**
     * 检查表是否存在
     */
    private boolean tableExists(Statement stmt, String tableName) throws Exception {
        ResultSet rs = stmt.executeQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "'"
        );
        boolean exists = rs.next();
        rs.close();
        return exists;
    }
    
    /**
     * 获取表的所有字段名
     */
    private Set<String> getTableColumns(Statement stmt, String tableName) throws Exception {
        Set<String> columns = new HashSet<>();
        ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")");
        while (rs.next()) {
            columns.add(rs.getString("name").toLowerCase());
        }
        rs.close();
        return columns;
    }
    
    /**
     * 打印访问地址
     */
    private void printAccessUrl() {
        try {
            // 获取端口
            Integer port = environment.getProperty("server.port", Integer.class, 8080);
            
            // 获取所有可用的IP地址
            List<String> ipAddresses = getAvailableIpAddresses();
            
            log.info("=".repeat(60));
            log.info("🚀 程序已经启动，可以通过以下地址访问：");
            log.info("=".repeat(60));
            
            // 打印localhost
            log.info("  🌐 http://localhost:{}", port);
            log.info("  🌐 http://127.0.0.1:{}", port);
            
            // 打印所有可用的IP地址
            for (String ip : ipAddresses) {
                log.info("  🌐 http://{}:{}", ip, port);
            }
            
            log.info("=".repeat(60));
            
        } catch (Exception e) {
            log.warn("获取访问地址失败: {}", e.getMessage());
            // 使用默认端口
            log.info("🚀 程序已经启动，可以访问：http://localhost:8080");
        }
    }
    
    /**
     * 获取所有可用的IP地址
     */
    private List<String> getAvailableIpAddresses() {
        List<String> ipAddresses = new ArrayList<>();
        
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                
                // 跳过回环接口和未启用的接口
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    
                    // 只处理IPv4地址
                    if (inetAddress instanceof java.net.Inet4Address) {
                        String ip = inetAddress.getHostAddress();
                        
                        // 跳过127.0.0.1
                        if (!"127.0.0.1".equals(ip)) {
                            ipAddresses.add(ip);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("获取网络接口失败: {}", e.getMessage());
        }
        
        return ipAddresses;
    }
}
