package com.fueltracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.fueltracker.data.*
import com.fueltracker.util.TimeUtils
import kotlinx.coroutines.launch

/**
 * 查询页面 - 深色车机主题（2.2x 放大版）
 * 支持按时段查询加油记录、油价变化趋势
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueryScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = AppDatabase.getInstance(context)
    val repo = remember { FuelRepository(db) }
    val scope = rememberCoroutineScope()

    // ── 查询条件 ──
    var queryType by remember { mutableStateOf("record") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var records by remember { mutableStateOf<List<RecordWithStats>>(emptyList()) }
    var priceTrend by remember { mutableStateOf<List<PricePoint>>(emptyList()) }
    var queryDone by remember { mutableStateOf(false) }

    val quickRanges = listOf(
        "本月" to { getQuickRange(0) },
        "上月" to { getQuickRange(-1) },
        "近3月" to { getQuickRange(-3, true) },
        "近半年" to { getQuickRange(-6, true) },
        "今年" to { getYearRange() }
    )

    Scaffold(
        containerColor = AppColors.BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Text("查询统计", fontSize = scaledSp(44), fontWeight = FontWeight.Bold,
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
                .padding(horizontal = 26.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // ── 查询类型切换 ──
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                FilterChip(
                    selected = queryType == "record",
                    onClick = { queryType = "record"; queryDone = false },
                    label = { Text("加油记录", fontSize = scaledSp(33)) },
                    modifier = Modifier.height(97.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.Accent.copy(alpha = 0.2f),
                        selectedLabelColor = AppColors.Accent,
                        containerColor = AppColors.CardDark,
                        labelColor = AppColors.TextSecondary
                    )
                )
                FilterChip(
                    selected = queryType == "price",
                    onClick = { queryType = "price"; queryDone = false },
                    label = { Text("油价变化", fontSize = scaledSp(33)) },
                    modifier = Modifier.height(97.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.Accent.copy(alpha = 0.2f),
                        selectedLabelColor = AppColors.Accent,
                        containerColor = AppColors.CardDark,
                        labelColor = AppColors.TextSecondary
                    )
                )
            }

            // ── 快捷时段 ──
            CompactLabel("快捷时段")
            Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                quickRanges.forEach { (label, rangeFn) ->
                    AssistChip(
                        onClick = {
                            val (s, e) = rangeFn()
                            startDate = s
                            endDate = e
                        },
                        label = { Text(label, fontSize = scaledSp(31)) },
                        modifier = Modifier.height(88.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = AppColors.CardDark,
                            labelColor = AppColors.TextSecondary
                        )
                    )
                }
            }

            // ── 自定义日期范围 ──
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                CompactInputColumn(
                    modifier = Modifier.weight(1f),
                    label = "开始日期",
                    value = startDate,
                    onValueChange = { startDate = it },
                    placeholder = "2026-01-01"
                )
                CompactInputColumn(
                    modifier = Modifier.weight(1f),
                    label = "结束日期",
                    value = endDate,
                    onValueChange = { endDate = it },
                    placeholder = "2026-12-31"
                )
            }

            // ── 查询按钮 ──
            Button(
                onClick = {
                    scope.launch {
                        val startTs = parseDateToMillis(startDate)
                        val endTs = parseDateToMillis(endDate, true)
                        if (startTs != null && endTs != null) {
                            if (queryType == "record") {
                                records = repo.getRecordsInRange(startTs, endTs)
                            } else {
                                priceTrend = repo.getPriceTrend(startTs, endTs)
                            }
                            queryDone = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(123.dp),
                shape = RoundedCornerShape(31.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Accent,
                    contentColor = AppColors.TextOnAccent,
                    disabledContainerColor = AppColors.Accent.copy(alpha = 0.25f),
                    disabledContentColor = AppColors.TextMuted
                ),
                enabled = startDate.isNotBlank() && endDate.isNotBlank()
            ) {
                Text("开始查询", fontSize = scaledSp(37), fontWeight = FontWeight.Bold)
            }

            Divider(modifier = Modifier.padding(vertical = 9.dp), color = AppColors.Divider)

            // ── 查询结果：加油记录 ──
            if (queryDone && queryType == "record") {
                CompactLabel("查询结果：共 ${records.size} 条")
                if (records.isEmpty()) {
                    Text("该时段无加油记录", color = AppColors.TextMuted,
                        modifier = Modifier.padding(vertical = 35.dp))
                } else {
                    val totalCost = records.sumOf { it.record.totalCost.toDouble() }
                    val totalLiters = records.sumOf { it.record.liters.toDouble() }
                    val totalDistance = records.mapNotNull { it.distanceKm }.sum()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.CardAccent)
                    ) {
                        Column(Modifier.padding(26.dp)) {
                            Text("时段汇总", fontWeight = FontWeight.Bold, fontSize = scaledSp(31),
                                color = AppColors.Accent)
                            Spacer(Modifier.height(18.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                StatItemSmall("总次数", "${records.size}次", AppColors.TextPrimary)
                                StatItemSmall("总花费", "¥%.0f".format(totalCost), AppColors.TextPrimary)
                                StatItemSmall("总升数", "%.1fL".format(totalLiters), AppColors.TextPrimary)
                                StatItemSmall("总行程", "${totalDistance}km", AppColors.TextPrimary)
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    records.forEach { item ->
                        RecordCard(item)
                    }
                }
            }

            // ── 查询结果：油价变化 ──
            if (queryDone && queryType == "price") {
                CompactLabel("查询结果：油价变化趋势")
                if (priceTrend.isEmpty()) {
                    Text("该时段无油价记录", color = AppColors.TextMuted,
                        modifier = Modifier.padding(vertical = 35.dp))
                } else {
                    val avgPrice = priceTrend.map { it.price }.average()
                    val minPrice = priceTrend.minOf { it.price }
                    val maxPrice = priceTrend.maxOf { it.price }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.CardWarm)
                    ) {
                        Column(Modifier.padding(26.dp)) {
                            Text("油价统计", fontWeight = FontWeight.Bold, fontSize = scaledSp(31),
                                color = AppColors.Accent)
                            Spacer(Modifier.height(18.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                StatItemSmall("平均", "¥%.2f".format(avgPrice), AppColors.TextPrimary)
                                StatItemSmall("最低", "¥%.2f".format(minPrice), AppColors.TextPrimary)
                                StatItemSmall("最高", "¥%.2f".format(maxPrice), AppColors.TextPrimary)
                                StatItemSmall("记录数", "${priceTrend.size}次", AppColors.TextPrimary)
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    priceTrend.forEach { point ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 5.dp, vertical = 7.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = AppColors.CardDark)
                        ) {
                            Row(
                                modifier = Modifier.padding(26.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${point.station} · ${point.grade}",
                                        fontSize = scaledSp(31), fontWeight = FontWeight.Medium,
                                        color = AppColors.TextPrimary)
                                    Text(point.date, fontSize = scaledSp(26), color = AppColors.TextMuted)
                                }
                                Text("¥%.2f".format(point.price), fontSize = scaledSp(35),
                                    fontWeight = FontWeight.Bold, color = AppColors.Accent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactInputColumn(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String = "",
    placeholder: String = ""
) {
    Column(modifier = modifier) {
        CompactLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = if (placeholder.isNotEmpty()) {
                { Text(placeholder, fontSize = scaledSp(29), color = AppColors.TextMuted) }
            } else null,
            suffix = if (suffix.isNotEmpty()) {
                { Text(suffix, fontSize = scaledSp(26), color = AppColors.TextMuted) }
            } else null,
            modifier = Modifier.fillMaxWidth().height(106.dp),
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

@Composable
fun StatItemSmall(label: String, value: String, valueColor: Color = AppColors.TextPrimary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = scaledSp(31), color = valueColor)
        Text(label, fontSize = scaledSp(24), color = AppColors.TextMuted)
    }
}

// ── 数据模型 ──

data class PricePoint(
    val date: String,
    val station: String,
    val grade: String,
    val price: Float
)

// ── 工具函数 ──

fun getQuickRange(monthOffset: Int, isDuration: Boolean = false): Pair<String, String> {
    val now = java.time.LocalDate.now()
    return if (isDuration) {
        val start = now.plusMonths(monthOffset.toLong()).withDayOfMonth(1)
        val end = now
        start.toString() to end.toString()
    } else {
        val target = now.plusMonths(monthOffset.toLong())
        val start = target.withDayOfMonth(1)
        val end = target.plusMonths(1).withDayOfMonth(1).minusDays(1)
        start.toString() to end.toString()
    }
}

fun getYearRange(): Pair<String, String> {
    val now = java.time.LocalDate.now()
    val start = now.withMonth(1).withDayOfMonth(1)
    val end = now.withMonth(12).withDayOfMonth(31)
    return start.toString() to end.toString()
}

fun parseDateToMillis(dateStr: String, endOfDay: Boolean = false): Long? {
    return try {
        val date = java.time.LocalDate.parse(dateStr)
        val dt = if (endOfDay) date.atTime(23, 59, 59) else date.atStartOfDay()
        dt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (e: Exception) {
        null
    }
}
