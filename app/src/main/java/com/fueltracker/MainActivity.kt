package com.fueltracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.fueltracker.data.FuelRecord
import com.fueltracker.ui.AddRecordScreen
import com.fueltracker.ui.AppColors
import com.fueltracker.ui.AppFontScale
import com.fueltracker.ui.LocalFontScale
import com.fueltracker.ui.MainScreen
import com.fueltracker.ui.QueryScreen
import com.fueltracker.ui.VehicleSettingsScreen

/**
 * 主 Activity - 车机加油记录 App 入口
 *
 * 车机适配要点：
 * 1. 禁用横屏旋转（车机通常固定横屏）
 * 2. 全屏显示，隐藏状态栏
 * 3. 深色模式自动适配（夜间驾驶）
 */
class MainActivity : ComponentActivity() {

    private var currentScreen by mutableStateOf("main")  // "main" | "add" | "query" | "settings"
    private var realMileage by mutableStateOf<Int?>(null)  // 真实里程（从 Car API 读取）
    private var carPropertyManager: Any? = null
    private var recordToEdit by mutableStateOf<FuelRecord?>(null)  // 要编辑的记录（null = 新增模式）

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── 初始化 Car API（真车读取里程）──
        initCarApi()

        // 车机全屏（隐藏状态栏/导航栏）- 新版 API
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            val context = LocalContext.current
            val fontScale = remember { AppFontScale.getScale(context) }

            // 车机强制深色模式，保护夜间驾驶安全
            CompositionLocalProvider(LocalFontScale provides fontScale) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = AppColors.BgDark
                    ) {
                        when (currentScreen) {
                        "main" -> MainScreen(
                            realMileage = realMileage,
                            onAddClick = { currentScreen = "add" },
                            onQueryClick = { currentScreen = "query" },
                            onSettingsClick = { currentScreen = "settings" },
                            onEditRecord = { record ->
                                recordToEdit = record
                                currentScreen = "add"
                            },
                            onExitClick = { finish() }
                        )
                        "add" -> AddRecordScreen(
                            recordToEdit = recordToEdit,
                            onBack = {
                                recordToEdit = null
                                currentScreen = "main"
                            },
                            onSaved = {
                                recordToEdit = null
                                currentScreen = "main"
                            }
                        )
                        "query" -> QueryScreen(
                            onBack = { currentScreen = "main" }
                        )
                        "settings" -> VehicleSettingsScreen(
                            onBack = { currentScreen = "main" }
                        )
                        }
                    }
                }
            }
        }
    }

    /**
     * 初始化 Car API - 真车读取里程（反射方式，无需编译依赖）
     * 模拟器/非车机环境会自动降级，不崩溃
     */
    private fun initCarApi() {
        try {
            // 反射获取 Car 类
            val carClass = Class.forName("android.car.Car")
            val createCarMethod = carClass.getMethod("createCar", android.content.Context::class.java)
            val car = createCarMethod.invoke(null, this)

            // 反射获取 PROPERTY_SERVICE
            val propertyServiceField = carClass.getField("PROPERTY_SERVICE")
            val propertyService = propertyServiceField.get(null) as String

            // 反射获取 CarPropertyManager
            val getCarManagerMethod = carClass.getMethod("getCarManager", String::class.java)
            carPropertyManager = getCarManagerMethod.invoke(car, propertyService)

            // 读取里程
            readMileage()
        } catch (e: Exception) {
            // 非车机环境（模拟器）忽略，realMileage 保持 null
            e.printStackTrace()
        }
    }

    /**
     * 读取真实里程（AAOS 车辆属性 - 反射调用）
     * 如果读取失败，realMileage 保持 null，UI 显示模拟数据
     */
    private fun readMileage() {
        try {
            val cpmClass = Class.forName("android.car.CarPropertyManager")
            val getIntMethod = cpmClass.getMethod("getIntProperty", Integer.TYPE, Integer.TYPE)

            // 反射获取 VehiclePropertyIds.ODO_METER = 0x11600204
            val vpClass = Class.forName("android.car.VehiclePropertyIds")
            val odoField = vpClass.getField("ODO_METER")
            val odoId = odoField.getInt(null)

            val mileage = getIntMethod.invoke(carPropertyManager, odoId, 0) as? Int
            runOnUiThread {
                realMileage = mileage
            }
        } catch (e: Exception) {
            // 权限不足或属性不支持，保持 null
            e.printStackTrace()
        }
    }
}
