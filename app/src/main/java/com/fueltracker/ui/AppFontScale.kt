package com.fueltracker.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 全局字体缩放管理
 * 5档：1=小(0.75x)  2=较小(0.88x)  3=中(1.0x/当前)  4=较大(1.12x)  5=大(1.25x)
 */
object AppFontScale {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_FONT_SCALE = "font_scale_level"

    /** 5档对应的缩放系数 */
    val SCALE_MAP = mapOf(
        1 to 0.75f,
        2 to 0.88f,
        3 to 1.00f,
        4 to 1.12f,
        5 to 1.25f
    )

    /** 档位中文名 */
    val LEVEL_LABELS = mapOf(
        1 to "小",
        2 to "较小",
        3 to "中",
        4 to "较大",
        5 to "大"
    )

    fun getScale(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val level = prefs.getInt(KEY_FONT_SCALE, 3)
        return SCALE_MAP[level] ?: 1.0f
    }

    fun setScale(context: Context, level: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_FONT_SCALE, level.coerceIn(1, 5)).apply()
    }

    fun getLevel(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_FONT_SCALE, 3)
    }
}

/** CompositionLocal：在 Composable 树中传递字体缩放值 */
val LocalFontScale = staticCompositionLocalOf { 1.0f }

/** 获取缩放后的字体大小（Int sp 基准值 → 缩放后的 TextUnit） */
@Composable
fun scaledSp(baseSp: Int): TextUnit {
    val scale = LocalFontScale.current
    return (baseSp * scale).sp
}

/** 获取缩放后的字体大小（Float sp 基准值 → 缩放后的 TextUnit） */
@Composable
fun scaledSp(baseSp: Float): TextUnit {
    val scale = LocalFontScale.current
    return (baseSp * scale).sp
}
