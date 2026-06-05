package com.fueltracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.window.Dialog
import com.fueltracker.data.*
import com.fueltracker.util.TimeUtils
import kotlinx.coroutines.launch

/**
 * 主界面 - 加油记录列表 + 本月统计（2.2x 放大版）
 * 深色车机主题，护眼安全
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    realMileage: Int? = null,
    onAddClick: () -> Unit,
    onQueryClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onEditRecord: (FuelRecord) -> Unit = {},
    onExitClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val db = AppDatabase.getInstance(context)
    val repo = remember { FuelRepository(db) }
    val scope = rememberCoroutineScope()

    var records by remember { mutableStateOf<List<RecordWithStats>>(emptyList()) }
    var stats by remember { mutableStateOf(MonthStatsData(0, 0f, 0f, 0f)) }
    var vehicleInfo by remember { mutableStateOf(loadVehicleInfo(context)) }

    val recordToDelete = remember { mutableStateOf<FuelRecord?>(null) }

    fun refreshData() {
        scope.launch {
            records = repo.getRecordsWithStats()
            stats = repo.getCurrentMonthStats()
            vehicleInfo = loadVehicleInfo(context)
        }
    }

    LaunchedEffect(Unit) { refreshData() }

    Scaffold(
        containerColor = AppColors.BgDark,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("加油记录", fontSize = scaledSp(44), fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary)
                },
                navigationIcon = {
                    IconButton(
                        onClick = onExitClick,
                        modifier = Modifier.size(106.dp)
                    ) {
                        Text("✕", fontSize = scaledSp(48), color = AppColors.TextSecondary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(106.dp)
                    ) {
                        Text("⚙", fontSize = scaledSp(44), color = AppColors.TextSecondary)
                    }
                    IconButton(
                        onClick = onQueryClick,
                        modifier = Modifier.size(106.dp)
                    ) {
                        Text("🔍", fontSize = scaledSp(44), color = AppColors.TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppColors.BgDark,
                    titleContentColor = AppColors.TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = AppColors.Accent,
                contentColor = AppColors.TextOnAccent,
                modifier = Modifier.size(141.dp)
            ) {
                Text("＋", fontSize = scaledSp(62), fontWeight = FontWeight.Bold)
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── 车辆信息卡片 ──
            if (vehicleInfo.isSet) {
                VehicleInfoCard(vehicleInfo, realMileage = realMileage)
            }

            // ── 本月统计卡片 ──
            StatsCard(stats)

            Spacer(Modifier.height(18.dp))

            // ── 记录列表标题 ──
            Text(
                "加油历史",
                modifier = Modifier.padding(horizontal = 35.dp),
                fontSize = scaledSp(33),
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextSecondary
            )

            if (records.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 106.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📭", fontSize = scaledSp(106))
                    Spacer(Modifier.height(18.dp))
                    Text("暂无加油记录", color = AppColors.TextMuted)
                    Text("点击 ＋ 添加第一条记录", color = AppColors.TextMuted, fontSize = scaledSp(26))
                }
            } else {
                records.forEach { item ->
                    RecordCard(
                        item = item,
                        onEdit = { onEditRecord(item.record) },
                        onDelete = { recordToDelete.value = item.record }
                    )
                }
            }

            Spacer(Modifier.height(176.dp)) // FAB 遮挡避让（放大后更高）
        }
    }

    // ── 删除确认对话框（1.5x 放大版）──
    recordToDelete.value?.let { record ->
        Dialog(onDismissRequest = { recordToDelete.value = null }) {
            Surface(
                shape = RoundedCornerShape(31.dp),
                color = AppColors.CardDark,
                modifier = Modifier
                    .width(500.dp)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(44.dp),
                    verticalArrangement = Arrangement.spacedBy(26.dp)
                ) {
                    Text(
                        "确认删除",
                        fontSize = scaledSp(33),
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        "确定删除这条加油记录？\n${TimeUtils.formatDate(record.timestamp)}\n¥%.0f / %.1fL".format(record.totalCost, record.liters),
                        fontSize = scaledSp(26),
                        color = AppColors.TextSecondary,
                        lineHeight = scaledSp(37).value.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { recordToDelete.value = null },
                            modifier = Modifier.heightIn(min = 70.dp)
                        ) {
                            Text(
                                "取消",
                                fontSize = scaledSp(26),
                                color = AppColors.TextSecondary
                            )
                        }
                        TextButton(
                            onClick = {
                                scope.launch {
                                    repo.deleteRecord(record)
                                    recordToDelete.value = null
                                    refreshData()
                                }
                            },
                            modifier = Modifier.heightIn(min = 70.dp)
                        ) {
                            Text(
                                "删除",
                                fontSize = scaledSp(26),
                                color = AppColors.TextError,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 车辆信息卡片 - 深色暖色（2.2x 放大版） */
@Composable
fun VehicleInfoCard(info: VehicleInfo, realMileage: Int? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp, vertical = 13.dp),
        shape = RoundedCornerShape(31.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.CardWarm
        )
    ) {
        Row(
            modifier = Modifier.padding(31.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(31.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = AppColors.Accent.copy(alpha = 0.15f),
                modifier = Modifier.size(106.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🚗", fontSize = scaledSp(53))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    info.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = scaledSp(35),
                    color = AppColors.TextPrimary
                )
                if (info.year.isNotBlank()) {
                    Text(
                        "${info.year}款",
                        fontSize = scaledSp(26),
                        color = AppColors.TextSecondary
                    )
                }
                realMileage?.let { mileage ->
                    Text(
                        "里程：${mileage}km",
                        fontSize = scaledSp(26),
                        color = AppColors.Accent
                    )
                }
            }
        }
    }
}

/** 本月统计卡片 - 琥珀色强调（2.2x 放大版） */
@Composable
fun StatsCard(stats: MonthStatsData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(26.dp),
        shape = RoundedCornerShape(35.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.CardAccent
        )
    ) {
        Column(Modifier.padding(35.dp)) {
            Text("本月统计", fontWeight = FontWeight.Bold, fontSize = scaledSp(33),
                color = Color.White)
            Spacer(Modifier.height(22.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem("加油次数", "${stats.count} 次", Color.White)
                StatItem("总花费", "¥%.0f".format(stats.totalCost), Color.White)
                StatItem("平均油耗", "%.1fL/百km".format(stats.avgLiters), Color.White)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, valueColor: Color = AppColors.TextPrimary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = scaledSp(40), color = valueColor)
        Text(label, fontSize = scaledSp(24), color = AppColors.TextMuted)
    }
}

/** 单条加油记录卡片（2.2x 放大版） */
@Composable
fun RecordCard(
    item: RecordWithStats,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val r = item.record
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp, vertical = 9.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.CardDark
        )
    ) {
        Column(Modifier.padding(horizontal = 31.dp, vertical = 22.dp)) {
            // ── 第一行：日期 + 加油站·油品 + 金额 ──
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        TimeUtils.formatDate(r.timestamp),
                        fontSize = scaledSp(26), color = AppColors.TextMuted,
                        modifier = Modifier.width(194.dp)
                    )
                    Text(
                        "${r.stationType}·${r.fuelGrade}",
                        fontWeight = FontWeight.Medium, fontSize = scaledSp(31),
                        color = AppColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    "¥%.0f".format(r.totalCost),
                    fontWeight = FontWeight.Bold, fontSize = scaledSp(35),
                    color = AppColors.Accent
                )
            }

            // ── 第二行：总行驶里程 | 加油里程 | 加油量 | 油耗 | 每km + 修改/删除 ──
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("总行驶里程", fontSize = scaledSp(22), color = AppColors.TextMuted)
                    Text("${r.odometer}km", fontSize = scaledSp(29), color = AppColors.TextSecondary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("加油里程", fontSize = scaledSp(22), color = AppColors.TextMuted)
                    Text(
                        item.distanceKm?.let { "${it}km" } ?: "—",
                        fontSize = scaledSp(29),
                        color = if (item.distanceKm != null) AppColors.Accent else AppColors.TextMuted,
                        fontWeight = if (item.distanceKm != null) FontWeight.Medium else FontWeight.Normal
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("加油量", fontSize = scaledSp(22), color = AppColors.TextMuted)
                    Text("%.1fL".format(r.liters), fontSize = scaledSp(29), color = AppColors.TextSecondary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("油耗", fontSize = scaledSp(22), color = AppColors.TextMuted)
                    Text(
                        item.consumptionL100km?.let { "%.1f".format(it) } ?: "—",
                        fontSize = scaledSp(29),
                        color = if (item.consumptionL100km != null) AppColors.Accent else AppColors.TextMuted,
                        fontWeight = if (item.consumptionL100km != null) FontWeight.Medium else FontWeight.Normal
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("每km", fontSize = scaledSp(22), color = AppColors.TextMuted)
                    Text(
                        item.costPerKm?.let { "%.2f".format(it) } ?: "—",
                        fontSize = scaledSp(29),
                        color = if (item.costPerKm != null) AppColors.Accent else AppColors.TextMuted,
                        fontWeight = if (item.costPerKm != null) FontWeight.Medium else FontWeight.Normal
                    )
                }
                // 修改 / 删除（和数据行水平对齐）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 13.dp)
                ) {
                    TextButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(53.dp)
                    ) {
                        Text("修改", fontSize = scaledSp(26), color = AppColors.Accent)
                    }
                    Spacer(Modifier.width(9.dp))
                    TextButton(
                        onClick = onDelete,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(53.dp)
                    ) {
                        Text("删除", fontSize = scaledSp(26), color = AppColors.TextError)
                    }
                }
            }
        }
    }
}
