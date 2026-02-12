package com.hv.cabinet.feature.returning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hv.cabinet.core.PerfMonitor
import com.hv.cabinet.domain.InventoryFlowState
import com.hv.cabinet.ui.components.AppScaffold
import com.hv.cabinet.ui.components.ConsumableListItem
import com.hv.cabinet.ui.components.EmptyState
import com.hv.cabinet.ui.components.FooterActionRow
import com.hv.cabinet.ui.components.PrimaryButton
import com.hv.cabinet.ui.components.StatusBanner

@Composable
fun ReturnScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ReturnViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        PerfMonitor.mark("return_screen_interactive")
        PerfMonitor.measure(
            start = "home_card_return_pointerdown",
            end = "return_screen_interactive",
            label = "home_return_to_interactive"
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onScreenLeave()
        }
    }

    AppScaffold(
        title = "耗材归还",
        subtitle = "状态：${state.flowState.label()}",
        onBack = onBack
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatusBanner(message = state.message)

            if (state.flowState == InventoryFlowState.Error) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = viewModel::retryStartInventory) {
                        Text("盘点启动失败，点击重试")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.barcode,
                    onValueChange = viewModel::updateBarcode,
                    label = { Text("扫码/条码") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                PrimaryButton(
                    text = "添加",
                    enabled = true,
                    onClick = viewModel::addBarcode
                )
            }

            FooterActionRow(
                primaryText = if (state.isInventoryBusy) "盘点中" else "开始盘点",
                onPrimaryClick = viewModel::startInventory,
                primaryEnabled = state.canStart,
                secondaryText = "确认归还",
                onSecondaryClick = viewModel::submit,
                secondaryEnabled = state.canSubmit,
                dangerText = "确认并登出",
                onDangerClick = { viewModel.submitAndLogout(onLogout) },
                dangerEnabled = state.canSubmit
            )

            if (state.warnings.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("识别异常", style = MaterialTheme.typography.titleSmall)
                        state.warnings.forEach { warning ->
                            Text(
                                text = "• ${warning.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            if (state.items.isEmpty()) {
                EmptyState(
                    title = "暂无归还项",
                    subtitle = "请点击“开始盘点”或手动输入条码后添加"
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.items, key = { it.rfid }) { item ->
                        ConsumableListItem(
                            item = item,
                            onRemove = viewModel::removeItem
                        )
                    }
                }
            }
        }
    }
}

private fun InventoryFlowState.label(): String = when (this) {
    InventoryFlowState.Idle -> "待命"
    InventoryFlowState.Starting -> "启动中"
    InventoryFlowState.WaitingAck -> "等待响应"
    InventoryFlowState.Inventorying -> "盘点中"
    InventoryFlowState.Submitting -> "提交中"
    InventoryFlowState.Completed -> "已完成"
    InventoryFlowState.Error -> "异常"
}
