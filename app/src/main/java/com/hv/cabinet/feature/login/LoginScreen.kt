package com.hv.cabinet.feature.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                    0.0f to BgAuraCore,
                    1.0f to BgDark,
                    center = androidx.compose.ui.geometry.Offset(500f, 500f),
                    radius = 1500f
                )
            )
            .imePadding()
    ) {
        // 右上角设置
        Text(
            text = "服务器设置",
            color = TextDim.copy(alpha = 0.9f),
            style = CabinetTypography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(CabinetSpacing.xl)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(999.dp))
                .border(CabinetSpacing.borderThin, GlassBorder, RoundedCornerShape(999.dp))
                .padding(horizontal = CabinetSpacing.md, vertical = CabinetSpacing.sm)
                .clickable { showSettings = true }
        )

        // 居中登录卡片 (ADS 抽象)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AuraSurface(
                modifier = Modifier.widthIn(max = CabinetSpacing.loginCardMaxWidth),
                contentPadding = PaddingValues(CabinetSpacing.massive)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CabinetSpacing.xl)
                ) {
                    // 品牌标识
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "身份授权",
                            color = TextMain,
                            style = CabinetTypography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(CabinetSpacing.sm))
                        Text(
                            text = "请完成安全验证以开始工作",
                            color = TextDim,
                            style = CabinetTypography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 登录方式切换 (ADS 抽象标签)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CabinetSpacing.md)
                    ) {
                        AuraLocationChip(
                            text = "账号登录",
                            isSelected = state.tab == LoginTab.Account,
                            onClick = { viewModel.updateTab(LoginTab.Account) },
                            modifier = Modifier.weight(1f)
                        )
                        AuraLocationChip(
                            text = "NFC 登录",
                            isSelected = state.tab == LoginTab.Nfc,
                            onClick = { viewModel.updateTab(LoginTab.Nfc) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 表单
                    if (state.tab == LoginTab.Account) {
                        AccountLoginForm(state, viewModel)
                    } else {
                        NfcLoginForm()
                    }

                    // 消息反馈
                    if (state.message.text.isNotBlank()) {
                        Text(
                            text = state.message.text,
                            color = if (state.message.level == MessageLevel.Error) DangerStart else AccentMint,
                            style = CabinetTypography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
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
        modifier = Modifier.fillMaxWidth().padding(vertical = CabinetSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CabinetSpacing.md)
    ) {
        Text(text = "请刷 NFC 卡自动登录", color = TextMain, style = CabinetTypography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = "正在实时监听读卡器信号...", color = TextDim, style = CabinetTypography.bodyMedium)
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
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = PrimaryStart,
            unfocusedBorderColor = GlassBorder,
            focusedContainerColor = Color.White.copy(alpha = 0.03f),
            unfocusedContainerColor = Color.Transparent,
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

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        AuraSurface(modifier = Modifier.widthIn(max = CabinetSpacing.settingsCardMaxWidth).clickable(enabled = false) {}) {
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
                        MessageLevel.Info -> PrimaryStart
                    }
                    Text(
                        text = state.message.text,
                        color = msgColor,
                        style = CabinetTypography.bodySmall,
                        fontWeight = FontWeight.SemiBold
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
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryStart,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = BtnBaseStart
            )
        )
    }
}
