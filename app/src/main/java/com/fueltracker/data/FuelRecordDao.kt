package com.fueltracker.data

import androidx.room.*

/**
 * 加油记录 DAO
 * 所有数据库操作封装在此
 */
@Dao
interface FuelRecordDao {

    /** 插入一条加油记录 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: FuelRecord): Long

    /** 更新记录 */
    @Update
    suspend fun update(record: FuelRecord)

    /** 删除记录 */
    @Delete
    suspend fun delete(record: FuelRecord)

    /** 获取所有记录（按时间倒序）*/
    @Query("SELECT * FROM fuel_records ORDER BY timestamp DESC")
    suspend fun getAllRecords(): List<FuelRecord>

    /** 获取最近一条记录（用于计算上次里程）*/
    @Query("""
        SELECT * FROM fuel_records 
        WHERE id != :excludeId 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    suspend fun getLastRecord(excludeId: Int = -1): FuelRecord?

    /** 按日期范围查询 */
    @Query("""
        SELECT * FROM fuel_records 
        WHERE timestamp BETWEEN :startTime AND :endTime 
        ORDER BY timestamp DESC
    """)
    suspend fun getRecordsBetween(startTime: Long, endTime: Long): List<FuelRecord>

    /** 获取汇总统计（本月）*/
    @Query("""
        SELECT 
            COUNT(*) as count,
            SUM(total_cost) as totalCost,
            SUM(liters) as totalLiters,
            AVG(liters) as avgLiters
        FROM fuel_records 
        WHERE timestamp BETWEEN :monthStart AND :monthEnd
    """)
    suspend fun getMonthlyStats(monthStart: Long, monthEnd: Long): MonthlyStats

    /** 获取所有记录用于统计（不限数量）*/
    @Query("SELECT * FROM fuel_records ORDER BY timestamp ASC")
    suspend fun getAllRecordsAsc(): List<FuelRecord>

    /** 记录总数 */
    @Query("SELECT COUNT(*) FROM fuel_records")
    suspend fun getCount(): Int

    /** 批量插入（用于恢复备份） */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg records: FuelRecord)

    /** 清空所有记录（用于恢复前清理） */
    @Query("DELETE FROM fuel_records")
    suspend fun clearAll()
}

/** 月度统计结果 */
data class MonthlyStats(
    val count: Int,
    val totalCost: Float?,
    val totalLiters: Float?,
    val avgLiters: Float?
)
