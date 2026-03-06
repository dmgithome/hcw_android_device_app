package com.hv.cabinet.feature.returning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hv.cabinet.domain.InventoryFlowState
import com.hv.cabinet.domain.InventoryMode
import com.hv.cabinet.domain.ScanWarning
import com.hv.cabinet.domain.ScanWarningType
import com.hv.cabinet.ui.components.*
import com.hv.cabinet.ui.theme.*
import kotlin.math.ceil

@Composable
fun ReturnScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    active: Boolean = true,
    viewModel: ReturnViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    var showConfirmModal by remember { mutableStateOf(false) }
    var showWarningModal by remember { mutableStateOf(false) }
    var warningPayload by remember { mutableStateOf<List<ScanWarning>>(emptyList()) }
    var inventorySessionActive by remember { mutableStateOf(false) }
    val warningBuffer = remember { mutableStateMapOf<ScanWarningType, Int>() }

    LaunchedEffect(active) {
        viewModel.onScreenActiveChanged(active)
        if (!active) {
            viewModel.onScreenLeave()
            showConfirmModal = false
            showWarningModal = false
            warningPayload = emptyList()
            inventorySessionActive = false
            warningBuffer.clear()
        }
    }

    LaunchedEffect(state.flowState, state.isInventoryBusy) {
        val inInventorySession = state.isInventoryBusy ||
            state.flowState == InventoryFlowState.Starting ||
            state.flowState == InventoryFlowState.WaitingAck ||
            state.flowState == InventoryFlowState.Inventorying

        if (inInventorySession && !inventorySessionActive) {
            inventorySessionActive = true
            warningBuffer.clear()
            showWarningModal = false
            warningPayload = emptyList()
        }

        if (inventorySessionActive && !inInventorySession) {
            warningPayload = warningBuffer.entries
                .sortedBy { it.key.ordinal }
                .map { (type, count) -> ScanWarning(type = type, count = count, message = "") }
            showConfirmModal = false
            showWarningModal = warningPayload.isNotEmpty()
            warningBuffer.clear()
            inventorySessionActive = false
        }
    }

    LaunchedEffect(state.warningsNonce) {
        if (state.warnings.isEmpty()) return@LaunchedEffect
        if (inventorySessionActive) {
            state.warnings.forEach { warning ->
                warningBuffer[warning.type] = (warningBuffer[warning.type] ?: 0) + warning.count
            }
        } else {
            warningPayload = state.warnings
            showWarningModal = true
        }
    }

    val total = state.items.size
    val totalPages = maxOf(1, ceil(total.toDouble() / DEFAULT_PAGE_SIZE).toInt())
    var currentPage by remember { mutableIntStateOf(1) }
    val clampedPage = currentPage.coerceIn(1, totalPages)
    
    val pageItems = remember(state.items, clampedPage) {
        val start = (clampedPage - 1) * DEFAULT_PAGE_SIZE
        state.items.subList(start, minOf(start + DEFAULT_PAGE_SIZE, total))
    }

    AuraScreen(
        title = "归还",
        operatorInfo = "${state.userName} - ${state.userRole}",
        mqttStatusText = state.mqttStatusText,
        mqttConnected = state.mqttConnected,
        onCancel = onBack
    ) {
        AuraEntrance(
            modifier = Modifier.fillMaxWidth(),
            entranceKey = active,
            delayMillis = 40,
            initialOffsetY = 16f,
            initialScale = 0.992f
        ) {
            AuraSurface(
                modifier = Modifier.fillMaxWidth(),
                variant = AuraSurfaceVariant.CoolGlow
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(CabinetSpacing.lg)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CabinetSpacing.actionGap)
                    ) {
                        AuraButton(
                            text = if (state.isInventoryBusy) "盘点中..." else "开始盘点扫描",
                            style = AuraButtonStyle.Normal,
                            modifier = Modifier.weight(1f).height(CabinetSpacing.actionButtonHeight),
                            onClick = viewModel::startInventory
                        )
                        AuraButton(
                            text = "确认提交归还",
                            style = AuraButtonStyle.Primary,
                            enabled = state.canSubmit,
                            modifier = Modifier.weight(1f).height(CabinetSpacing.actionButtonHeight),
                            onClick = { showConfirmModal = true }
                        )
                        AuraButton(
                            text = "提交并退出系统",
                            style = AuraButtonStyle.Highlight,
                            enabled = state.canSubmit,
                            modifier = Modifier.weight(1f).height(CabinetSpacing.actionButtonHeight),
                            onClick = { viewModel.submitAndLogout(onLogout) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InventoryStatusPill(state = state.flowState)
                        Text(
                            text = "归还位置由系统自动推断",
                            color = TextSubtle,
                            style = CabinetTypography.bodySmall
                        )
                    }
                }
            }
        }

        AuraEntrance(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            entranceKey = "return-table-$active",
            delayMillis = 110,
            initialOffsetY = 22f,
            initialScale = 0.988f
        ) {
            AuraSurface(
                modifier = Modifier.fillMaxSize(),
                variant = AuraSurfaceVariant.CoolGlow,
                contentPadding = PaddingValues()
            ) {
                Column {
                    ConsumableDataTable(
                        items = pageItems,
                        conflictRfids = state.conflictRfids,
                        onRemove = viewModel::removeItem,
                        variant = TableVariant.Return,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(CabinetSpacing.footerBarHeight)
                            .background(FooterSurface)
                            .padding(horizontal = CabinetSpacing.lg),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "共计识别到 ${total} 项归还耗材",
                            color = TextSubtle,
                            style = CabinetTypography.bodySmall
                        )
                        PaginationFooter(
                            total = total,
                            currentPage = clampedPage,
                            onPageChange = { currentPage = it }
                        )
                    }
                }
            }
        }
    }

    if (showWarningModal) {
        WarningDialog(
            warnings = warningPayload,
            mode = InventoryMode.RETURN,
            onDismiss = {
                showWarningModal = false
                warningPayload = emptyList()
            }
        )
    }

    if (active) {
        ScanKeyboardHandler(
            onScanned = { code ->
                viewModel.updateBarcode(code)
                viewModel.addBarcode()
            }
        )
    }

    if (showConfirmModal) {
        AuraConfirmModal(
            title = "归还确认",
            description = "即将提交 ${total} 项耗材的归还记录。\n系统将自动更新原始订单状态。",
            onConfirm = {
                showConfirmModal = false
                viewModel.submit()
            },
            onCancel = { showConfirmModal = false }
        )
    }
}
