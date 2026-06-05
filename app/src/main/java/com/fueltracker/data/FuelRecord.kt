package com.fueltracker.data

import androidx.room.*

/**
 * 加油记录实体
 * 核心数据表，存储每次加油的完整信息
 */
@Entity(
    tableName = "fuel_records",
    indices = [Index(value = ["timestamp"], unique = false)]
)
data class FuelRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** 加油站类型：中石油 / 中石化 / 其他 */
    @ColumnInfo(name = "station_type")
    val stationType: String,

    /** 加油站名称（可选，如"中石油北京朝阳北路站"）*/
    @ColumnInfo(name = "station_name")
    val stationName: String = "",

    /** 油品等级：92号 / 95号 / 98号 / 柴油 */
    @ColumnInfo(name = "fuel_grade")
    val fuelGrade: String,

    /** 当日油价（元/升）*/
    @ColumnInfo(name = "fuel_price")
    val fuelPrice: Float,

    /** 加油量（升）*/
    @ColumnInfo(name = "liters")
    val liters: Float,

    /** 实际花费（元）*/
    @ColumnInfo(name = "total_cost")
    val totalCost: Float,

    /** 加油时里程表读数（总行驶里程 km）*/
    @ColumnInfo(name = "odometer")
    val odometer: Int,

    /** 加油里程 = 本次里程 - 上次里程（km），首次加油为 null */
    @ColumnInfo(name = "trip_mileage")
    val tripMileage: Int? = null,

    /** 加油时间（Unix 时间戳 ms）*/
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    /** 备注 */
    @ColumnInfo(name = "notes")
    val notes: String = ""
) {
    /** 本次行驶里程 = 本次里程 - 上次里程（需外部计算）*/
    fun distanceSinceLast(lastOdometer: Int?): Int? {
        if (lastOdometer == null) return null
        return (odometer - lastOdometer).coerceAtLeast(0)
    }

    /** 本次油耗 L/100km */
    fun fuelConsumption(distanceKm: Int?): Float? {
        if (distanceKm == null || distanceKm <= 0) return null
        return (liters / distanceKm) * 100f
    }
}
