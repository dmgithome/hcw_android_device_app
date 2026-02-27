package com.hv.cabinet.feature.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hv.cabinet.domain.MessageLevel
import com.hv.cabinet.ui.components.PrimaryButton
import com.hv.cabinet.ui.components.TransparentPanel
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF031026), Color(0xFF0A1E3D), Color(0xFF07172D))
                )
            )
            .imePadding()
    ) {
        // Right-top settings button
        TextButton(
            onClick = { showSettings = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 16.dp)
        ) {
            Text(
                "服务器设置",
                color = Color(0xAA52C7EA),
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Centered login card
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 460.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0x33192E4A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4452C7EA))
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 品牌标识区域
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "耗材屋",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF52C7EA),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "智能耗材管理系统",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    // 品牌区域与登录表单的分隔线
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0x2252C7EA))
                    )

                    // Login method selector (simplified chip row)
                    LoginMethodChipRow(
                        selected = state.tab,
                        onSelect = viewModel::updateTab
                    )

                    // Form content
                    if (state.tab == LoginTab.Account) {
                        AccountLoginForm(state = state, viewModel = viewModel)
                    } else {
                        NfcLoginForm(state = state)
                    }

                    // Error/info message
                    if (state.message.text.isNotBlank()) {
                        Text(
                            text = state.message.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = when (state.message.level) {
                                MessageLevel.Error -> Color(0xFFE16969)
                                MessageLevel.Warning -> Color(0xFFE1B869)
                                MessageLevel.Success -> Color(0xFF55C68E)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Loading overlay
        if (state.loading) {
            LoginLoadingOverlay(state = state)
        }

        // Settings overlay
        if (showSettings) {
            SettingsOverlay(
                state = state,
                viewModel = viewModel,
                onDismiss = { showSettings = false }
            )
        }
    }
}

@Composable
private fun LoginMethodChipRow(
    selected: LoginTab,
    onSelect: (LoginTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        LoginChip(
            text = "账号登录",
            selected = selected == LoginTab.Account,
            onClick = { onSelect(LoginTab.Account) }
        )
        LoginChip(
            text = "NFC 登录",
            selected = selected == LoginTab.Nfc,
            onClick = { onSelect(LoginTab.Nfc) }
        )
    }
}

@Composable
private fun LoginChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Color(0x4452C7EA) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) Color(0x8852C7EA) else Color(0x336C93C8)
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color(0xFF52C7EA) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun AccountLoginForm(
    state: LoginUiState,
    viewModel: LoginViewModel
) {
    OutlinedTextField(
        value = state.userName,
        onValueChange = viewModel::updateUserName,
        label = { Text("账号") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    OutlinedTextField(
        value = state.password,
        onValueChange = viewModel::updatePassword,
        label = { Text("密码") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    PrimaryButton(
        text = "登录",
        enabled = !state.loading,
        loading = state.loading,
        modifier = Modifier.fillMaxWidth(),
        onClick = viewModel::login
    )
}

@Composable
private fun NfcLoginForm(state: LoginUiState) {
    Text(
        "请刷 NFC 卡自动登录",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
    OutlinedTextField(
        value = state.nfcReaderId,
        onValueChange = {},
        readOnly = true,
        label = { Text("读卡器 ReaderId(MAC)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    OutlinedTextField(
        value = state.nfcCardNo,
        onValueChange = {},
        readOnly = true,
        label = { Text("解析后的卡号") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    PrimaryButton(
        text = "等待刷卡登录",
        enabled = false,
        loading = state.loading,
        modifier = Modifier.fillMaxWidth(),
        onClick = {}
    )
}

@Composable
private fun SettingsOverlay(
    state: LoginUiState,
    viewModel: LoginViewModel,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .clickable(enabled = false, onClick = {}), // block click-through
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF0E1E34),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4452C7EA))
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "服务器设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Config fields
                OutlinedTextField(
                    value = state.apiBaseUrl,
                    onValueChange = viewModel::updateApiBaseUrl,
                    label = { Text("API Base URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.mqttBrokerUri,
                    onValueChange = viewModel::updateMqttUri,
                    label = { Text("MQTT Broker URI") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.mqttUsername,
                    onValueChange = viewModel::updateMqttUser,
                    label = { Text("MQTT 用户名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.mqttPassword,
                    onValueChange = viewModel::updateMqttPassword,
                    label = { Text("MQTT 密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.cabinetIpsRaw,
                    onValueChange = viewModel::updateCabinetIps,
                    label = { Text("柜体IP列表（兜底，逗号分隔）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ConfigSwitchRow(
                    title = "NFC格式转换",
                    subtitle = "按 Web 规则从 Value 中截取并转十进制",
                    checked = state.nfcFormatTransform,
                    onCheckedChange = viewModel::updateNfcFormatTransform
                )
                ConfigSwitchRow(
                    title = "转换严格模式",
                    subtitle = "转换失败时拦截，不继续登录",
                    checked = state.nfcFormatTransformStrict,
                    onCheckedChange = viewModel::updateNfcFormatTransformStrict,
                    enabled = state.nfcFormatTransform
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "调试日志",
                    style = MaterialTheme.typography.titleSmall
                )
                ConfigSwitchRow(
                    title = "调试日志",
                    subtitle = "打印更多调试信息",
                    checked = state.debugLogEnabled,
                    onCheckedChange = viewModel::updateDebugLogEnabled
                )
                ConfigSwitchRow(
                    title = "性能日志",
                    subtitle = "记录页面和操作耗时打点",
                    checked = state.perfLogEnabled,
                    onCheckedChange = viewModel::updatePerfLogEnabled
                )
                ConfigSwitchRow(
                    title = "HTTP BODY日志",
                    subtitle = "请求/响应体日志（重启后生效）",
                    checked = state.httpBodyLogEnabled,
                    onCheckedChange = viewModel::updateHttpBodyLogEnabled
                )

                // Error message in settings
                if (state.message.text.isNotBlank()) {
                    Text(
                        text = state.message.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (state.message.level) {
                            MessageLevel.Error -> Color(0xFFE16969)
                            MessageLevel.Success -> Color(0xFF55C68E)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                PrimaryButton(
                    text = "保存配置",
                    enabled = !state.savingConfig && !state.loading,
                    loading = state.savingConfig,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = viewModel::saveConfig
                )
            }
        }
    }
}

@Composable
private fun LoginLoadingOverlay(state: LoginUiState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000)),
        contentAlignment = Alignment.Center
    ) {
        TransparentPanel(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(16.dp),
            startColor = Color(0x33334E74),
            endColor = Color(0x220E1A2F),
            borderColor = Color(0x6652C7EA)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp)
                Text(
                    text = if (state.tab == LoginTab.Nfc) "正在处理刷卡登录..." else "正在登录...",
                    textAlign = TextAlign.Center
                )
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
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}
