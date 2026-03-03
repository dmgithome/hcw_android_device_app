package com.hv.cabinet.feature.returning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hv.cabinet.core.PerfMonitor
import com.hv.cabinet.domain.InventoryFlowState
import com.hv.cabinet.ui.components.AppScaffold
import com.hv.cabinet.ui.components.AppScaffoldVariant
import com.hv.cabinet.ui.components.ConfirmButton
import com.hv.cabinet.ui.components.ConsumableDataTable
import com.hv.cabinet.ui.components.DEFAULT_PAGE_SIZE
import com.hv.cabinet.ui.components.OutlinedConfirmButton
import com.hv.cabinet.ui.components.PaginationFooter
import com.hv.cabinet.ui.components.PrimaryButton
import com.hv.cabinet.ui.components.ScanKeyboardHandler
import com.hv.cabinet.ui.components.TableVariant
import com.hv.cabinet.ui.components.WarningDialog
import kotlin.math.ceil

@Composable
fun ReturnScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    active: Boolean = true,
    viewModel: ReturnViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(active) {
        if (active) {
            PerfMonitor.mark("return_screen_interactive")
            PerfMonitor.measure(
                start = "home_card_return_pointerdown",
                end = "return_screen_interactive",
                label = "home_return_to_interactive"
            )
        } else {
            viewModel.onScreenLeave()
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.onScreenLeave() }
    }

    // Snackbar for messages
    LaunchedEffect(active, state.message) {
        if (active && state.message.visible) {
            snackbarHostState.showSnackbar(state.message.text)
        }
    }

    // Warning overlay state
    var showWarnings by remember { mutableStateOf(false) }
    LaunchedEffect(active, state.warningsNonce) {
        if (active && state.warnings.isNotEmpty() && state.flowState == InventoryFlowState.Idle) {
            showWarnings = true
        }
    }

    val userInfoText = "${state.userName} - ${state.userRole}".let {
        if (it == " - ") "" else it
    }

    // Pagination state — managed here, no callback loop
    var currentPage by remember { mutableIntStateOf(1) }
    val total = state.items.size
    val totalPages = maxOf(1, ceil(total.toDouble() / DEFAULT_PAGE_SIZE).toInt())
    val clampedPage = currentPage.coerceIn(1, totalPages)
    if (clampedPage != currentPage) currentPage = clampedPage

    val pageItems = remember(state.items, clampedPage) {
        val start = (clampedPage - 1) * DEFAULT_PAGE_SIZE
        state.items.subList(start, minOf(start + DEFAULT_PAGE_SIZE, total))
    }

    // Root Box: AppScaffold + warning overlay in same Activity window
    Box(modifier = Modifier.fillMaxSize()) {
        AppScaffold(
            title = "耗材屋",
            onBack = onBack,
            variant = AppScaffoldVariant.Business,
            lightweight = true,
            snackbarHostState = snackbarHostState,
            actions = {
                if (userInfoText.isNotBlank()) {
                    Text(
                        text = userInfoText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Title + Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "读码归还",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton(
                            text = if (state.isInventoryBusy) "盘点进行中" else "开始盘点",
                            enabled = state.canStart,
                            loading = state.awaitingStartAck || state.flowState == InventoryFlowState.Starting,
                            onClick = viewModel::startInventory
                        )
                        ConfirmButton(
                            text = "确认归还",
                            enabled = state.canSubmit,
                            loading = state.isSubmitting,
                            onClick = viewModel::submit
                        )
                        OutlinedConfirmButton(
                            text = "确认归还并登出",
                            enabled = state.canSubmit,
                            onClick = { viewModel.submitAndLogout(onLogout) }
                        )
                    }
                }

                // Data table (fills remaining space)
                if ((state.flowState == InventoryFlowState.Starting || state.awaitingStartAck) && state.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                            Text(
                                text = "正在启动盘点...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    ConsumableDataTable(
                        items = pageItems,
                        conflictRfids = state.conflictRfids,
                        onRemove = viewModel::removeItem,
                        variant = TableVariant.Return,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                // Pagination footer
                PaginationFooter(
                    total = total,
                    currentPage = clampedPage,
                    onPageChange = { currentPage = it }
                )

                // Invisible barcode scanner handler
                if (active) {
                    ScanKeyboardHandler(
                        onScanned = { code ->
                            viewModel.updateBarcode(code)
                            viewModel.addBarcode()
                        }
                    )
                }
            }
        }

        // Warning overlay — same window, no Dialog, key events stay with Activity
        if (active && showWarnings) {
            WarningDialog(
                warnings = state.warnings,
                onDismiss = { showWarnings = false }
            )
        }
    }
}
