package com.feijimiao.xianyuassistant.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL Schema解析器
 * 自动解析schema.sql文件，提取表、字段、索引、触发器定义
 */
@Slf4j
@Component
public class SqlSchemaParser {

    /**
     * 解析schema.sql文件
     */
    public SchemaDefinition parseSchemaFile(String schemaFilePath) {
        try {
            log.info("开始解析Schema文件: {}", schemaFilePath);
            
            ClassPathResource resource = new ClassPathResource(schemaFilePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            
            String sqlContent = content.toString();
            
            // 移除SQL注释（--开头的行）
            sqlContent = removeComments(sqlContent);
            
            SchemaDefinition schema = new SchemaDefinition();
            
            // 解析表定义
            schema.setTables(parseTables(sqlContent));
            log.info("解析到 {} 个表定义", schema.getTables().size());
            
            // 解析索引定义
            schema.setIndexes(parseIndexes(sqlContent));
            log.info("解析到 {} 个索引定义", schema.getIndexes().size());
            
            // 解析触发器定义
            schema.setTriggers(parseTriggers(sqlContent));
            log.info("解析到 {} 个触发器定义", schema.getTriggers().size());
            
            return schema;
            
        } catch (Exception e) {
            log.error("解析Schema文件失败: {}", schemaFilePath, e);
            throw new RuntimeException("解析Schema文件失败", e);
        }
    }
    
    /**
     * 移除SQL注释
     */
    private String removeComments(String sql) {
        // 移除单行注释 -- comment
        String result = sql.replaceAll("--.*?\\n", "\n");
        // 移除多行注释 /* comment */
        result = result.replaceAll("/\\*.*?\\*/", "");
        return result;
    }
    
    /**
     * 解析表定义
     */
    private Map<String, TableDefinition> parseTables(String sqlContent) {
        Map<String, TableDefinition> tables = new LinkedHashMap<>();
        
        // 匹配CREATE TABLE语句
        // 支持多行定义，直到遇到);结束
        Pattern tablePattern = Pattern.compile(
            "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)\\s*\\((.*?)\\);",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = tablePattern.matcher(sqlContent);
        
        while (matcher.find()) {
            String tableName = matcher.group(1);
            String tableBody = matcher.group(2);
            
            TableDefinition table = new TableDefinition();
            table.setName(tableName);
            table.setCreateSql(matcher.group(0));
            
            // 解析字段
            table.setColumns(parseColumns(tableBody));
            
            tables.put(tableName, table);
        }
        
        return tables;
    }
    
    /**
     * 解析字段定义
     */
    private List<ColumnDefinition> parseColumns(String tableBody) {
        List<ColumnDefinition> columns = new ArrayList<>();
        
        // 分割字段定义（考虑括号嵌套）
        List<String> parts = splitByComma(tableBody);
        
        for (String part : parts) {
            part = part.trim();
            
            if (part.isEmpty()) {
                continue;
            }
            
            // 跳过约束定义（PRIMARY KEY, FOREIGN KEY, UNIQUE, CHECK等）
            if (part.toUpperCase().startsWith("PRIMARY KEY") ||
                part.toUpperCase().startsWith("FOREIGN KEY") ||
                part.toUpperCase().startsWith("UNIQUE") ||
                part.toUpperCase().startsWith("CHECK") ||
                part.toUpperCase().startsWith("CONSTRAINT")) {
                continue;
            }
            
            // 解析字段
            ColumnDefinition column = parseColumn(part);
            if (column != null) {
                columns.add(column);
            }
        }
        
        return columns;
    }
    
    /**
     * 解析单个字段
     */
    private ColumnDefinition parseColumn(String columnDef) {
        // 字段格式: name type [constraints]
        // 例如: id INTEGER PRIMARY KEY AUTOINCREMENT
        // 例如: account_note VARCHAR(100)
        
        Pattern pattern = Pattern.compile("^(\\w+)\\s+(\\w+(?:\\([^)]*\\))?)(.*)$");
        Matcher matcher = pattern.matcher(columnDef);
        
        if (matcher.find()) {
            ColumnDefinition column = new ColumnDefinition();
            column.setName(matcher.group(1));
            column.setType(matcher.group(2));
            column.setConstraints(matcher.group(3).trim());
            column.setDefinition(columnDef);
            
            return column;
        }
        
        return null;
    }
    
    /**
     * 按逗号分割，考虑括号嵌套
     */
    private List<String> splitByComma(String str) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        
        for (char c : str.toCharArray()) {
            if (c == '(') {
                depth++;
                current.append(c);
            } else if (c == ')') {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0) {
                parts.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        
        return parts;
    }
    
    /**
     * 解析索引定义
     */
    private Map<String, IndexDefinition> parseIndexes(String sqlContent) {
        Map<String, IndexDefinition> indexes = new LinkedHashMap<>();
        
        // 匹配CREATE INDEX语句
        Pattern indexPattern = Pattern.compile(
            "CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)\\s+ON\\s+(\\w+)\\s*\\(([^)]+)\\);",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = indexPattern.matcher(sqlContent);
        
        while (matcher.find()) {
            String indexName = matcher.group(1);
            String tableName = matcher.group(2);
            String columns = matcher.group(3);
            
            IndexDefinition index = new IndexDefinition();
            index.setName(indexName);
            index.setTableName(tableName);
            index.setColumns(Arrays.asList(columns.split(",")));
            index.setCreateSql(matcher.group(0));
            index.setUnique(matcher.group(0).toUpperCase().contains("UNIQUE"));
            
            indexes.put(indexName, index);
        }
        
        return indexes;
    }
    
    /**
     * 解析触发器定义
     */
    private Map<String, TriggerDefinition> parseTriggers(String sqlContent) {
        Map<String, TriggerDefinition> triggers = new LinkedHashMap<>();
        
        // 匹配CREATE TRIGGER语句
        // 注意：触发器可能使用$$作为分隔符
        Pattern triggerPattern = Pattern.compile(
            "CREATE\\s+TRIGGER\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(\\w+)\\s+(.*?)\\s+BEGIN\\s+(.*?)\\s+END;?",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = triggerPattern.matcher(sqlContent);
        
        while (matcher.find()) {
            String triggerName = matcher.group(1);
            String triggerHeader = matcher.group(2);
            String triggerBody = matcher.group(3);
            
            TriggerDefinition trigger = new TriggerDefinition();
            trigger.setName(triggerName);
            trigger.setDefinition(matcher.group(0));
            trigger.setCreateSql("CREATE TRIGGER IF NOT EXISTS " + triggerName + " " + 
                               triggerHeader + " BEGIN " + triggerBody + " END");
            
            triggers.put(triggerName, trigger);
        }
        
        return triggers;
    }
    
    /**
     * Schema定义
     */
    public static class SchemaDefinition {
        private Map<String, TableDefinition> tables;
        private Map<String, IndexDefinition> indexes;
        private Map<String, TriggerDefinition> triggers;
        
        public Map<String, TableDefinition> getTables() { return tables; }
        public void setTables(Map<String, TableDefinition> tables) { this.tables = tables; }
        
        public Map<String, IndexDefinition> getIndexes() { return indexes; }
        public void setIndexes(Map<String, IndexDefinition> indexes) { this.indexes = indexes; }
        
        public Map<String, TriggerDefinition> getTriggers() { return triggers; }
        public void setTriggers(Map<String, TriggerDefinition> triggers) { this.triggers = triggers; }
    }
    
    /**
     * 表定义
     */
    public static class TableDefinition {
        private String name;
        private String createSql;
        private List<ColumnDefinition> columns;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getCreateSql() { return createSql; }
        public void setCreateSql(String createSql) { this.createSql = createSql; }
        
        public List<ColumnDefinition> getColumns() { return columns; }
        public void setColumns(List<ColumnDefinition> columns) { this.columns = columns; }
    }
    
    /**
     * 字段定义
     */
    public static class ColumnDefinition {
        private String name;
        private String type;
        private String constraints;
        private String definition;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getConstraints() { return constraints; }
        public void setConstraints(String constraints) { this.constraints = constraints; }
        
        public String getDefinition() { return definition; }
        public void setDefinition(String definition) { this.definition = definition; }
    }
    
    /**
     * 索引定义
     */
    public static class IndexDefinition {
        private String name;
        private String tableName;
        private List<String> columns;
        private boolean unique;
        private String createSql;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        
        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }
        
        public boolean isUnique() { return unique; }
        public void setUnique(boolean unique) { this.unique = unique; }
        
        public String getCreateSql() { return createSql; }
        public void setCreateSql(String createSql) { this.createSql = createSql; }
    }
    
    /**
     * 触发器定义
     */
    public static class TriggerDefinition {
        private String name;
        private String definition;
        private String createSql;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDefinition() { return definition; }
        public void setDefinition(String definition) { this.definition = definition; }
        
        public String getCreateSql() { return createSql; }
        public void setCreateSql(String createSql) { this.createSql = createSql; }
    }
}
