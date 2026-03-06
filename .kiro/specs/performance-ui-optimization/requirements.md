# 需求文档

## 简介

本需求针对耗材屋安卓设备端（`com.hv.cabinet`）在嵌入式 RK3568 硬件上的性能优化与 UI 简洁化改进。应用当前存在明显卡顿（Compose 重组过度、高频 MQTT 事件频繁刷新、R8 未启用、不必要的人为延迟等）以及 UI 可读性不足（数据表密集难读、空状态简陋、间距不统一等）。所有改动严格限定在不改变业务逻辑、HTTP API、MQTT Topic 和操作流程的前提下进行。UI 改进以简洁、清晰、高可读性为原则，不引入任何增加渲染负担的动画或特效。

## 术语表

- **System（系统）**：耗材屋安卓设备端应用（`com.hv.cabinet`）
- **ConsumableDataTable（耗材数据表）**：取用/归还页面中展示耗材列表的 Compose 组件
- **AppScaffold（应用脚手架）**：全局页面布局容器组件，包含背景渐变、顶栏等
- **InventoryEventBatcher（盘点事件批处理器）**：聚合高频 RFID 消息的批处理工具类
- **Recomposition（重组）**：Compose 框架中因状态变化触发的 UI 重新计算与绘制过程
- **R8**：Android 构建工具链中的代码缩减与优化器
- **RK3568**：瑞芯微嵌入式处理器平台，本应用的目标运行硬件

## 需求

### 需求 1：Compose 重组性能优化

**用户故事：** 作为设备操作员，我希望在高频 RFID 扫描期间界面保持流畅响应，以便我能顺利完成取用和归还操作。

#### 验收标准

1. THE System SHALL 为 `ConsumableUiModel` 和 `InventoryCoreState` 等频繁传入 Composable 的数据类添加 `@Immutable` 或 `@Stable` 注解，以减少不必要的重组
2. WHEN ConsumableDataTable 接收到新的列表数据时，THE System SHALL 仅重组发生变化的行，而非整个表格
3. THE System SHALL 将 AppScaffold 中的渐变 Brush 对象缓存为 `remember` 值，避免每次重组时重新创建
4. THE System SHALL 将 ConsumableDataTable 中频繁使用的 Color 常量提取为顶层或 companion object 常量，避免在重组时重复创建对象

### 需求 2：导航与启动延迟优化

**用户故事：** 作为设备操作员，我希望页面切换快速无延迟，以便我能高效完成日常操作。

#### 验收标准

1. WHEN 用户从首页点击进入取用或归还页面时，THE System SHALL 移除 HomeViewModel 中 `launchNavigation` 方法里的 `delay(450)` 调用，改用基于导航完成回调的防重复点击机制
2. THE System SHALL 将 Compose Compiler 扩展版本升级至与当前 Kotlin 版本兼容的最新稳定版，以获取最新的编译器优化

### 需求 3：构建配置优化

**用户故事：** 作为开发者，我希望 Release 构建启用代码缩减和优化，以便应用在嵌入式设备上运行更高效。

#### 验收标准

1. THE System SHALL 在 Release 构建类型中启用 `isMinifyEnabled = true`，开启 R8 代码缩减与优化
2. WHEN 启用 R8 后，THE System SHALL 配置正确的 ProGuard 规则，确保 Kotlin Serialization、Retrofit、Hilt、Paho MQTT 等依赖库正常运行
3. THE System SHALL 在启用 R8 后通过完整的功能回归测试，确保所有业务流程不受影响

### 需求 4：RFID 事件批处理优化

**用户故事：** 作为设备操作员，我希望在高频 RFID 扫描时界面不出现明显卡顿，以便我能持续放置耗材而不中断操作。

#### 验收标准

1. THE System SHALL 将 InventoryEventBatcher 的默认刷新窗口从 140ms 调整为更适合 RK3568 硬件的值（建议 250-350ms），以减少 UI 刷新频率
2. WHEN 批处理器在一个时间窗口内累积了多条 RFID 事件时，THE System SHALL 合并为一次 StateFlow 更新，避免多次连续触发 UI 重组
3. IF InventoryEventBatcher 的刷新窗口调整后仍出现卡顿，THEN THE System SHALL 提供可配置的窗口参数以便针对不同硬件调优

### 需求 5：数据表可读性改进

**用户故事：** 作为设备操作员，我希望耗材数据表格清晰易读，以便我能快速识别和核对耗材信息。

#### 验收标准

1. THE System SHALL 为 ConsumableDataTable 的数据行添加交替行背景色（斑马纹），提升行间区分度
2. THE System SHALL 为表头行提供更明显的视觉区分（如加深背景色、增加底部分隔线）
3. WHEN 数据表中存在冲突 RFID 条目时，THE System SHALL 以清晰的高亮背景色标识冲突行，使操作员能快速定位
4. THE System SHALL 为数据表的单元格提供适当的内边距和行高，确保在 RK3568 设备屏幕上文字不拥挤且易于触摸操作

### 需求 6：加载与空状态改进

**用户故事：** 作为设备操作员，我希望在数据加载和无数据时看到明确的状态提示，以便我知道系统当前状态和下一步操作。

#### 验收标准

1. WHEN 取用或归还页面正在加载耗材数据时，THE System SHALL 显示简洁的加载指示器（如 CircularProgressIndicator 配合文字说明），告知用户数据正在获取中
2. WHEN ConsumableDataTable 的数据列表为空时，THE System SHALL 显示包含图标和引导文案的空状态视图，替代当前的纯文字提示
3. WHEN 目标位置列表正在加载时，THE System SHALL 显示加载指示器

### 需求 7：状态消息图标增强

**用户故事：** 作为设备操作员，我希望状态提示信息带有直观的图标，以便我能快速判断当前操作状态。

#### 验收标准

1. THE System SHALL 为不同级别的状态消息（Info、Success、Warning、Error）配置对应的 Material Icons 图标
2. THE System SHALL 为状态提示栏提供与消息级别匹配的背景色，增强视觉层次

### 需求 8：登录页面简洁化改进

**用户故事：** 作为设备操作员，我希望登录页面简洁专业，以便快速完成登录操作。

#### 验收标准

1. THE System SHALL 为登录页面添加应用名称或品牌标识区域，提升页面辨识度
2. THE System SHALL 确保登录卡片的边框、间距和字体层级清晰统一

### 需求 9：间距与视觉一致性

**用户故事：** 作为设备操作员，我希望界面各元素间距统一协调，以便获得整洁专业的视觉体验。

#### 验收标准

1. THE System SHALL 统一全局面板间距、内边距和圆角值，确保各页面视觉风格一致
2. THE System SHALL 确保所有按钮（PrimaryButton、ConfirmButton、OutlinedConfirmButton）的尺寸、间距和圆角保持一致的设计语言

### 需求 10：业务逻辑不变性保障

**用户故事：** 作为项目负责人，我希望所有优化改动不影响现有业务功能，以便确保系统稳定可靠。

#### 验收标准

1. THE System SHALL 确保所有性能优化和 UI 改进不改变任何 HTTP API 调用路径、请求参数和响应处理逻辑
2. THE System SHALL 确保所有改动不改变 MQTT Topic 订阅、消息解析和事件处理逻辑
3. THE System SHALL 确保取用流程中目标位置必须选择、归还流程中并发校验、409 冲突提示等业务规则保持不变
4. THE System SHALL 在所有改动完成后通过 `docs/验收标准_v2.md` 中定义的全部功能回归用例
