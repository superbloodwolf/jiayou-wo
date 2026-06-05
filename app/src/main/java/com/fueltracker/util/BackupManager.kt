package com.fueltracker.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.fueltracker.data.AppDatabase
import com.fueltracker.data.FuelRecord
import com.fueltracker.ui.VehicleInfo
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 数据备份管理器
 * - 导出：生成JSON文件，通过系统分享发送
 * - 恢复：读取JSON文件，清空后批量插入
 */
class BackupManager(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val dao = db.fuelRecordDao()
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    /** 导出所有记录为JSON，并通过系统分享发送（车机无分享应用时保存到外部目录） */
    suspend fun exportAndShare(): Boolean = withContext(Dispatchers.IO) {
        try {
            val records = dao.getAllRecords()
            if (records.isEmpty()) {
                showToast("暂无记录可备份")
                return@withContext false
            }

            val vehicleInfo = loadVehicleInfo(context)
            val backup = BackupData(
                version = BACKUP_VERSION,
                exportTime = System.currentTimeMillis(),
                exportTimeFormatted = TimeUtils.formatDateTime(System.currentTimeMillis()),
                vehicleInfo = vehicleInfo,
                records = records
            )

            val json = gson.toJson(backup)
            val fileName = "fueltracker_backup_${LocalDateTime.now().format(dateFormatter)}.json"

            // 1. 先保存到应用外部文档目录（车机总能访问到）
            val publicDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: context.externalCacheDir
            val publicFile = File(publicDir, fileName)
            FileWriter(publicFile).use { it.write(json) }

            // 2. 尝试系统分享
            val cacheFile = File(context.externalCacheDir, fileName)
            FileWriter(cacheFile).use { it.write(json) }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "加油记录备份")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "FuelTracker 加油记录备份\n共 ${records.size} 条记录\n导出时间：${backup.exportTimeFormatted}"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            withContext(Dispatchers.Main) {
                val pm = context.packageManager
                val resolveInfo = pm.queryIntentActivities(intent, 0)
                if (resolveInfo.isNotEmpty()) {
                    // 有分享应用，启动选择器
                    val chooser = Intent.createChooser(intent, "分享备份文件到手机...")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                } else {
                    // 车机没有分享应用，提示用户文件保存位置
                    showToast("备份已保存，请用文件管理器导出：${publicFile.name}")
                }
            }
            true
        } catch (e: Exception) {
            showToast("备份失败：${e.message}")
            false
        }
    }

    /** 从URI读取JSON并恢复数据 */
    suspend fun restoreFromUri(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            } ?: return@withContext ImportResult.Error("无法读取文件")

            restoreFromJson(json)
        } catch (e: Exception) {
            ImportResult.Error("恢复失败：${e.message}")
        }
    }

    /** 从JSON字符串恢复数据 */
    suspend fun restoreFromJson(json: String): ImportResult = withContext(Dispatchers.IO) {
        try {
            val backup = gson.fromJson(json, BackupData::class.java)
                ?: return@withContext ImportResult.Error("备份文件解析失败")

            if (backup.version > BACKUP_VERSION) {
                return@withContext ImportResult.Error("备份版本(${backup.version})过高，请升级App")
            }

            val records = backup.records
            if (records.isEmpty()) {
                return@withContext ImportResult.Error("备份中没有记录")
            }

            // 将id重置为0，让Room重新自增，避免冲突
            val cleanedRecords = records.map { it.copy(id = 0) }

            // 清空旧数据并批量插入
            dao.clearAll()
            dao.insertAll(*cleanedRecords.toTypedArray())

            // 恢复车辆信息
            backup.vehicleInfo?.let { info ->
                saveVehicleInfo(context, info)
            }

            ImportResult.Success(records.size)
        } catch (e: Exception) {
            ImportResult.Error("恢复失败：${e.message}")
        }
    }

    /** 获取备份信息（记录数等），不恢复 */
    suspend fun peekBackupInfo(uri: Uri): BackupInfo? = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            } ?: return@withContext null

            val backup = gson.fromJson(json, BackupData::class.java) ?: return@withContext null
            BackupInfo(
                recordCount = backup.records.size,
                exportTime = backup.exportTimeFormatted ?: "未知",
                vehicleInfo = backup.vehicleInfo?.displayName ?: "未设置"
            )
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun showToast(msg: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val BACKUP_VERSION = 1
        const val MIME_TYPE_JSON = "application/json"
    }
}

// ── 数据类 ──

data class BackupData(
    val version: Int,
    val exportTime: Long,
    val exportTimeFormatted: String? = null,
    val vehicleInfo: VehicleInfo? = null,
    val records: List<FuelRecord> = emptyList()
)

data class BackupInfo(
    val recordCount: Int,
    val exportTime: String,
    val vehicleInfo: String
)

sealed class ImportResult {
    data class Success(val count: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

// ── 车辆信息读写（供BackupManager使用）──

private fun loadVehicleInfo(context: Context): VehicleInfo {
    val prefs = context.getSharedPreferences("vehicle_prefs", Context.MODE_PRIVATE)
    return VehicleInfo(
        brand = prefs.getString("brand", "") ?: "",
        model = prefs.getString("model", "") ?: "",
        year = prefs.getString("year", "") ?: ""
    )
}

private fun saveVehicleInfo(context: Context, info: VehicleInfo) {
    context.getSharedPreferences("vehicle_prefs", Context.MODE_PRIVATE)
        .edit()
        .putString("brand", info.brand)
        .putString("model", info.model)
        .putString("year", info.year)
        .apply()
}
