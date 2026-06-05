package com.fueltracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*

/**
 * 底部数字键盘弹窗 - 占屏幕 1/4，点击背景关闭
 */
@Composable
fun NumberPadDialog(
    initialValue: String = "",
    suffix: String = "",
    isDecimal: Boolean = true,
    maxLength: Int = 8,
    onDismiss: () -> Unit,
    onValueChange: (String) -> Unit
) {
    var editingValue by remember { mutableStateOf(initialValue) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 键盘区域（点击不关闭）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(AppColors.BgElevated)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 当前值显示 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (editingValue.isNotEmpty()) editingValue else "0",
                    fontSize = scaledSp(48),
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
                if (suffix.isNotEmpty()) {
                    Text(
                        text = suffix,
                        fontSize = scaledSp(28),
                        color = AppColors.TextSecondary
                    )
                }
            }

            // ── 数字键盘 4×3 网格 ──
            val buttons = listOf(
                listOf("7", "8", "9"),
                listOf("4", "5", "6"),
                listOf("1", "2", "3"),
                listOf(if (isDecimal) "." else "C", "0", "⌫")
            )

            buttons.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { key ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = when (key) {
                                "⌫" -> AppColors.CardDark
                                "C" -> AppColors.CardDark
                                else -> AppColors.BgSurface
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clickable {
                                    when (key) {
                                        "⌫" -> editingValue = editingValue.dropLast(1)
                                        "C" -> editingValue = ""
                                        "." -> {
                                            if (isDecimal && !editingValue.contains(".")) {
                                                editingValue = if (editingValue.isEmpty()) "0." else "$editingValue."
                                            }
                                        }
                                        else -> {
                                            if (editingValue.length < maxLength) {
                                                editingValue = if (editingValue == "0" && key != ".") key else editingValue + key
                                            }
                                        }
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                when (key) {
                                    "⌫" -> Text(
                                        text = "⌫",
                                        fontSize = scaledSp(28),
                                        fontWeight = FontWeight.Medium,
                                        color = AppColors.TextError
                                    )
                                    "C" -> Text(
                                        text = "C",
                                        fontSize = scaledSp(26),
                                        fontWeight = FontWeight.Medium,
                                        color = AppColors.Warning
                                    )
                                    "." -> Text(
                                        text = ".",
                                        fontSize = scaledSp(32),
                                        fontWeight = FontWeight.Medium,
                                        color = AppColors.TextPrimary
                                    )
                                    else -> Text(
                                        text = key,
                                        fontSize = scaledSp(32),
                                        fontWeight = FontWeight.Medium,
                                        color = AppColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(8.dp))

            // ── 确认（回车）按钮 ──
            Button(
                onClick = {
                    onValueChange(editingValue)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Accent,
                    contentColor = AppColors.TextOnAccent
                )
            ) {
                Text("确认", fontSize = scaledSp(28), fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
