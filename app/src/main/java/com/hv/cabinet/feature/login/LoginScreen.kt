package com.hv.cabinet.feature.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hv.cabinet.domain.MessageLevel
import com.hv.cabinet.ui.components.*
import com.hv.cabinet.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            if (effect is LoginEffect.LoginSuccess) {
                onLoginSuccess()
            }
        }
    }

    // 登录页专用全屏背景
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .background(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    0.0f to Color.White.copy(alpha = 0.04f),
                    1.0f to Color.Transparent,
                    center = androidx.compose.ui.geometry.Offset(760f, 0f),
                    radius = 980f
                )
            )
            .imePadding()
    ) {
        // 右上角设置
        AuraEntrance(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(CabinetSpacing.xl),
            delayMillis = 40,
            initialOffsetY = 12f,
            initialScale = 0.992f
        ) {
            Text(
                text = "服务器设置",
                color = TextSubtle,
                style = CabinetTypography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(999.dp))
                    .border(CabinetSpacing.borderThin, Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
                    .padding(horizontal = CabinetSpacing.md, vertical = CabinetSpacing.sm)
                    .clickable { showSettings = true }
            )
        }

        // 居中登录卡片 (ADS 抽象)
        AuraEntrance(
            modifier = Modifier.fillMaxSize(),
            delayMillis = 90,
            initialOffsetY = 26f,
            initialScale = 0.978f
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AuraSurface(
                    variant = AuraSurfaceVariant.SoftGlow,
                    modifier = Modifier.widthIn(max = CabinetSpacing.loginCardMaxWidth),
                    contentPadding = PaddingValues(22.dp)
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isWideLayout = maxWidth >= 760.dp

                        if (isWideLayout) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(CabinetSpacing.lg),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LoginHeroPane(
                                    modifier = Modifier.weight(1.15f)
                                )
                                LoginFormPane(
                                    state = state,
                                    viewModel = viewModel,
                                    modifier = Modifier.weight(0.92f)
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(CabinetSpacing.lg)
                            ) {
                                LoginHeroPane()
                                LoginFormPane(
                                    state = state,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showSettings) {
            SettingsOverlay(state, viewModel, onDismiss = { showSettings = false })
        }
    }
}

@Composable
private fun LoginHeroPane(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(CabinetSpacing.lg)
    ) {
        Text(
            text = "HCW Secure Device",
            color = TextDim,
            style = CabinetTypography.labelMedium,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.8.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "开始前先完成一次可信验证",
            color = TextMain,
            style = CabinetTypography.displaySmall,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 42.sp
        )
        Text(
            text = "更像一台高端终端，而不是后台表单。焦点只保留在身份、状态和进入系统三个动作。",
            color = TextSubtle,
            style = CabinetTypography.bodyMedium,
            lineHeight = 26.sp
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(CabinetSpacing.sm)
        ) {
            LoginMiniStat("账号登录")
            LoginMiniStat("NFC 常驻监听")
            LoginMiniStat("设备配置")
        }
    }
}

@Composable
private fun LoginMiniStat(text: String) {
    Text(
        text = text,
        color = TextSubtle,
        style = CabinetTypography.labelMedium,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
            .border(CabinetSpacing.borderThin, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun LoginFormPane(
    state: LoginUiState,
    viewModel: LoginViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.025f), RoundedCornerShape(20.dp))
            .border(CabinetSpacing.borderThin, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(CabinetSpacing.md)
    ) {
        LoginModeSelector(
            currentTab = state.tab,
            onSelect = viewModel::updateTab
        )

        if (state.tab == LoginTab.Account) {
            AccountLoginForm(state, viewModel)
        } else {
            NfcLoginForm()
        }

        if (state.message.text.isNotBlank()) {
            Text(
                text = state.message.text,
                color = if (state.message.level == MessageLevel.Error) DangerStart else AccentMint,
                style = CabinetTypography.bodySmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LoginModeSelector(
    currentTab: LoginTab,
    onSelect: (LoginTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CabinetSpacing.sm)
    ) {
        LoginModeOption(
            text = "账号登录",
            selected = currentTab == LoginTab.Account,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(LoginTab.Account) }
        )
        LoginModeOption(
            text = "NFC 登录",
            selected = currentTab == LoginTab.Nfc,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(LoginTab.Nfc) }
        )
    }
}

@Composable
private fun LoginModeOption(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .background(
                brush = if (selected) {
                    Brush.verticalGradient(listOf(PrimaryStart, PrimaryEnd))
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.04f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    )
                },
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = CabinetSpacing.borderThin,
                color = if (selected) Color.Transparent else Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) PrimaryInk else TextDim,
            style = CabinetTypography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AccountLoginForm(state: LoginUiState, viewModel: LoginViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(CabinetSpacing.lg)) {
        ADSTextField(
            value = state.userName,
            onValueChange = viewModel::updateUserName,
            label = "用户账号 / 访问令牌"
        )
        ADSTextField(
            value = state.password,
            onValueChange = viewModel::updatePassword,
            label = "安全口令",
            isPassword = true
        )
        AuraButton(
            text = if (state.loading) "验证中..." else "开启系统",
            style = AuraButtonStyle.Primary,
            modifier = Modifier.fillMaxWidth().height(CabinetSpacing.footerBarHeight),
            onClick = viewModel::login,
            enabled = !state.loading
        )
    }
}

@Composable
private fun NfcLoginForm() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = CabinetSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CabinetSpacing.md)
    ) {
        Text(text = "请刷 NFC 卡自动登录", color = TextMain, style = CabinetTypography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(text = "正在实时监听读卡器信号...", color = TextSubtle, style = CabinetTypography.bodyMedium)
        Spacer(modifier = Modifier.height(CabinetSpacing.sm))
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(CabinetSpacing.progressLineHeight),
            color = PrimaryStart,
            trackColor = GlassBorder
        )
    }
}

/**
 * ADS 规范化的输入框封装
 */
@Composable
private fun ADSTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextDim, style = CabinetTypography.labelSmall) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        shape = RoundedCornerShape(CabinetSpacing.buttonRadius),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextMain,
            unfocusedTextColor = TextMain,
            focusedBorderColor = PrimaryStart.copy(alpha = 0.72f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.06f),
            focusedContainerColor = Color.White.copy(alpha = 0.05f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
            cursorColor = PrimaryStart
        )
    )
}

@Composable
private fun SettingsOverlay(state: LoginUiState, viewModel: LoginViewModel, onDismiss: () -> Unit) {
    var closeAfterSave by remember { mutableStateOf(false) }
    var saveFlowStarted by remember { mutableStateOf(false) }
    LaunchedEffect(state.savingConfig, state.message.level, closeAfterSave) {
        if (!closeAfterSave) return@LaunchedEffect
        if (state.savingConfig) {
            saveFlowStarted = true
            return@LaunchedEffect
        }
        if (!saveFlowStarted) return@LaunchedEffect

        closeAfterSave = false
        saveFlowStarted = false
        if (state.message.level == MessageLevel.Success) {
            onDismiss()
        }
    }

    AuraModalLayer(
        scrimAlpha = 0.85f,
        onScrimClick = onDismiss
    ) {
        AuraSurface(
            variant = AuraSurfaceVariant.SoftGlow,
            modifier = Modifier
                .widthIn(max = CabinetSpacing.settingsCardMaxWidth)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CabinetSpacing.md)
            ) {
                Text("服务器设置", color = TextMain, style = CabinetTypography.titleMedium, fontWeight = FontWeight.Bold)

                ADSTextField(value = state.apiBaseUrl, onValueChange = viewModel::updateApiBaseUrl, label = "API 地址")
                ADSTextField(value = state.mqttBrokerUri, onValueChange = viewModel::updateMqttUri, label = "MQTT 地址")
                ADSTextField(value = state.mqttUsername, onValueChange = viewModel::updateMqttUser, label = "MQTT 用户名")
                ADSTextField(
                    value = state.mqttPassword,
                    onValueChange = viewModel::updateMqttPassword,
                    label = "MQTT 密码",
                    isPassword = true
                )
                ADSTextField(
                    value = state.cabinetIpsRaw,
                    onValueChange = viewModel::updateCabinetIps,
                    label = "柜体IP列表（兜底，逗号分隔）"
                )

                ConfigSwitchRow(
                    title = "NFC 格式转换",
                    subtitle = "按规则从刷卡值中截取并转为十进制",
                    checked = state.nfcFormatTransform,
                    onCheckedChange = viewModel::updateNfcFormatTransform
                )
                ConfigSwitchRow(
                    title = "转换严格模式",
                    subtitle = "转换失败时拦截登录",
                    checked = state.nfcFormatTransformStrict,
                    onCheckedChange = viewModel::updateNfcFormatTransformStrict,
                    enabled = state.nfcFormatTransform
                )
                ConfigSwitchRow(
                    title = "调试日志",
                    subtitle = "记录更多调试信息",
                    checked = state.debugLogEnabled,
                    onCheckedChange = viewModel::updateDebugLogEnabled
                )
                ConfigSwitchRow(
                    title = "性能日志",
                    subtitle = "记录页面和操作耗时",
                    checked = state.perfLogEnabled,
                    onCheckedChange = viewModel::updatePerfLogEnabled
                )
                ConfigSwitchRow(
                    title = "HTTP Body 日志",
                    subtitle = "请求/响应体日志（重启后生效）",
                    checked = state.httpBodyLogEnabled,
                    onCheckedChange = viewModel::updateHttpBodyLogEnabled
                )

                if (state.message.text.isNotBlank()) {
                    val msgColor = when (state.message.level) {
                        MessageLevel.Error -> DangerStart
                        MessageLevel.Warning -> WarnStart
                        MessageLevel.Success -> AccentMint
                        MessageLevel.Info -> TextSubtle
                    }
                    Text(
                        text = state.message.text,
                        color = msgColor,
                        style = CabinetTypography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(CabinetSpacing.md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CabinetSpacing.md)
                ) {
                    AuraButton(
                        text = "取消",
                        style = AuraButtonStyle.Normal,
                        modifier = Modifier
                            .weight(1f)
                            .height(CabinetSpacing.modalButtonHeight),
                        enabled = !state.savingConfig,
                        onClick = onDismiss
                    )
                    AuraButton(
                        text = if (state.savingConfig) "保存中..." else "保存并返回",
                        style = AuraButtonStyle.Primary,
                        modifier = Modifier
                            .weight(1f)
                            .height(CabinetSpacing.modalButtonHeight),
                        enabled = !state.savingConfig && !state.loading,
                        onClick = {
                            closeAfterSave = true
                            saveFlowStarted = false
                            viewModel.saveConfig()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(CabinetSpacing.xs)
        ) {
            Text(
                text = title,
                color = if (enabled) TextMain else TextDim.copy(alpha = 0.75f),
                style = CabinetTypography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = TextDim,
                style = CabinetTypography.bodySmall
            )
        }
        Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BgDark,
                    checkedTrackColor = PrimaryStart,
                    uncheckedThumbColor = TextMain,
                    uncheckedTrackColor = BtnBaseStart
                )
        )
    }
}
