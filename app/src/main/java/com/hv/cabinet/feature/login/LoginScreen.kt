package com.hv.cabinet.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hv.cabinet.ui.components.AppScaffold
import com.hv.cabinet.ui.components.PrimaryButton
import com.hv.cabinet.ui.components.StatusBanner

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is LoginEffect.LoginSuccess) {
                onLoginSuccess()
            }
        }
    }

    AppScaffold(
        title = "智能柜设备端",
        subtitle = "账号 / NFC 登录"
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusBanner(message = state.message)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("用户登录", style = MaterialTheme.typography.titleMedium)
                    TabRow(selectedTabIndex = if (state.tab == LoginTab.Account) 0 else 1) {
                        Tab(
                            selected = state.tab == LoginTab.Account,
                            onClick = { viewModel.updateTab(LoginTab.Account) },
                            text = { Text("账号登录") }
                        )
                        Tab(
                            selected = state.tab == LoginTab.Nfc,
                            onClick = { viewModel.updateTab(LoginTab.Nfc) },
                            text = { Text("NFC登录") }
                        )
                    }

                    if (state.tab == LoginTab.Account) {
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
                    } else {
                        OutlinedTextField(
                            value = state.nfcIp,
                            onValueChange = viewModel::updateNfcIp,
                            label = { Text("NFC读卡IP") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.nfcCardNo,
                            onValueChange = viewModel::updateNfcCardNo,
                            label = { Text("卡号 (支持自动回填)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    PrimaryButton(
                        text = "登录",
                        enabled = !state.loading,
                        loading = state.loading,
                        onClick = viewModel::login
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("设备配置", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = viewModel::toggleConfigExpanded) {
                            Text(if (state.configExpanded) "收起" else "展开")
                        }
                    }

                    if (state.configExpanded) {
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
                            label = { Text("柜体IP列表(逗号分隔)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Button(
                            onClick = viewModel::saveConfig,
                            enabled = !state.savingConfig
                        ) {
                            if (state.savingConfig) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("保存配置")
                            }
                        }
                    } else {
                        Text(
                            text = "API 与 MQTT 配置已保存，展开后可编辑。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
