package com.fueltracker.ui

import androidx.compose.ui.graphics.Color

/**
 * 车机深色主题色板 - 苹果绿版
 * 设计原则：
 * - 背景亮度 ≤ 20%，避免夜间眩光
 * - 主色调用苹果绿色系，清新醒目且不伤眼
 * - 文本对比度 ≥ 7:1，确保强光/夜间均可读
 */
object AppColors {

    // ── 背景 ──
    /** 主背景：极深灰黑 */
    val BgDark       = Color(0xFF0D0D0D)
    /** 次级背景：用于卡片、输入框底色 */
    val BgSurface    = Color(0xFF1A1A1A)
    /** 悬浮/弹窗背景 */
    val BgElevated   = Color(0xFF242424)

    // ── 卡片 ──
    /** 通用卡片背景 */
    val CardDark     = Color(0xFF1C1C1E)
    /** 统计卡片：深绿色 */
    val CardAccent   = Color(0xFF1A2E1A)
    /** 车辆信息卡片：深绿灰 */
    val CardWarm     = Color(0xFF162016)
    /** 选中/高亮卡片 */
    val CardSelected = Color(0xFF182A18)

    // ── 文本 ──
    /** 主文本：近白 */
    val TextPrimary   = Color(0xFFF0F0F0)
    /** 次级文本：浅灰 */
    val TextSecondary = Color(0xFFB0B0B0)
    /** 弱提示文本：中灰 */
    val TextMuted     = Color(0xFF6E6E6E)
    /** 强调色上的文本：深绿黑 */
    val TextOnAccent  = Color(0xFF0A1A00)
    /** 错误/警示文本 */
    val TextError     = Color(0xFFFF6B5B)

    // ── 强调色（苹果绿色系，清新醒目）──
    /** 主强调色：苹果绿 */
    val Accent        = Color(0xFF7EC850)
    /** 强调色悬停/按下态 */
    val AccentHover   = Color(0xFF9AD970)
    /** 强调色弱化 */
    val AccentSoft    = Color(0xFF7EC850).copy(alpha = 0.15f)
    /** 分隔线/边框 */
    val Divider       = Color(0xFF2C2C2E)
    /** 输入框边框 */
    val Border        = Color(0xFF3A3A3C)

    // ── 语义色（低饱和，不刺眼）──
    /** 成功：暗绿 */
    val Success       = Color(0xFF4CAF50)
    /** 警告：暗黄 */
    val Warning       = Color(0xFFFFC107)
    /** 信息：暗青 */
    val Info          = Color(0xFF29B6F6)
}
