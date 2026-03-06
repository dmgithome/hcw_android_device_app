# hcw_android_device_app

设备端纯原生 Android 项目（Kotlin + Compose），用于替换原 WebView 设备端交互。

## 当前版本定位

- 版本：V2（UI + 功能完整化）
- 范围：登录（账号/NFC）、首页双入口、耗材取用、耗材归还、确认并登出
- 管理端：继续由 `/Users/dm/code/hcw_web_app` 维护

## 基础信息

- 包名：`com.hv.cabinet`
- 最低版本：`minSdk 23`
- 目标版本：`targetSdk 35`
- Java/Kotlin：`17 / 1.9.25`

## 项目结构

- `app/src/main/java/com/hv/cabinet/ui`
- `app/src/main/java/com/hv/cabinet/navigation`
- `app/src/main/java/com/hv/cabinet/data/api`
- `app/src/main/java/com/hv/cabinet/data/mqtt`
- `app/src/main/java/com/hv/cabinet/data/store`
- `app/src/main/java/com/hv/cabinet/domain`
- `app/src/main/java/com/hv/cabinet/feature/login`
- `app/src/main/java/com/hv/cabinet/feature/home`
- `app/src/main/java/com/hv/cabinet/feature/take`
- `app/src/main/java/com/hv/cabinet/feature/returning`
- `app/src/main/java/com/hv/cabinet/core`

## 后端与 MQTT 默认值

- API Base URL：`http://192.168.31.162:5099/`
- MQTT Broker：`tcp://172.25.5.250:1883`

登录页可修改并持久化设备配置（DataStore）。
MQTT 的 NFC 监听为应用运行期常驻开启，无需切换到“NFC 登录”页签即可触发刷卡登录。
取用页的“目标位置”采用内存缓存与首页预热策略，首次无缓存时加载，后续进入优先秒开并后台刷新。
登录后的“首页/取用/归还”采用工作区常驻切换，不再每次切页重建页面。
盘点流程采用“前端调用后端盘点接口 -> 后端按 IP 调用领还台本机读写器驱动 -> 驱动回传 MQTT 盘点状态”链路。
2026-03-03 起，盘点消息仅由当前激活页面处理，避免取用/归还页面互相串扰；同时修复“盘点已开始后被回写为等待并误报超时”的状态竞争问题。
2026-03-04 起，设备端 UI 采用 `C` 风格（深灰专业）并按横屏场景做轻量绘制优化：去除重渐变/重叠层，统一组件配色与信息层级，在不改变页面元素与业务逻辑前提下提升清晰度与稳定性。
2026-03-04 起，取用/归还详情页顶部标题改为页面名（“取用/归还”），并将“目标位置（选择）”小标题调整为可隐藏，默认在取用页以更紧凑布局展示。
2026-03-04 起，页面层接入 ADS v14（Aura Design System）组件：登录、首页、取用、归还统一使用 `AuraScreen/AuraSurface/AuraButton` 组合，并完成横屏间距 Token 化。
2026-03-04 起，取用页已移除“目标位置”小标题，仅保留可选位置项；归还页新增“位置自动推断”信息带，保持“顶部操作 + 中部说明 + 底部表格”的一致布局。
2026-03-04 起，首页双入口卡片改为“居中收窄”布局，避免双卡占满屏幕；“退出登录”按钮并入顶部标题栏，采用低饱和胶囊样式，降低突兀感。
2026-03-04 起，UI 细节继续优化：统一字号与按钮高度、顶部栏时间改为实时、取用/归还加入状态信息带、表格支持交替行与冲突高亮，保证“仅视觉优化，不改业务流程”。
2026-03-04 起，取用/归还页补齐“耗材异常提示”弹窗：提示分组与老项目一致（取用：已取用无法再次出库/未入库耗材/异常耗材；归还：未取用无法归还/未入库耗材/异常耗材），视觉统一为 ADS 风格，并调整为“盘点结束后一次性弹出”。

## 协议对齐（不变）

HTTP API：

- `POST /api/v1/amis/login`
- `POST /api/nfc/loginByNFC`
- `GET /api/v1/amis/user_info`
- `GET /api/inventory/callInventory?IP=...`
- `POST /api/stock/getConsumeOrReturnConsumablesByBarcode`
- `POST /api/stock/createTakeAndReturnLog`

MQTT Topic：

- `table/rfid/fast_tag/#`
- `table/rfid/inventory_status/#`
- `dk25_nfc/card/#`

## 构建与安装

```bash
cd /Users/dm/code/hcw_android_device_app
./gradlew clean assembleDebug -x lint
./gradlew clean assembleRelease -x lint
```

便捷脚本：

```bash
/Users/dm/code/hcw_android_device_app/scripts/build_debug.sh
/Users/dm/code/hcw_android_device_app/scripts/build_release.sh
/Users/dm/code/hcw_android_device_app/scripts/install_debug.sh
# 指定设备（自动按 5555 端口连接）
/Users/dm/code/hcw_android_device_app/scripts/install_debug.sh 192.168.31.134
```

说明：

- `install_debug.sh` 安装 debug 包后会自动执行一次设备侧 `speed` 编译优化，减少首次冷启动卡顿。
- 如设备已通过 `adb connect` 连接，可不传 IP 直接执行脚本。

产物：

- Debug APK：`/Users/dm/code/hcw_android_device_app/app/build/outputs/apk/debug/app-debug.apk`
- Release APK：`/Users/dm/code/hcw_android_device_app/app/build/outputs/apk/release/app-release.apk`

## 性能采样（ADB）

```bash
# 参数1=包名(可选)，参数2=monkey事件数(可选，默认30)
/Users/dm/code/hcw_android_device_app/scripts/perf_framestats.sh com.hv.cabinet 30
```

## 质量门禁建议

```bash
cd /Users/dm/code/hcw_android_device_app
./gradlew assembleDebug -x lint
./gradlew testDebugUnitTest
./gradlew assembleRelease -x lint
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## PRD 与任务清单

- 交付 PRD：`/Users/dm/code/hcw_android_device_app/docs/PRD.md`
