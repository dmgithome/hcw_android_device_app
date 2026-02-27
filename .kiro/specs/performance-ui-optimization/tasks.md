# Implementation Plan: 性能优化与 UI 简洁化

## Overview

渐进式优化方案，按风险从低到高排列：先做零风险的注解和缓存优化，再做 UI 改进，最后做构建配置变更（R8）。每个阶段都有检查点确保不引入回归。

## Tasks

- [x] 1. Domain Models 稳定性注解与测试基础设施
  - [x] 1.1 为 `ConsumableUiModel`、`UiMessage`、`ScanWarning`、`InventoryCoreState`、`TableColumn` 添加 `@Immutable` 注解
    - 修改 `app/src/main/java/com/hv/cabinet/domain/DomainModels.kt`
    - 修改 `app/src/main/java/com/hv/cabinet/ui/components/ConsumableDataTable.kt` 中的 `TableColumn`
    - 检查 `ConsumableDto` 是否需要同步标注
    - _Requirements: 1.1_
  - [x] 1.2 添加 Kotest Property Testing 依赖到 `app/build.gradle.kts`
    - 添加 `kotest-runner-junit5`、`kotest-property`、`kotest-assertions-core` 测试依赖
    - _Requirements: 测试基础设施_
  - [ ]* 1.3 编写属性测试：数据模型不可变性约束
    - **Property 1: 数据模型不可变性约束**
    - 使用反射验证所有 `@Immutable` 标注类的字段均为 val 且类型不可变
    - **Validates: Requirements 1.1**

- [x] 2. AppScaffold 与 ConsumableDataTable 性能优化
  - [x] 2.1 AppScaffold Brush 缓存优化
    - 修改 `app/src/main/java/com/hv/cabinet/ui/components/AppScaffold.kt`
    - 将 `backgroundBrush` 包裹在 `remember(variant)` 中
    - _Requirements: 1.3_
  - [x] 2.2 ConsumableDataTable 颜色常量提取
    - 修改 `app/src/main/java/com/hv/cabinet/ui/components/ConsumableDataTable.kt`
    - 将硬编码 Color 值提取为顶层 `private val` 常量
    - _Requirements: 1.4_

- [x] 3. HomeViewModel 导航延迟移除
  - [x] 3.1 移除 `launchNavigation` 中的 `delay(450)` 并简化为同步调用
    - 修改 `app/src/main/java/com/hv/cabinet/feature/home/HomeViewModel.kt`
    - 移除 `delay(450)` 和 `viewModelScope.launch` 包裹
    - 保留 `routeLocked` 防重复点击机制
    - _Requirements: 2.1_
  - [ ]* 3.2 编写属性测试：路由锁防重复导航
    - **Property 2: 路由锁防重复导航**
    - 验证 routeLocked=true 时连续调用不触发额外 navigate 回调
    - **Validates: Requirements 2.1**

- [x] 4. InventoryEventBatcher 优化
  - [x] 4.1 调整默认刷新窗口为 300ms
    - 修改 `app/src/main/java/com/hv/cabinet/domain/InventoryEventBatcher.kt`
    - 将 `flushWindowMs` 默认值从 140L 改为 300L
    - _Requirements: 4.1_
  - [ ]* 4.2 编写属性测试：事件批处理合并
    - **Property 3: 事件批处理合并**
    - 验证同一窗口内的多条事件合并为一次 onFlush 调用
    - **Validates: Requirements 4.2**

- [x] 5. Checkpoint - 性能优化验证
  - 确保所有测试通过，如有问题请告知。
  - 运行 `./gradlew :app:compileDebugKotlin` 确认编译通过

- [x] 6. ConsumableDataTable UI 可读性改进
  - [x] 6.1 添加交替行背景色（斑马纹）和改进表头样式
    - 修改 `app/src/main/java/com/hv/cabinet/ui/components/ConsumableDataTable.kt`
    - 偶数行使用 `Color(0x0DFFFFFF)` 背景，奇数行透明
    - 表头背景色加深为 `Color(0x441B4E88)`
    - 增加单元格内边距和行高
    - _Requirements: 5.1, 5.2, 5.4_
  - [x] 6.2 改进冲突行高亮样式
    - 冲突行使用 `Color(0x33E16969)` 背景（保持现有），优先级高于斑马纹
    - _Requirements: 5.3_
  - [ ]* 6.3 编写属性测试：交替行背景色
    - **Property 4: 交替行背景色**
    - 验证非冲突行的背景色由行索引奇偶决定
    - **Validates: Requirements 5.1**
  - [ ]* 6.4 编写属性测试：冲突行高亮
    - **Property 5: 冲突行高亮**
    - 验证冲突 RFID 行使用冲突背景色且优先于斑马纹
    - **Validates: Requirements 5.3**

- [x] 7. StatusBanner 图标增强与 EmptyState 改进
  - [x] 7.1 为 StatusBanner 添加消息级别对应的 Material Icons 图标和背景色
    - 修改 `app/src/main/java/com/hv/cabinet/ui/components/StatusBanner.kt`
    - Info→Outlined.Info, Success→Outlined.CheckCircle, Warning→Outlined.Warning, Error→Outlined.Error
    - _Requirements: 7.1, 7.2_
  - [x] 7.2 为 EmptyState 组件添加可选图标参数，更新 ConsumableDataTable 空状态
    - 修改 `app/src/main/java/com/hv/cabinet/ui/components/EmptyState.kt`
    - 修改 `app/src/main/java/com/hv/cabinet/ui/components/ConsumableDataTable.kt` 空状态部分
    - _Requirements: 6.2, 8.1_
  - [ ]* 7.3 编写属性测试：状态消息级别视觉映射
    - **Property 6: 状态消息级别视觉映射**
    - 验证每个 MessageLevel 映射到唯一的图标和背景色
    - **Validates: Requirements 7.1, 7.2**

- [x] 8. 登录页面简洁化与间距统一
  - [x] 8.1 登录页面添加品牌标识并统一间距
    - 修改 `app/src/main/java/com/hv/cabinet/feature/login/LoginScreen.kt`
    - 在登录卡片顶部添加应用名称文字标识
    - 统一卡片内部间距
    - _Requirements: 8.1, 8.2_
  - [x] 8.2 创建全局间距常量并统一各组件间距
    - 创建或修改间距常量定义
    - 审查并统一 ActionButtons、TransparentPanel、FeatureCard 等组件的间距和圆角
    - _Requirements: 9.1, 9.2_

- [x] 9. 加载状态改进
  - [x] 9.1 为取用/归还页面添加数据加载指示器
    - 修改 `app/src/main/java/com/hv/cabinet/feature/take/TakeScreen.kt`
    - 修改 `app/src/main/java/com/hv/cabinet/feature/returning/ReturnScreen.kt`
    - 在数据加载中显示 CircularProgressIndicator + 文字说明
    - _Requirements: 6.1, 6.3_

- [x] 10. Checkpoint - UI 改进验证
  - 确保所有测试通过，如有问题请告知。
  - 运行 `./gradlew :app:compileDebugKotlin` 确认编译通过

- [x] 11. 构建配置优化（R8 + Compose Compiler 升级）
  - [x] 11.1 启用 R8 并配置 ProGuard 规则
    - 修改 `app/build.gradle.kts`：Release 构建设置 `isMinifyEnabled = true`、`isShrinkResources = true`
    - 修改 `app/proguard-rules.pro`：添加 Kotlin Serialization、Retrofit、Hilt、Paho MQTT 的 keep 规则
    - _Requirements: 3.1, 3.2_
  - [x] 11.2 升级 Compose Compiler 扩展版本
    - 修改 `app/build.gradle.kts` 中的 `kotlinCompilerExtensionVersion`
    - 检查 Kotlin 版本兼容性矩阵，选择最新兼容版本
    - _Requirements: 2.2_

- [x] 12. 业务逻辑不变性验证
  - [ ]* 12.1 编写属性测试：取用提交必须选择目标位置
    - **Property 7: 取用提交必须选择目标位置**
    - 验证 targetLocationId 为空时 submit 被阻止
    - **Validates: Requirements 10.3**
  - [x] 12.2 验证 CabinetApi.kt 和 MqttManager.kt 未被修改
    - 确认 HTTP API 接口和 MQTT Topic 订阅逻辑无变更
    - _Requirements: 10.1, 10.2_

- [x] 13. Final Checkpoint - 全量验证
  - 确保所有测试通过，如有问题请告知。
  - 运行 `./gradlew :app:testDebugUnitTest`
  - 运行 `./gradlew :app:assembleDebug :app:assembleRelease`
  - 确认 Release APK 可正常安装运行

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 任务按风险从低到高排列：注解优化 → UI 改进 → 构建配置变更
- R8 启用（Task 11）是最高风险项，放在最后执行，确保前面的改动已验证通过
- 每个属性测试对应设计文档中的一个正确性属性
- 所有改动完成后需通过 `docs/验收标准_v2.md` 中的全部回归用例
