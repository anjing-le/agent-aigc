package com.anjing.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * 日期工具类
 * 
 * @author Backend Template
 * @since 2025-01-20
 */
public class DateUtils {

    public static final String DEFAULT_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";
    public static final String DEFAULT_TIME_PATTERN = "HH:mm:ss";
    public static final ZoneId UTC_ZONE = TimeZoneUtils.defaultZoneId();

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATETIME_PATTERN);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATE_PATTERN);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_TIME_PATTERN);

    private DateUtils() {
        // 工具类，禁止实例化
    }

    /**
     * LocalDateTime转字符串
     * 
     * @param dateTime 时间
     * @return 字符串
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DATETIME_FORMATTER);
    }

    /**
     * LocalDateTime转字符串（自定义格式）
     * 
     * @param dateTime 时间
     * @param pattern  格式
     * @return 字符串
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 字符串转LocalDateTime
     * 
     * @param dateTimeStr 时间字符串
     * @return LocalDateTime
     */
    public static LocalDateTime parse(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
    }

    /**
     * 字符串转LocalDateTime（自定义格式）
     * 
     * @param dateTimeStr 时间字符串
     * @param pattern     格式
     * @return LocalDateTime
     */
    public static LocalDateTime parse(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Date转LocalDateTime
     * 
     * @param date Date对象
     * @return LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        return toLocalDateTime(date, UTC_ZONE);
    }

    /**
     * Date to LocalDateTime with explicit zone.
     *
     * @param date Date object
     * @param zoneId target zone
     * @return LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date date, ZoneId zoneId) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(zoneId == null ? UTC_ZONE : zoneId).toLocalDateTime();
    }

    /**
     * LocalDateTime转Date
     * 
     * @param dateTime LocalDateTime对象
     * @return Date
     */
    public static Date toDate(LocalDateTime dateTime) {
        return toDate(dateTime, UTC_ZONE);
    }

    /**
     * LocalDateTime to Date with explicit zone.
     *
     * @param dateTime LocalDateTime object
     * @param zoneId source zone
     * @return Date
     */
    public static Date toDate(LocalDateTime dateTime, ZoneId zoneId) {
        if (dateTime == null) {
            return null;
        }
        return Date.from(dateTime.atZone(zoneId == null ? UTC_ZONE : zoneId).toInstant());
    }

    /**
     * Current instant in UTC.
     *
     * @return Instant
     */
    public static Instant nowInstant() {
        return Instant.now();
    }

    /**
     * Current timestamp in epoch milliseconds.
     *
     * @return epoch milliseconds
     */
    public static long nowEpochMilli() {
        return nowInstant().toEpochMilli();
    }

    /**
     * Current offset datetime in UTC.
     *
     * @return OffsetDateTime
     */
    public static OffsetDateTime nowUtc() {
        return OffsetDateTime.ofInstant(nowInstant(), UTC_ZONE);
    }

    /**
     * Current local date time in the platform default zone.
     *
     * @return LocalDateTime
     */
    public static LocalDateTime nowLocalDateTime() {
        return LocalDateTime.ofInstant(nowInstant(), UTC_ZONE);
    }

    /**
     * Format instant as ISO-8601 UTC string.
     *
     * @param instant instant
     * @return ISO string
     */
    public static String formatIso(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    /**
     * Current instant as ISO-8601 UTC string.
     *
     * @return ISO UTC string
     */
    public static String nowIso() {
        return formatIso(nowInstant());
    }

    /**
     * Parse ISO-8601 instant string.
     *
     * @param value ISO string
     * @return Instant
     */
    public static Instant parseIso(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return Instant.parse(value);
    }

    /**
     * 获取当前时间字符串
     * 
     * @return 当前时间字符串
     */
    public static String now() {
        return LocalDateTime.now(UTC_ZONE).format(DATETIME_FORMATTER);
    }

    /**
     * 获取当前 UTC 时间字符串（自定义格式）
     *
     * @param pattern 格式
     * @return 当前 UTC 时间字符串
     */
    public static String now(String pattern) {
        return LocalDateTime.now(UTC_ZONE).format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 获取当前日期字符串
     * 
     * @return 当前日期字符串
     */
    public static String today() {
        return LocalDateTime.now(UTC_ZONE).format(DATE_FORMATTER);
    }

    /**
     * 计算两个时间之间的差值（秒）
     * 
     * @param start 开始时间
     * @param end   结束时间
     * @return 秒数
     */
    public static long betweenSeconds(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.SECONDS.between(start, end);
    }

    /**
     * 计算两个时间之间的差值（分钟）
     * 
     * @param start 开始时间
     * @param end   结束时间
     * @return 分钟数
     */
    public static long betweenMinutes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * 计算两个时间之间的差值（小时）
     * 
     * @param start 开始时间
     * @param end   结束时间
     * @return 小时数
     */
    public static long betweenHours(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(start, end);
    }

    /**
     * 计算两个时间之间的差值（天数）
     * 
     * @param start 开始时间
     * @param end   结束时间
     * @return 天数
     */
    public static long betweenDays(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 判断是否为今天
     * 
     * @param dateTime 时间
     * @return 是否为今天
     */
    public static boolean isToday(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now(UTC_ZONE);
        return dateTime.toLocalDate().equals(now.toLocalDate());
    }

    /**
     * 获取一天的开始时间
     * 
     * @param dateTime 时间
     * @return 一天的开始时间
     */
    public static LocalDateTime startOfDay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    /**
     * 获取一天的结束时间
     * 
     * @param dateTime 时间
     * @return 一天的结束时间
     */
    public static LocalDateTime endOfDay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
    }
}
