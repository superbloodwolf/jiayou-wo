# ⛽ FuelTracker — 车机加油记录 App

> 基于 Android Automotive OS（AAOS）的车机加油记录应用  
> 支持哈弗大狗等长城 Coffee OS 车机（向下兼容普通 Android）

---

## ✅ 已实现功能（基础版）

| 功能 | 状态 | 说明 |
|------|------|------|
| 加油记录录入 | ✅ | 加油站类型、油品、油价、加油量、金额、里程 |
| 自动计算金额 | ✅ | 油价 × 加油量，也可手动修改 |
| 自动计算油耗 | ✅ | 基于上次里程自动计算 L/100km |
| 加油历史列表 | ✅ | 按时间倒序，显示油耗和行程 |
| 本月统计卡片 | ✅ | 加油次数、总花费、平均油耗 |
| 车机 UI 适配 | ✅ | 大按钮、大字、高对比度 |
| 深色/浅色模式 | ✅ | 自动跟随系统 |
| 本地数据库 | ✅ | Room SQLite，无需联网 |
| 车辆里程读取 | ⚠️ | 需车机 Car API 权限（无权限则手动输入）|

---

## 🚀 快速启动

### 1. 用 Android Studio 打开项目
```
打开 FuelTracker/ 文件夹
等待 Gradle 同步完成（约 1~2 分钟）
```

### 2. 连接车机（或 AAOS 模拟器）
```bash
# 查看连接设备
adb devices

# 安装到车机
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. 运行
点击 Android Studio 的 **Run ▶** 按钮，选择车机设备。

---

## 📐 项目结构

```
app/src/main/java/com/fueltracker/
├── MainActivity.kt          ← 入口，页面路由
├── FuelTrackerApp.kt       ← Application 初始化
├── ui/
│   ├── MainScreen.kt      ← 主界面（列表 + 统计）
│   └── AddRecordScreen.kt ← 新增加油记录
├── data/
│   ├── FuelRecord.kt      ← 数据库实体
│   ├── FuelRecordDao.kt  ← 数据库操作
│   ├── AppDatabase.kt     ← Room 数据库
│   └── FuelRepository.kt  ← 数据仓库层
└── util/
    └── CarUtils.kt        ← 车辆 API + 油价工具
```

---

## 🔧 配置说明

### 关闭 Car API（模拟器/无权限时）
在 `AddRecordScreen.kt` 中，车辆里程读取默认关闭，改为手动输入。

若需在真车机上启用：
1. 在 `AndroidManifest.xml` 取消注释 Car 权限
2. 在车机上授予 `CAR_SPEED` 权限

### 修改油价接口
在 `CarUtils.kt` 的 `FuelPriceApi` 中填入你的 API Key：
```kotlin
// 聚合数据油价接口（示例）
// https://www.juhe.cn/docs/api/id/205
const val API_KEY = "你的API_KEY"
const val API_URL = "https://opendata.baidu.com/api.php?..."
```

---

## 📱 车机适配要点

- ✅ 横屏锁定（`screenOrientation="landscape"`）
- ✅ 按钮最小 48dp（驾驶时盲操友好）
- ✅ 字体 ≥ 16sp
- ✅ 全屏显示（隐藏状态栏/导航栏）
- ✅ AAOS 桌面入口（`AUTOMOTIVE_SPACE` category）

---

## 📋 下一步可扩展功能

- [ ] 油价自动获取（对接聚合数据 API）
- [ ] 加油小票 OCR 识别（自动填充）
- [ ] 油耗趋势图表（MPAndroidChart）
- [ ] 导出 CSV / 分享给微信
- [ ] 多车管理
- [ ] 与车机导航联动（自动记录加油地点）
- [ ] 私有 SDK 深度集成（需长城合作）

---

## ⚠️ 注意事项

- **Car API 权限**：需车机厂商授权，无权限时自动降级为手动输入，不影响核心功能。
- **AAOS 模拟器**：标准 Android 模拟器无法测试 Car API，需用 Android Studio 的 **Automotive OS 模拟器**。
- **安装到车机**：需开启车机 `开发者模式`（设置 → 关于 → 连点版本号 7 次）。

---

## 📄 许可

MIT License — 可自由修改、商用，但涉及车辆控制部分请遵守相关法规。
