package com.fueltracker.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 时间工具类 - 固定使用北京时间（Asia/Shanghai），避免车机/模拟器时区设置错误
 */
object TimeUtils {

    /** 固定使用北京时间，避免设备时区设置问题 */
    val ZONE: ZoneId = ZoneId.of("Asia/Shanghai")

    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /** 当前时间戳（毫秒） */
    fun nowMillis(): Long = System.currentTimeMillis()

    /** 格式化日期 yyyy-MM-dd */
    fun formatDate(timestamp: Long): String {
        val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZONE)
        return dt.format(DATE_FORMATTER)
    }

    /** 格式化日期时间 yyyy-MM-dd HH:mm */
    fun formatDateTime(timestamp: Long): String {
        val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZONE)
        return dt.format(DATE_TIME_FORMATTER)
    }

    /** 获取今天的开始时间戳 */
    fun todayStartMillis(): Long {
        val now = LocalDateTime.now(ZONE)
        return now.withHour(0).withMinute(0).withSecond(0)
            .atZone(ZONE).toInstant().toEpochMilli()
    }

    /** 获取今天的结束时间戳 */
    fun todayEndMillis(): Long {
        val now = LocalDateTime.now(ZONE)
        return now.withHour(23).withMinute(59).withSecond(59)
            .atZone(ZONE).toInstant().toEpochMilli()
    }
}
