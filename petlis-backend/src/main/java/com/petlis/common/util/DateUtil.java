package com.petlis.common.util;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 日期工具类
 *
 * 处理Unix时间戳和LocalDateTime之间的转换
 *
 * @author PETLIS Team
 */
public class DateUtil {

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Unix时间戳转LocalDateTime
     *
     * @param timestamp Unix时间戳(秒)
     * @return LocalDateTime
     */
    public static LocalDateTime fromTimestamp(Integer timestamp) {
        if (timestamp == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
    }

    /**
     * Unix时间戳转LocalDateTime(毫秒)
     *
     * @param timestamp Unix时间戳(毫秒)
     * @return LocalDateTime
     */
    public static LocalDateTime fromTimestampMillis(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    /**
     * LocalDateTime转Unix时间戳(秒)
     *
     * @param dateTime LocalDateTime
     * @return Unix时间戳(秒)
     */
    public static Integer toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return (int) dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    /**
     * LocalDateTime转Unix时间戳(毫秒)
     *
     * @param dateTime LocalDateTime
     * @return Unix时间戳(毫秒)
     */
    public static Long toTimestampMillis(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 格式化LocalDateTime
     *
     * @param dateTime LocalDateTime
     * @return 格式化字符串
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DEFAULT_FORMATTER);
    }

    /**
     * 格式化LocalDateTime(自定义格式)
     *
     * @param dateTime LocalDateTime
     * @param pattern 格式化模式
     * @return 格式化字符串
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return dateTime.format(formatter);
    }

    /**
     * 获取当前时间戳(秒)
     *
     * @return Unix时间戳
     */
    public static Integer currentTimestamp() {
        return (int) Instant.now().getEpochSecond();
    }

    /**
     * 获取当前时间戳(毫秒)
     *
     * @return Unix时间戳(毫秒)
     */
    public static Long currentTimestampMillis() {
        return Instant.now().toEpochMilli();
    }
}
