package com.hv.cabinet.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hv.cabinet.domain.InventoryFlowState
import com.hv.cabinet.ui.theme.AccentMint
import com.hv.cabinet.ui.theme.CabinetSpacing
import com.hv.cabinet.ui.theme.CabinetTypography
import com.hv.cabinet.ui.theme.DangerStart
import com.hv.cabinet.ui.theme.GlassBorder
import com.hv.cabinet.ui.theme.HighlightStart
import com.hv.cabinet.ui.theme.PanelBgSoft
import com.hv.cabinet.ui.theme.PrimaryStart
import com.hv.cabinet.ui.theme.TextSubtle

@Composable
fun InventoryStatusPill(
    state: InventoryFlowState,
    modifier: Modifier = Modifier
) {
    val (bg, border, fg) = when (state) {
        InventoryFlowState.Inventorying -> Triple(AccentMint.copy(alpha = 0.10f), AccentMint.copy(alpha = 0.18f), AccentMint)
        InventoryFlowState.WaitingAck,
        InventoryFlowState.Starting -> Triple(HighlightStart.copy(alpha = 0.08f), HighlightStart.copy(alpha = 0.18f), HighlightStart)
        InventoryFlowState.Error -> Triple(DangerStart.copy(alpha = 0.10f), DangerStart.copy(alpha = 0.18f), DangerStart)
        InventoryFlowState.Completed -> Triple(PrimaryStart.copy(alpha = 0.10f), PrimaryStart.copy(alpha = 0.18f), PrimaryStart)
        else -> Triple(PanelBgSoft.copy(alpha = 0.86f), GlassBorder.copy(alpha = 0.9f), TextSubtle)
    }
    val animatedBg by animateColorAsState(
        targetValue = bg,
        animationSpec = tween(durationMillis = 220),
        label = "inventoryStatusBackground"
    )
    val animatedBorder by animateColorAsState(
        targetValue = border,
        animationSpec = tween(durationMillis = 220),
        label = "inventoryStatusBorder"
    )
    val animatedFg by animateColorAsState(
        targetValue = fg,
        animationSpec = tween(durationMillis = 220),
        label = "inventoryStatusForeground"
    )

    Row(
        modifier = modifier
            .background(animatedBg, RoundedCornerShape(999.dp))
            .border(CabinetSpacing.borderThin, animatedBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(animatedFg, RoundedCornerShape(999.dp))
                .size(6.dp)
        )
        Text(
            text = "状态：${state.label()}",
            style = CabinetTypography.bodySmall,
            color = animatedFg,
            fontWeight = FontWeight.Medium
        )
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
