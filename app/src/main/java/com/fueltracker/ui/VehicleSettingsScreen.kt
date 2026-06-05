package com.fueltracker.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.fueltracker.util.BackupInfo
import com.fueltracker.util.BackupManager
import com.fueltracker.util.ImportResult
import kotlinx.coroutines.launch

/**
 * 设置页面 - 深色车机主题（2.2x 放大版）
 * 包含：车辆信息设置 + 数据备份/恢复
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("vehicle_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val backupManager = remember { BackupManager(context) }

    // ── 车辆信息状态 ──
    var brand by remember { mutableStateOf(prefs.getString("brand", "") ?: "") }
    var model by remember { mutableStateOf(prefs.getString("model", "") ?: "") }
    var year by remember { mutableStateOf(prefs.getString("year", "") ?: "") }
    var vehicleSaved by remember { mutableStateOf(false) }

    // ── 漏记提醒阈值 ──
    var thresholdStr by remember { mutableStateOf(prefs.getInt("missed_record_threshold", 650).toString()) }
    var thresholdSaved by remember { mutableStateOf(false) }

    // ── 备份/恢复状态 ──
    var backupInfo by remember { mutableStateOf<BackupInfo?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var restoreResult by remember { mutableStateOf<ImportResult?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }

    // ── 文件选择器（恢复用）──
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isRestoring = true
                backupInfo = backupManager.peekBackupInfo(it)
                restoreUri = it
                showRestoreConfirm = true
                isRestoring = false
            }
        }
    }

    Scaffold(
        containerColor = AppColors.BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Text("设置", fontSize = scaledSp(44), fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(106.dp)) {
                        Text("←", fontSize = scaledSp(53), color = AppColors.TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.BgDark,
                    titleContentColor = AppColors.TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 35.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(35.dp)
        ) {
            // ═════════════════════════════════════
            //  车辆信息
            // ═════════════════════════════════════
            SectionTitle("🚗 车辆信息")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(31.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.CardDark)
            ) {
                Column(
                    modifier = Modifier.padding(35.dp),
                    verticalArrangement = Arrangement.spacedBy(26.dp)
                ) {
                    SettingTextField(
                        label = "品牌",
                        value = brand,
                        onValueChange = { brand = it; vehicleSaved = false },
                        placeholder = "如：哈弗、丰田..."
                    )
                    SettingTextField(
                        label = "型号",
                        value = model,
                        onValueChange = { model = it; vehicleSaved = false },
                        placeholder = "如：大狗、卡罗拉..."
                    )
                    SettingTextField(
                        label = "年份",
                        value = year,
                        onValueChange = {
                            year = it.filter { c -> c.isDigit() }.take(4)
                            vehicleSaved = false
                        },
                        placeholder = "如：2020"
                    )

                    Button(
                        onClick = {
                            prefs.edit()
                                .putString("brand", brand)
                                .putString("model", model)
                                .putString("year", year)
                                .apply()
                            vehicleSaved = true
                        },
                        modifier = Modifier.fillMaxWidth().height(106.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Accent,
                            contentColor = AppColors.TextOnAccent
                        )
                    ) {
                        Text(
                            if (vehicleSaved) "已保存 ✓" else "保存车辆信息",
                            fontSize = scaledSp(35), fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ═════════════════════════════════════
            //  提醒设置
            // ═════════════════════════════════════
            SectionTitle("⚠ 提醒设置")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(31.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.CardDark)
            ) {
                Column(
                    modifier = Modifier.padding(35.dp),
                    verticalArrangement = Arrangement.spacedBy(26.dp)
                ) {
                    Text(
                        "当本次里程比上次增加超过以下数值时，提示可能漏记加油记录。",
                        fontSize = scaledSp(29),
                        color = AppColors.TextMuted,
                        lineHeight = 40.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        OutlinedTextField(
                            value = thresholdStr,
                            onValueChange = {
                                thresholdStr = it.filter { c -> c.isDigit() }.take(4)
                                thresholdSaved = false
                            },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = scaledSp(33), color = AppColors.TextPrimary
                            ),
                            suffix = { Text("km", fontSize = scaledSp(29), color = AppColors.TextSecondary) },
                            modifier = Modifier.weight(1f).heightIn(min = 114.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppColors.Accent,
                                unfocusedBorderColor = AppColors.Border,
                                focusedTextColor = AppColors.TextPrimary,
                                unfocusedTextColor = AppColors.TextPrimary,
                                focusedContainerColor = AppColors.BgSurface,
                                unfocusedContainerColor = AppColors.BgSurface,
                                cursorColor = AppColors.Accent
                            )
                        )
                        Button(
                            onClick = {
                                val value = thresholdStr.toIntOrNull() ?: 650
                                val clamped = value.coerceIn(100, 5000)
                                prefs.edit().putInt("missed_record_threshold", clamped).apply()
                                thresholdStr = clamped.toString()
                                thresholdSaved = true
                            },
                            modifier = Modifier.height(106.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Accent,
                                contentColor = AppColors.TextOnAccent
                            )
                        ) {
                            Text(
                                if (thresholdSaved) "已保存 ✓" else "保存",
                                fontSize = scaledSp(31), fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ═════════════════════════════════════
            //  显示设置
            // ═════════════════════════════════════
            SectionTitle("🔤 显示设置")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(31.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.CardDark)
            ) {
                Column(
                    modifier = Modifier.padding(35.dp),
                    verticalArrangement = Arrangement.spacedBy(26.dp)
                ) {
                    Text(
                        "调整 App 字体大小，重启后生效。",
                        fontSize = scaledSp(29),
                        color = AppColors.TextMuted,
                        lineHeight = 40.sp
                    )

                    var fontLevel by remember {
                        mutableStateOf(AppFontScale.getLevel(context))
                    }
                    var fontSaved by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { level ->
                            val isSelected = level == fontLevel
                            val label = AppFontScale.LEVEL_LABELS[level] ?: ""
                            Button(
                                onClick = {
                                    fontLevel = level
                                    fontSaved = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .height(88.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected)
                                        AppColors.Accent else AppColors.BgSurface,
                                    contentColor = if (isSelected)
                                        AppColors.TextOnAccent else AppColors.TextSecondary
                                )
                            ) {
                                Text(
                                    label,
                                    fontSize = scaledSp(31),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            AppFontScale.setScale(context, fontLevel)
                            fontSaved = true
                        },
                        modifier = Modifier.fillMaxWidth().height(106.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Accent,
                            contentColor = AppColors.TextOnAccent
                        )
                    ) {
                        Text(
                            if (fontSaved) "已保存，重启生效 ✓" else "保存字体设置",
                            fontSize = scaledSp(35), fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ═════════════════════════════════════
            //  数据管理（备份/恢复）
            // ═════════════════════════════════════
            SectionTitle("💾 数据管理")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(31.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.CardDark)
            ) {
                Column(
                    modifier = Modifier.padding(35.dp),
                    verticalArrangement = Arrangement.spacedBy(26.dp)
                ) {
                    Text(
                        "将加油记录备份到手机，换车或重装App时可恢复数据。",
                        fontSize = scaledSp(29),
                        color = AppColors.TextMuted,
                        lineHeight = 40.sp
                    )

                    // 导出按钮
                    Button(
                        onClick = {
                            scope.launch {
                                isExporting = true
                                backupManager.exportAndShare()
                                isExporting = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(106.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Accent.copy(alpha = 0.2f),
                            contentColor = AppColors.Accent
                        ),
                        enabled = !isExporting
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(44.dp),
                                strokeWidth = 5.dp,
                                color = AppColors.Accent
                            )
                        } else {
                            Text("📤 导出备份（分享到手机）", fontSize = scaledSp(33), fontWeight = FontWeight.Medium)
                        }
                    }

                    // 恢复按钮
                    OutlinedButton(
                        onClick = {
                            openDocumentLauncher.launch(BackupManager.MIME_TYPE_JSON)
                        },
                        modifier = Modifier.fillMaxWidth().height(106.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppColors.TextSecondary
                        ),
                        enabled = !isRestoring,
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp, AppColors.Border
                        )
                    ) {
                        if (isRestoring) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(44.dp),
                                strokeWidth = 5.dp,
                                color = AppColors.TextSecondary
                            )
                        } else {
                            Text("📥 从文件恢复备份", fontSize = scaledSp(33))
                        }
                    }

                    // 恢复结果显示
                    restoreResult?.let { result ->
                        when (result) {
                            is ImportResult.Success -> {
                                Text(
                                    "✅ 恢复成功！共导入 ${result.count} 条记录",
                                    fontSize = scaledSp(29),
                                    color = AppColors.Accent,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                            is ImportResult.Error -> {
                                Text(
                                    "❌ ${result.message}",
                                    fontSize = scaledSp(29),
                                    color = AppColors.TextError,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── 恢复确认对话框 ──
    if (showRestoreConfirm && backupInfo != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirm = false
                restoreUri = null
                backupInfo = null
            },
            title = {
                Text("确认恢复备份", fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Text(
                        "即将恢复备份数据，当前所有记录将被替换。",
                        color = AppColors.TextSecondary
                    )
                    Spacer(Modifier.height(9.dp))
                    backupInfo?.let { info ->
                        Text("📄 备份信息", fontWeight = FontWeight.Medium, color = AppColors.TextPrimary)
                        Text("记录数：${info.recordCount} 条", color = AppColors.TextSecondary)
                        Text("导出时间：${info.exportTime}", color = AppColors.TextSecondary)
                        Text("车辆：${info.vehicleInfo}", color = AppColors.TextSecondary)
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(
                        "确定要覆盖当前数据吗？",
                        color = AppColors.TextError,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            restoreUri?.let { uri ->
                                isRestoring = true
                                val result = backupManager.restoreFromUri(uri)
                                restoreResult = result
                                isRestoring = false
                            }
                            showRestoreConfirm = false
                            restoreUri = null
                            backupInfo = null
                        }
                    }
                ) {
                    Text("确认恢复", color = AppColors.TextError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    restoreUri = null
                    backupInfo = null
                }) {
                    Text("取消", color = AppColors.TextMuted)
                }
            },
            containerColor = AppColors.CardDark
        )
    }
}

// ── 辅助组件 ──

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = scaledSp(33),
        fontWeight = FontWeight.SemiBold,
        color = AppColors.TextSecondary,
        modifier = Modifier.padding(horizontal = 9.dp)
    )
}

@Composable
private fun SettingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(label, fontSize = scaledSp(29), color = AppColors.TextMuted)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = { Text(placeholder, fontSize = scaledSp(31), color = AppColors.TextMuted) },
            modifier = Modifier.fillMaxWidth().height(114.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.Accent,
                unfocusedBorderColor = AppColors.Border,
                focusedTextColor = AppColors.TextPrimary,
                unfocusedTextColor = AppColors.TextPrimary,
                focusedContainerColor = AppColors.BgSurface,
                unfocusedContainerColor = AppColors.BgSurface,
                focusedPlaceholderColor = AppColors.TextMuted,
                unfocusedPlaceholderColor = AppColors.TextMuted
            )
        )
    }
}

// ── 车辆信息数据类（保持原有定义）──

data class VehicleInfo(
    val brand: String = "",
    val model: String = "",
    val year: String = ""
) {
    val displayName: String
        get() = when {
            brand.isNotBlank() && model.isNotBlank() -> "$brand · $model"
            brand.isNotBlank() -> brand
            model.isNotBlank() -> model
            else -> "未设置"
        }

    val fullName: String
        get() = when {
            brand.isNotBlank() && model.isNotBlank() && year.isNotBlank() -> "$brand $model ($year)"
            brand.isNotBlank() && model.isNotBlank() -> "$brand $model"
            else -> displayName
        }

    val isSet: Boolean
        get() = brand.isNotBlank() || model.isNotBlank()
}

fun loadVehicleInfo(context: Context): VehicleInfo {
    val prefs = context.getSharedPreferences("vehicle_prefs", Context.MODE_PRIVATE)
    return VehicleInfo(
        brand = prefs.getString("brand", "") ?: "",
        model = prefs.getString("model", "") ?: "",
        year = prefs.getString("year", "") ?: ""
    )
}
