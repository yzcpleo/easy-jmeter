package io.github.guojiaxing1995.easyJmeter.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时区工具类
 * 统一使用东八区（Asia/Shanghai）时间
 *
 * @author Assistant
 */
public class TimeZoneUtil {
    
    /**
     * 东八区时区
     */
    public static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    
    /**
     * 时间格式化器（yyyy-MM-dd HH:mm:ss）
     */
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 获取当前东八区时间
     *
     * @return LocalDateTime（东八区时间）
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(BEIJING_ZONE);
    }
    
    /**
     * 将系统时间转换为东八区时间
     *
     * @param localDateTime 系统时间
     * @return 东八区时间
     */
    public static LocalDateTime toBeijingTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        // 如果已经是东八区时间，直接返回
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        return zonedDateTime.withZoneSameInstant(BEIJING_ZONE).toLocalDateTime();
    }
    
    /**
     * 格式化时间为字符串（东八区时间）
     *
     * @param localDateTime 时间
     * @return 格式化后的字符串（yyyy-MM-dd HH:mm:ss）
     */
    public static String format(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.format(DATETIME_FORMATTER);
    }
    
    /**
     * 解析时间字符串为东八区时间
     *
     * @param timeStr 时间字符串（yyyy-MM-dd HH:mm:ss）
     * @return LocalDateTime（东八区时间）
     */
    public static LocalDateTime parse(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        try {
            // 移除可能的T和Z
            timeStr = timeStr.replace("T", " ").replace("Z", "").trim();
            if (timeStr.length() > 19) {
                timeStr = timeStr.substring(0, 19);
            }
            return LocalDateTime.parse(timeStr, DATETIME_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
}

