package com.fueltracker.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 车辆里程读取工具（基础版 - 降级为手动输入）
 *
 * ⚠️ 注意：
 * - 基础版不依赖 AAOS Car API，所有数据手动输入
 * - 进阶版可接入 Car API 自动读取里程
 * - 如需自动读取，需使用 AAOS 系统镜像编译
 */
class OdometerHelper(private val context: Context) {

    private val _odometer = MutableStateFlow<Float?>(null)
    val odometer = _odometer.asStateFlow()

    /** Car API 是否可用（基础版永远返回 false） */
    var isCarApiAvailable: Boolean = false
        private set

    /** 读取里程（基础版：提示用户手动输入） */
    fun readOdometer(
        onResult: (km: Float?, error: String?) -> Unit
    ) {
        // 基础版不读取车辆数据，直接返回不可用
        isCarApiAvailable = false
        onResult(null, "基础版：请手动输入里程表读数")
    }

    fun disconnect() {
        // 基础版无连接需要断开
    }
}

/**
 * 油价网络接口（通用 HTTP 封装）
 * 可接入聚合数据 / 易源等油价 API
 *
 * 建议使用方式：
 * 1. 调用免费油价接口（如 juhe.cn API）
 * 2. 失败则降级为手动输入
 */
object FuelPriceApi {

    /** 油价返回数据模型（聚合数据格式）*/
    data class FuelPriceResponse(
        val resultcode: String,
        val reason: String,
        val result: FuelPriceData?
    )

    data class FuelPriceData(
        val bj: CityPrice?,   // 北京
        val sh: CityPrice?,   // 上海
        val gd: CityPrice?,   // 广东
        // ... 其他省份
    )

    data class CityPrice(
        val b92: String,   // 92号价格
        val b95: String,   // 95号价格
        val b98: String,   // 98号
        val d0: String      // 柴油
    )

    /**
     * 离线备用油价（2026年6月参考价）
     * 无网络时作为默认参考值
     */
    fun getOfflinePrice(province: String, grade: String): Float {
        // 简单映射表，实际可扩展为本地 JSON
        return when (grade) {
            "92号" -> 7.85f
            "95号" -> 8.35f
            "98号" -> 9.18f
            else   -> 7.85f
        }
    }
}

