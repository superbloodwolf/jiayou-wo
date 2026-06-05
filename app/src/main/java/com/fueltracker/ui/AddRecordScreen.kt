package com.fueltracker.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.fueltracker.data.*
import com.fueltracker.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * 日期时间的可点击段落组件
 * 点击后弹出对应长度的数字键盘
 */
@Composable
fun RowScope.DateSegment(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) AppColors.Accent.copy(alpha = 0.2f) else Color.Transparent
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(13.dp))
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = scaledSp(40), fontWeight = FontWeight.Medium, color = AppColors.TextPrimary)
    }
}

/**
 * 新建加油记录页面 - 深色车机主题（2.2x 放大版）
 * 核心录入界面，大按钮+数字输入器，避免小键盘输入
 * 支持：双向自动计算、时间选择（补录数据）、里程校验提醒
 * 使用内置数字输入器，屏蔽系统键盘
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordScreen(
    recordToEdit: FuelRecord? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val db = AppDatabase.getInstance(context)
    val repo = remember { FuelRepository(db) }
    val scope = rememberCoroutineScope()

    // ── 表单状态（编辑模式预填充）──
    var stationType by remember { mutableStateOf(recordToEdit?.stationType ?: "中石油") }
    var fuelGrade by remember { mutableStateOf(recordToEdit?.fuelGrade ?: "92号") }
    var fuelPrice by remember { mutableStateOf(recordToEdit?.fuelPrice ?: 0f) }
    var liters by remember { mutableStateOf(recordToEdit?.liters ?: 0f) }
    var totalCost by remember { mutableStateOf(recordToEdit?.totalCost ?: 0f) }
    var odometer by remember { mutableStateOf(recordToEdit?.odometer ?: 0) }
    var notes by remember { mutableStateOf(recordToEdit?.notes ?: "") }
    var saving by remember { mutableStateOf(false) }

    // ── 双向计算模式：防止循环触发 ──
    var calcMode by remember { mutableStateOf("none") }

    // ── 时间选择（分段点击编辑）──
    var recordTimestamp by remember { mutableStateOf(recordToEdit?.timestamp ?: TimeUtils.nowMillis()) }

    /** 从 recordTimestamp 解析各字段 */
    fun getDateFields(): java.time.ZonedDateTime {
        return Instant.ofEpochMilli(recordTimestamp).atZone(TimeUtils.ZONE)
    }

    /** 由年月日时分重新组装时间戳 */
    fun setDateFields(year: Int, month: Int, day: Int, hour: Int, minute: Int) {
        try {
            val dt = java.time.LocalDateTime.of(year, month, day, hour, minute)
            recordTimestamp = dt.atZone(TimeUtils.ZONE).toInstant().toEpochMilli()
        } catch (_: Exception) {
            Toast.makeText(context, "日期时间无效，请重新输入", Toast.LENGTH_SHORT).show()
        }
    }

    // ── 里程校验警告 ──
    var showOdometerWarning by remember { mutableStateOf(false) }
    var odometerWarningMessage by remember { mutableStateOf("") }
    var odometerWarningTitle by remember { mutableStateOf("") }
    var pendingSaveRecord by remember { mutableStateOf<FuelRecord?>(null) }

    // ── 漏记提醒（里程跳跃值检查）──
    var showMissedWarning by remember { mutableStateOf(false) }
    var missedDistance by remember { mutableStateOf(0) }
    var missedEstimateLiters by remember { mutableStateOf("") }

    // ── 里程跳跃阈值（从设置读取，默认650km）──
    val prefs = remember { context.getSharedPreferences("vehicle_prefs", Context.MODE_PRIVATE) }
    val missedThreshold = remember { prefs.getInt("missed_record_threshold", 650) }

    // ── 数字输入器状态 ──
    var activeInput by remember { mutableStateOf<String?>(null) }
    // activeInput 取值：
    //   "fuelPrice" | "liters" | "totalCost" | "odometer"
    //   "dateYear" | "dateMonth" | "dateDay" | "dateHour" | "dateMinute"

    // ── 读取上次记录 ──
    var lastRecord by remember { mutableStateOf<FuelRecord?>(null) }

    LaunchedEffect(Unit) {
        lastRecord = repo.getLastRecord()
        lastRecord?.fuelPrice?.let { fuelPrice = it }
        lastRecord?.odometer?.let { odometer = it }
    }

    // ── 双向自动计算 ──
    LaunchedEffect(liters, fuelPrice) {
        if (calcMode == "liters" && fuelPrice > 0f && liters > 0f) {
            totalCost = fuelPrice * liters
        }
    }
    LaunchedEffect(totalCost, fuelPrice) {
        if (calcMode == "cost" && fuelPrice > 0f && totalCost > 0f) {
            liters = totalCost / fuelPrice
        }
    }

    val distanceSinceLast = remember(odometer, lastRecord) {
        val last = lastRecord?.odometer
        if (odometer > 0 && last != null && odometer > last) (odometer - last) else null
    }

    val consumption = remember(liters, distanceSinceLast) {
        if (liters > 0f && distanceSinceLast != null && distanceSinceLast > 0)
            (liters / distanceSinceLast) * 100f
        else null
    }

    // ── 里程校验函数 ──
    fun validateOdometer(): String? {
        val last = lastRecord ?: return null
        // 检查1：总行驶里程应大于上次里程
        if (odometer <= last.odometer) {
            return "总行驶里程（${odometer}km）应大于上次里程（${last.odometer}km），请检查输入。"
        }
        // 检查2：本次加油里程不应超过跳跃阈值
        val trip = odometer - last.odometer
        if (trip > missedThreshold) {
            return "本次加油里程（${trip}km）超过跳跃阈值（${missedThreshold}km），可能漏记加油记录，请检查。"
        }
        return null
    }

    // ── 数字输入器回调 ──
    fun onNumberPadConfirm(value: String) {
        when (activeInput) {
            "fuelPrice" -> {
                val num = value.filter { c -> c.isDigit() || c == '.' }.toFloatOrNull() ?: 0f
                fuelPrice = num.coerceIn(0f, 20f)
                if (calcMode == "liters" && fuelPrice > 0f && liters > 0f) totalCost = fuelPrice * liters
            }
            "liters" -> {
                val num = value.filter { c -> c.isDigit() || c == '.' }.toFloatOrNull() ?: 0f
                liters = num.coerceIn(0f, 200f)
                calcMode = "liters"
                if (fuelPrice > 0f && liters > 0f) totalCost = fuelPrice * liters
            }
            "totalCost" -> {
                val num = value.filter { c -> c.isDigit() || c == '.' }.toFloatOrNull() ?: 0f
                totalCost = num.coerceIn(0f, 9999f)
                calcMode = "cost"
                if (fuelPrice > 0f && totalCost > 0f) liters = totalCost / fuelPrice
            }
            "odometer" -> {
                val num = value.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                odometer = num.coerceIn(0, 999999)
            }
            // ── 日期时间分段编辑 ──
            "dateYear" -> {
                val num = value.filter { it.isDigit() }.toIntOrNull()
                if (num != null && num in 1900..2100) {
                    val z = getDateFields()
                    setDateFields(num, z.monthValue, z.dayOfMonth, z.hour, z.minute)
                } else {
                    Toast.makeText(context, "年份请输入1900-2100", Toast.LENGTH_SHORT).show()
                }
            }
            "dateMonth" -> {
                val num = value.filter { it.isDigit() }.toIntOrNull()
                if (num != null && num in 1..12) {
                    val z = getDateFields()
                    setDateFields(z.year, num, z.dayOfMonth, z.hour, z.minute)
                } else {
                    Toast.makeText(context, "月份请输入1-12", Toast.LENGTH_SHORT).show()
                }
            }
            "dateDay" -> {
                val num = value.filter { it.isDigit() }.toIntOrNull()
                if (num != null && num in 1..31) {
                    val z = getDateFields()
                    setDateFields(z.year, z.monthValue, num, z.hour, z.minute)
                } else {
                    Toast.makeText(context, "日期请输入1-31", Toast.LENGTH_SHORT).show()
                }
            }
            "dateHour" -> {
                val num = value.filter { it.isDigit() }.toIntOrNull()
                if (num != null && num in 0..23) {
                    val z = getDateFields()
                    setDateFields(z.year, z.monthValue, z.dayOfMonth, num, z.minute)
                } else {
                    Toast.makeText(context, "小时请输入0-23", Toast.LENGTH_SHORT).show()
                }
            }
            "dateMinute" -> {
                val num = value.filter { it.isDigit() }.toIntOrNull()
                if (num != null && num in 0..59) {
                    val z = getDateFields()
                    setDateFields(z.year, z.monthValue, z.dayOfMonth, z.hour, num)
                } else {
                    Toast.makeText(context, "分钟请输入0-59", Toast.LENGTH_SHORT).show()
                }
            }
        }
        activeInput = null
    }

    Scaffold(
        containerColor = AppColors.BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (recordToEdit != null) "修改记录" else "新建加油记录",
                        fontSize = scaledSp(44), fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
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
                .padding(horizontal = 26.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ── 加油时间（分段点击修改）──
            CompactLabel("加油时间（点击对应位置修改）")
            val zdt = getDateFields()
            Row(
                modifier = Modifier.fillMaxWidth().height(106.dp)
                    .background(AppColors.BgSurface, RoundedCornerShape(26.dp))
                    .padding(horizontal = 22.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateSegment(zdt.year.toString(), activeInput == "dateYear") { activeInput = "dateYear" }
                Text("-", fontSize = scaledSp(35), color = AppColors.TextMuted)
                DateSegment("%02d".format(zdt.monthValue), activeInput == "dateMonth") { activeInput = "dateMonth" }
                Text("-", fontSize = scaledSp(35), color = AppColors.TextMuted)
                DateSegment("%02d".format(zdt.dayOfMonth), activeInput == "dateDay") { activeInput = "dateDay" }
                Spacer(Modifier.width(26.dp))
                DateSegment("%02d".format(zdt.hour), activeInput == "dateHour") { activeInput = "dateHour" }
                Text(":", fontSize = scaledSp(35), color = AppColors.TextMuted)
                DateSegment("%02d".format(zdt.minute), activeInput == "dateMinute") { activeInput = "dateMinute" }
            }

            // ── 加油站类型 & 油品（合并一行）──
            Row(horizontalArrangement = Arrangement.spacedBy(35.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    CompactLabel("加油站")
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        listOf("中石油", "中石化", "其他").forEach { type ->
                            FilterChip(
                                selected = stationType == type,
                                onClick = { stationType = type },
                                label = { Text(type, fontSize = scaledSp(29)) },
                                modifier = Modifier.height(97.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AppColors.Accent.copy(alpha = 0.2f),
                                    selectedLabelColor = AppColors.Accent,
                                    containerColor = AppColors.CardDark,
                                    labelColor = AppColors.TextSecondary
                                )
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    CompactLabel("油品")
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        listOf("92号", "95号", "98号", "柴油").forEach { grade ->
                            FilterChip(
                                selected = fuelGrade == grade,
                                onClick = { fuelGrade = grade },
                                label = { Text(grade, fontSize = scaledSp(29)) },
                                modifier = Modifier.height(97.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AppColors.Accent.copy(alpha = 0.2f),
                                    selectedLabelColor = AppColors.Accent,
                                    containerColor = AppColors.CardDark,
                                    labelColor = AppColors.TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            // ── 油价 & 加油量（点击弹出数字输入器）──
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                // 油价
                Column(modifier = Modifier.weight(1f)) {
                    CompactLabel("油价(元/升)")
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = AppColors.BgSurface,
                        border = BorderStroke(1.dp, AppColors.Border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(81.dp)
                            .clickable { activeInput = "fuelPrice" }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (fuelPrice > 0f) "%.2f".format(fuelPrice) else "点击输入",
                                fontSize = scaledSp(33),
                                color = if (fuelPrice > 0f) AppColors.TextPrimary else AppColors.TextMuted
                            )
                            Text("元/L", fontSize = scaledSp(29), color = AppColors.TextSecondary)
                        }
                    }
                }
                // 加油量
                Column(modifier = Modifier.weight(1f)) {
                    CompactLabel("加油量(升)")
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = AppColors.BgSurface,
                        border = BorderStroke(1.dp, AppColors.Border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(81.dp)
                            .clickable { activeInput = "liters" }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (liters > 0f) "%.1f".format(liters) else "点击输入",
                                fontSize = scaledSp(33),
                                color = if (liters > 0f) AppColors.TextPrimary else AppColors.TextMuted
                            )
                            Text("L", fontSize = scaledSp(29), color = AppColors.TextSecondary)
                        }
                    }
                }
            }

            // ── 金额（点击弹出数字输入器）──
            CompactLabel("实付金额(元)")
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = AppColors.BgSurface,
                border = BorderStroke(1.dp, AppColors.Border),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(81.dp)
                    .clickable { activeInput = "totalCost" }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (totalCost > 0f) "%.2f".format(totalCost) else "点击输入",
                        fontSize = scaledSp(33),
                        color = if (totalCost > 0f) AppColors.TextPrimary else AppColors.TextMuted
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("元", fontSize = scaledSp(29), color = AppColors.TextSecondary)
                        Spacer(Modifier.width(18.dp))
                        TextButton(onClick = {
                            calcMode = "liters"
                            totalCost = fuelPrice * liters
                        }) {
                            Text("重算", fontSize = scaledSp(29), color = AppColors.Accent)
                        }
                    }
                }
            }

            // ── 总行驶里程（点击弹出数字输入器）──
            CompactLabel("总行驶里程(km)")
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = AppColors.BgSurface,
                border = BorderStroke(1.dp, AppColors.Border),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(81.dp)
                    .clickable { activeInput = "odometer" }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (odometer > 0) odometer.toString() else "点击输入",
                        fontSize = scaledSp(33),
                        color = if (odometer > 0) AppColors.TextPrimary else AppColors.TextMuted
                    )
                    Text("km", fontSize = scaledSp(29), color = AppColors.TextSecondary)
                }
            }

            // ── 自动计算结果 ──
            if (distanceSinceLast != null || consumption != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.CardDark)
                ) {
                    Column(Modifier.padding(horizontal = 31.dp, vertical = 22.dp)) {
                        distanceSinceLast?.let {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("加油里程", fontSize = scaledSp(29), color = AppColors.TextMuted)
                                Text("$it km", fontSize = scaledSp(33), fontWeight = FontWeight.Medium,
                                    color = AppColors.TextPrimary)
                            }
                        }
                        consumption?.let {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("本次油耗", fontSize = scaledSp(29), color = AppColors.TextMuted)
                                Text("%.1f L/100km".format(it), fontSize = scaledSp(33),
                                    fontWeight = FontWeight.Bold, color = AppColors.Accent)
                            }
                        }
                    }
                }
            }

            // ── 备注 ──
            CompactLabel("备注(可选)")
            CompactTextField(
                value = notes,
                onValueChange = { if (it.length <= 50) notes = it },
                placeholder = "如：长途回家..."
            )

            // ── 保存按钮 ──
            Button(
                onClick = {
                    // 里程校验
                    val validationError = validateOdometer()
                    if (validationError != null) {
                        odometerWarningMessage = validationError
                        odometerWarningTitle = "⚠ 里程数据异常"
                        pendingSaveRecord = FuelRecord(
                            id = recordToEdit?.id ?: 0,
                            stationType = stationType,
                            stationName = "",
                            fuelGrade = fuelGrade,
                            fuelPrice = fuelPrice,
                            liters = liters,
                            totalCost = totalCost,
                            odometer = odometer,
                            timestamp = recordTimestamp,
                            notes = notes
                        )
                        showOdometerWarning = true
                    } else {
                        // 无校验错误，检查里程跳跃值（漏记提醒）
                        val jump = distanceSinceLast ?: 0
                        if (lastRecord != null && jump > missedThreshold) {
                            missedDistance = jump
                            pendingSaveRecord = FuelRecord(
                                id = recordToEdit?.id ?: 0,
                                stationType = stationType,
                                stationName = "",
                                fuelGrade = fuelGrade,
                                fuelPrice = fuelPrice,
                                liters = liters,
                                totalCost = totalCost,
                                odometer = odometer,
                                timestamp = recordTimestamp,
                                notes = notes
                        )
                            showMissedWarning = true
                        } else {
                            saving = true
                            scope.launch {
                                val record = FuelRecord(
                                    id = recordToEdit?.id ?: 0,
                                    stationType = stationType,
                                    stationName = "",
                                    fuelGrade = fuelGrade,
                                    fuelPrice = fuelPrice,
                                    liters = liters,
                                    totalCost = totalCost,
                                    odometer = odometer,
                                    timestamp = recordTimestamp,
                                    notes = notes
                                )
                                if (recordToEdit != null) {
                                    repo.updateRecord(record)
                                } else {
                                    repo.addRecord(record)
                                }
                                saving = false
                                onSaved()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(123.dp),
                shape = RoundedCornerShape(31.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Accent,
                    contentColor = AppColors.TextOnAccent,
                    disabledContainerColor = AppColors.Accent.copy(alpha = 0.3f),
                    disabledContentColor = AppColors.TextMuted
                ),
                enabled = !saving && fuelPrice > 0f && liters > 0f && odometer > 0
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(53.dp),
                        strokeWidth = 4.dp,
                        color = AppColors.TextOnAccent
                    )
                } else {
                    Text(
                        if (recordToEdit != null) "保存修改" else "保存记录",
                        fontSize = scaledSp(37), fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // ── 里程校验警告对话框（再次确认流程）──
    if (showOdometerWarning) {
        AlertDialog(
            onDismissRequest = {
                showOdometerWarning = false
                odometerWarningMessage = ""
                pendingSaveRecord = null
            },
            title = {
                Text(odometerWarningTitle, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(
                        odometerWarningMessage,
                        color = AppColors.TextSecondary,
                        fontSize = scaledSp(29),
                        lineHeight = 40.sp
                    )
                    Text(
                        "如确认输入无误，可点击「确认保存」强制保存。",
                        fontSize = scaledSp(26),
                        color = AppColors.TextMuted
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            pendingSaveRecord?.let {
                                if (recordToEdit != null) {
                                    repo.updateRecord(it)
                                } else {
                                    repo.addRecord(it)
                                }
                            }
                            showOdometerWarning = false
                            odometerWarningMessage = ""
                            pendingSaveRecord = null
                            saving = false
                            onSaved()
                        }
                    }
                ) {
                    Text("确认保存", color = AppColors.Accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showOdometerWarning = false
                        odometerWarningMessage = ""
                        pendingSaveRecord = null
                    }
                ) {
                    Text("重新输入", color = AppColors.TextMuted)
                }
            },
            containerColor = AppColors.CardDark
        )
    }

    // ── 漏记提醒对话框 ──
    if (showMissedWarning) {
        AlertDialog(
            onDismissRequest = {
                showMissedWarning = false
                pendingSaveRecord = null
                missedEstimateLiters = ""
            },
            title = { Text("⚠ 可能漏记加油", fontWeight = FontWeight.Bold, color = AppColors.TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(
                        "本次加油里程 ${missedDistance}km，超过设定阈值 ${missedThreshold}km，可能存在漏记的加油记录。",
                        color = AppColors.TextSecondary,
                        fontSize = scaledSp(29),
                        lineHeight = 40.sp
                    )
                    Text(
                        "如果确实漏记了，可以估算补录一次加油量，让油耗数据更准确。",
                        fontSize = scaledSp(26),
                        color = AppColors.TextMuted
                    )
                    Spacer(Modifier.height(9.dp))
                    CompactLabel("估算漏记的加油量（升，可选）")
                    CompactTextField(
                        value = missedEstimateLiters,
                        onValueChange = { missedEstimateLiters = it },
                        placeholder = "如：35（留空则直接保存）",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        pendingSaveRecord?.let { repo.addRecord(it) }
                        val estimate = missedEstimateLiters.filter { c -> c.isDigit() || c == '.' }
                            .toFloatOrNull()
                        if (estimate != null && estimate > 0f) {
                            val midOdometer = ((lastRecord?.odometer ?: 0) + odometer) / 2
                            val midTimestamp = recordTimestamp - (24 * 60 * 60 * 1000L)
                            val missedRecord = FuelRecord(
                                stationType = stationType,
                                stationName = "（补录）",
                                fuelGrade = fuelGrade,
                                fuelPrice = fuelPrice,
                                liters = estimate,
                                totalCost = estimate * fuelPrice,
                                odometer = midOdometer,
                                timestamp = midTimestamp,
                                notes = "系统补录：漏记加油约 ${"%.1f".format(estimate)}L"
                            )
                            repo.addRecord(missedRecord)
                        }
                        showMissedWarning = false
                        missedEstimateLiters = ""
                        pendingSaveRecord = null
                        saving = false
                        onSaved()
                    }
                }) {
                    Text("保存", color = AppColors.Accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch {
                        pendingSaveRecord?.let { repo.addRecord(it) }
                        showMissedWarning = false
                        missedEstimateLiters = ""
                        pendingSaveRecord = null
                        saving = false
                        onSaved()
                    }
                }) {
                    Text("跳过", color = AppColors.TextMuted)
                }
            },
            containerColor = AppColors.CardDark
        )
    }

    // ── 数字输入器弹窗 ──
    if (activeInput != null) {
        // 日期分段字段的当前值
        val zdt = if (activeInput in listOf("dateYear","dateMonth","dateDay","dateHour","dateMinute"))
            getDateFields() else null

        val initVal = when (activeInput) {
            "fuelPrice" -> if (fuelPrice > 0f) "%.2f".format(fuelPrice) else ""
            "liters"   -> if (liters > 0f) "%.1f".format(liters) else ""
            "totalCost" -> if (totalCost > 0f) "%.2f".format(totalCost) else ""
            "odometer"  -> if (odometer > 0) odometer.toString() else ""
            "dateYear"  -> zdt?.year?.toString() ?: ""
            "dateMonth" -> "%02d".format(zdt?.monthValue ?: 1)
            "dateDay"   -> "%02d".format(zdt?.dayOfMonth ?: 1)
            "dateHour"  -> "%02d".format(zdt?.hour ?: 0)
            "dateMinute"-> "%02d".format(zdt?.minute ?: 0)
            else         -> ""
        }
        val suffixVal = when (activeInput) {
            "fuelPrice" -> "元/L"
            "liters"   -> "L"
            "totalCost" -> "元"
            "odometer" -> "km"
            "dateYear"  -> "年"
            "dateMonth" -> "月"
            "dateDay"   -> "日"
            "dateHour"  -> "时"
            "dateMinute"-> "分"
            else         -> ""
        }
        val isDec = activeInput in listOf("fuelPrice", "liters", "totalCost")
        val maxLen = when (activeInput) {
            "dateYear"   -> 4
            "dateMonth", "dateDay", "dateHour", "dateMinute" -> 2
            else         -> 8
        }

        NumberPadDialog(
            initialValue = initVal,
            suffix = suffixVal,
            isDecimal = isDec,
            maxLength = maxLen,
            onValueChange = { onNumberPadConfirm(it) },
            onDismiss = { activeInput = null }
        )
    }
}

// ── 辅助组件（2.2x 放大版）──

/** 小标签 */
@Composable
fun CompactLabel(text: String) {
    Text(
        text = text,
        fontSize = scaledSp(26),
        color = AppColors.TextMuted,
        modifier = Modifier.padding(vertical = 5.dp)
    )
}

/** 紧凑文本输入框（备注/估算输入用） */
@Composable
fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, fontSize = scaledSp(31), color = AppColors.TextMuted)
        },
        textStyle = LocalTextStyle.current.copy(fontSize = scaledSp(33), color = AppColors.TextPrimary),
        singleLine = true,
        shape = RoundedCornerShape(22.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.Accent,
            unfocusedBorderColor = AppColors.Border,
            focusedTextColor = AppColors.TextPrimary,
            unfocusedTextColor = AppColors.TextPrimary,
            focusedContainerColor = AppColors.BgSurface,
            unfocusedContainerColor = AppColors.BgSurface,
            cursorColor = AppColors.Accent
        ),
        modifier = modifier.fillMaxWidth().heightIn(min = 81.dp)
    )
}
