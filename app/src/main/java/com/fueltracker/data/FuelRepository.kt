package com.fueltracker.data

import com.fueltracker.util.TimeUtils
import kotlinx.coroutines.flow.*
import java.time.*

/**
 * 数据仓库层
 * 统一数据访问入口，隔离数据源（数据库 + 网络 + 车辆 API）
 */
class FuelRepository(private val db: AppDatabase) {

    private val dao = db.fuelRecordDao()

    // ── 记录 CRUD ──

    suspend fun addRecord(record: FuelRecord): Long {
        // 自动计算加油里程（tripMileage）
        val last = getLastRecord(excludeId = record.id)
        val tripMileage = if (last != null && record.odometer > last.odometer) {
            record.odometer - last.odometer
        } else null

        val recordWithTrip = if (tripMileage != null) {
            FuelRecord(
                id = record.id,
                stationType = record.stationType,
                stationName = record.stationName,
                fuelGrade = record.fuelGrade,
                fuelPrice = record.fuelPrice,
                liters = record.liters,
                totalCost = record.totalCost,
                odometer = record.odometer,
                tripMileage = tripMileage,
                timestamp = record.timestamp,
                notes = record.notes
            )
        } else record

        return dao.insert(recordWithTrip)
    }

    suspend fun deleteRecord(record: FuelRecord) =
        dao.delete(record)

    suspend fun updateRecord(record: FuelRecord) =
        dao.update(record)

    fun getAllRecordsFlow(): Flow<List<FuelRecord>> =
        flow { emit(dao.getAllRecords()) }

    suspend fun getAllRecords(): List<FuelRecord> =
        dao.getAllRecords()

    suspend fun getLastRecord(excludeId: Int = -1): FuelRecord? =
        dao.getLastRecord(excludeId)

    // ── 统计计算 ──

    /** 计算每次记录的行驶里程和油耗（依赖前一条记录）*/
    suspend fun getRecordsWithStats(): List<RecordWithStats> {
        val records = dao.getAllRecordsAsc()
        return records.mapIndexed { index, record ->
            val prev = records.getOrNull(index - 1)
            // 优先使用已存储的 tripMileage，否则实时计算
            val distance = record.tripMileage ?: prev?.let { record.odometer - it.odometer }
            val consumption = distance?.let {
                if (it > 0) (record.liters / it) * 100f else null
            }
            RecordWithStats(
                record = record,
                distanceKm = distance,
                consumptionL100km = consumption
            )
        }.reversed() // 返回倒序（最新在前）
    }

    /** 本月统计 */
    suspend fun getCurrentMonthStats(): MonthStatsData {
        val now = LocalDate.now(TimeUtils.ZONE)
        val monthStart = now.withDayOfMonth(1).atStartOfDay(TimeUtils.ZONE).toEpochSecond() * 1000
        val monthEnd = now.plusMonths(1).withDayOfMonth(1).atStartOfDay(TimeUtils.ZONE).toEpochSecond() * 1000 - 1

        // 获取本月记录（升序，用于计算油耗）
        val records = dao.getAllRecordsAsc()
            .filter { it.timestamp in monthStart..monthEnd }

        // 计算每条记录的油耗，取有油耗数据的平均值
        val consumptions = records.mapIndexed { index, record ->
            val prev = records.getOrNull(index - 1)
            val distance = record.tripMileage ?: prev?.let { record.odometer - it.odometer }
            distance?.let { if (it > 0) (record.liters / it) * 100f else null }
        }.filterNotNull()

        val avgConsumption = if (consumptions.isNotEmpty()) {
            consumptions.sum() / consumptions.size
        } else 0f

        val stats = dao.getMonthlyStats(monthStart, monthEnd)
        return MonthStatsData(
            count = stats.count,
            totalCost = stats.totalCost ?: 0f,
            totalLiters = stats.totalLiters ?: 0f,
            avgLiters = avgConsumption
        )
    }

    /** 近 N 次油耗趋势（用于图表）*/
    suspend fun getRecentConsumption(limit: Int = 10): List<Float> {
        val records = dao.getAllRecords()
        return records.take(limit).reversed().mapNotNull { record ->
            val prev = dao.getAllRecords().find { it.timestamp < record.timestamp }
            val distance = prev?.let { record.odometer - it.odometer }
            distance?.let { if (it > 0) (record.liters / it) * 100f else null }
        }
    }

    // ── 查询功能 ──

    /** 按时段查询加油记录 */
    suspend fun getRecordsInRange(startTs: Long, endTs: Long): List<RecordWithStats> {
        val allRecords = dao.getAllRecordsAsc()
        val filtered = allRecords.filter { it.timestamp in startTs..endTs }
        return filtered.mapIndexed { index, record ->
            // 在过滤后的列表里找前一条
            val prevInFiltered = filtered.getOrNull(index - 1)
            val distance = prevInFiltered?.let { record.odometer - it.odometer }
            val consumption = distance?.let {
                if (it > 0) (record.liters / it) * 100f else null
            }
            RecordWithStats(
                record = record,
                distanceKm = distance,
                consumptionL100km = consumption
            )
        }.reversed()
    }

    /** 按时段查询油价变化趋势 */
    suspend fun getPriceTrend(startTs: Long, endTs: Long): List<com.fueltracker.ui.PricePoint> {
        val allRecords = dao.getAllRecordsAsc().filter { it.timestamp in startTs..endTs }
        return allRecords.map { r ->
            com.fueltracker.ui.PricePoint(
                date = TimeUtils.formatDate(r.timestamp),
                station = r.stationType,
                grade = r.fuelGrade,
                price = r.fuelPrice
            )
        }
    }
}

// ── 数据包装类 ──

data class RecordWithStats(
    val record: FuelRecord,
    val distanceKm: Int?,
    val consumptionL100km: Float?,
    /** 每公里花费（元/km），行驶里程有效时计算 */
    val costPerKm: Float? = distanceKm?.let {
        if (it > 0) record.totalCost / it else null
    }
)

data class MonthStatsData(
    val count: Int,
    val totalCost: Float,
    val totalLiters: Float,
    val avgLiters: Float
)
