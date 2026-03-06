package com.hv.cabinet.ui.theme

import androidx.compose.ui.unit.dp

object CabinetSpacing {
    // 基础原子间距
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 18.dp
    val xl = 24.dp
    val massive = 24.dp

    // 场景专用间距
    val screenPadding = 18.dp
    val panelPadding = 20.dp
    val actionGap = 14.dp
    val sectionGap = panelPadding // 兼容旧组件默认参数

    // 页面尺寸
    val topBarHeight = 50.dp
    val actionButtonHeight = 52.dp
    val footerBarHeight = 52.dp
    val modalButtonHeight = 52.dp
    val progressLineHeight = 3.dp
    val loginCardMaxWidth = 920.dp
    val settingsCardMaxWidth = 520.dp
    val confirmModalMaxWidth = 480.dp
    val homeCardMaxHeight = 324.dp
    val homeCardGap = 16.dp

    // 形状 Token
    val cardRadius = 24.dp
    val buttonRadius = 18.dp
    val chipRadius = 999.dp
    val borderThin = 1.dp
    val borderThick = 1.dp
}
