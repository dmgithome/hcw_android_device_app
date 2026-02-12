# PRD：原生设备端 V2 一次性交付（UI + 功能完整）

## 一、摘要

本 PRD 用于将 `/Users/dm/code/hcw_android_device_app` 升级到可上线交付标准，覆盖 UI、核心业务流程、稳定性、性能与安装发布。  
执行口径：内部完成开发与门禁验证后再提测，过程内按任务清单逐项勾选并记录证据。

## 二、对外接口与类型变化

1. 后端 HTTP API：无变化。  
2. MQTT Topic：无变化。  
3. 业务规则口径：无变化。  
4. 新增内容仅为原生内部类型与 UI 状态模型，不影响管理端与后端接口。

## 三、任务清单（完成即勾选）

- [x] T01 新建 PRD 文档骨架与章节目录（目标文件：`/Users/dm/code/hcw_android_device_app/docs/PRD.md`）
- [x] T02 统一设计系统（色板/字体/间距/按钮/卡片/输入框）
- [x] T03 新增公共组件层（页面骨架、状态条、空态、错误态、操作区）
- [x] T04 登录页重构（账号/NFC 双模式、配置折叠区、输入校验、错误映射、加载态）
- [x] T05 首页重构（会话信息、双入口大卡片、按下反馈、防重复点击、退出确认）
- [x] T06 抽象取用/归还统一状态机（Idle/Starting/WaitingAck/Inventorying/Submitting/Done/Error）
- [x] T07 取用页重构（状态区、扫码区、异常区、清单区、提交区）
- [x] T08 归还页重构（同构布局、归还语义校验、异常提示）
- [x] T09 盘点启动超时与重试机制（ACK 超时、失败恢复、提示可操作）
- [x] T10 导航稳定性修复（登录后栈清理、登出回登录、防白屏回弹）
- [x] T11 网络错误产品化（401/403/404/超时/断网映射可读文案）
- [x] T12 MQTT 可靠性增强（断线重连恢复订阅、退订保障、解析容错）
- [x] T13 日志与性能埋点（点击响应、页面可交互、关键链路耗时）
- [x] T14 构建发布脚本与 README 同步（debug/release/install/perf 命令可直接执行）
- [ ] T15 全量功能回归（登录、取用、归还、确认并登出、异常恢复）
- [ ] T16 性能回归与提测包产出（debug/release + adb 指标对比）

## 四、已完成项证据

### T02 设计系统

- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/ui/theme/Color.kt`
- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/ui/theme/Type.kt`
- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/ui/theme/Theme.kt`

### T03 公共组件

- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/ui/components/AppScaffold.kt`
- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/ui/components/StatusBanner.kt`
- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/ui/components/ActionButtons.kt`
- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/ui/components/ConsumableListItem.kt`
- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/ui/components/EmptyState.kt`
- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/ui/components/FeatureCard.kt`

### T04-T05 页面重构

- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/feature/login/LoginScreen.kt`
- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/feature/login/LoginViewModel.kt`
- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/feature/home/HomeScreen.kt`
- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/feature/home/HomeViewModel.kt`

### T06-T09 业务流与状态机

- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/domain/DomainModels.kt`
- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/feature/take/TakeViewModel.kt`
- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/feature/take/TakeScreen.kt`
- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/feature/returning/ReturnViewModel.kt`
- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/feature/returning/ReturnScreen.kt`

### T10-T13 稳定性与性能

- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/navigation/CabinetApp.kt`
- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/data/api/CabinetRepository.kt`
- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/data/api/BaseUrlProvider.kt`
- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/data/mqtt/MqttManager.kt`
- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/core/PerfMonitor.kt`
- `/Users/dm/code/hcw_android_device_app/app/src/main/java/com/hv/cabinet/core/AppError.kt`

### T14 构建发布链路

- `/Users/dm/code/hcw_android_device_app/scripts/build_debug.sh`
- `/Users/dm/code/hcw_android_device_app/scripts/build_release.sh`
- `/Users/dm/code/hcw_android_device_app/scripts/install_debug.sh`
- `/Users/dm/code/hcw_android_device_app/scripts/perf_framestats.sh`
- `/Users/dm/code/hcw_android_device_app/README.md`

## 五、验证记录

### 编译与测试

```bash
cd /Users/dm/code/hcw_android_device_app
./gradlew assembleDebug -x lint
./gradlew testDebugUnitTest
./gradlew assembleRelease -x lint
```

结果：均通过。

### 安装与启动

```bash
adb install -r /Users/dm/code/hcw_android_device_app/app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop com.hv.cabinet
adb shell monkey -p com.hv.cabinet -c android.intent.category.LAUNCHER 1
```

结果：安装成功并可拉起应用。

### 自动化冒烟（已执行）

```bash
# 首页 -> 取用页 -> 返回 -> 归还页 -> 返回
adb shell input tap 960 322
adb shell input keyevent 4
adb shell input tap 960 789
adb shell input keyevent 4

# 退出登录确认弹窗出现
adb shell input tap 1875 56
adb shell input keyevent 4
```

结果：页面跳转正常，主控台/取用/归还关键文案均可识别，退出确认弹窗可正常出现与关闭。

### 性能采样（当前轮）

```bash
adb shell dumpsys gfxinfo com.hv.cabinet reset
adb shell monkey -p com.hv.cabinet 30
adb shell dumpsys gfxinfo com.hv.cabinet framestats
```

结果：已采样成功。  
说明：该采样为随机事件数据，`T15/T16` 需要按固定业务脚本（登录/取用/归还连续 30 次）做最终对比验收。

## 六、待完成项与阻塞

1. T15：账号登录、NFC 登录、提交流程仍需现场账号与外设链路做全量回归。  
2. T16：需按固定业务脚本（非 monkey）采集改造前后 `framestats` 对比，形成提测结论。

## 七、风险与回滚

1. 风险：一次性改动面大，仍可能存在现场设备兼容差异。  
2. 回滚：保留上一版可运行 APK，异常时执行 `adb install -r` 回装。  
