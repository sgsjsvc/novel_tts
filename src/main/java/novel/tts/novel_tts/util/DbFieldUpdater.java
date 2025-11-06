package novel.tts.novel_tts.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 通用 MySQL 整数字段加减工具类，确保字段不会小于0
 */
@Component
public class DbFieldUpdater {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 全表字段加减，确保字段 >= 0
     */
    public void updateField(String tableName, String fieldName, int delta) {
        updateField(tableName, fieldName, delta, null, null);
    }

    /**
     * 指定 WHERE 条件的字段加减，确保字段 >= 0
     */
    public void updateField(String tableName, String fieldName, int delta,
                            String whereField, Object whereValue) {

        // ✅ 防止 SQL 注入
        if (!tableName.matches("^[a-zA-Z0-9_]+$") || !fieldName.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("非法的表名或字段名");
        }
        if (whereField != null && !whereField.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("非法的 WHERE 字段名");
        }

        String operator = delta >= 0 ? "+" : "-";
        int absDelta = Math.abs(delta);

        // 使用 GREATEST 确保字段 >= 0
        String sql = String.format("UPDATE %s SET %s = GREATEST(%s %s ?, 0)", tableName, fieldName, fieldName, operator);

        if (whereField != null && whereValue != null) {
            sql += " WHERE " + whereField + " = ?";
            jdbcTemplate.update(sql, absDelta, whereValue);
        } else {
            jdbcTemplate.update(sql, absDelta);
        }
    }
}
