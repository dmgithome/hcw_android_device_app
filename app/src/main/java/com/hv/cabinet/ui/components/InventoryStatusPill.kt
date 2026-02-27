package com.hv.cabinet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hv.cabinet.domain.InventoryFlowState

@Composable
fun InventoryStatusPill(
    state: InventoryFlowState,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (state) {
        InventoryFlowState.Inventorying -> Color(0x2655C68E) to Color(0xFF8BF0BA)
        InventoryFlowState.WaitingAck,
        InventoryFlowState.Starting -> Color(0x2652C7EA) to Color(0xFF8EDCFF)
        InventoryFlowState.Error -> Color(0x33E16969) to Color(0xFFFFA7A7)
        InventoryFlowState.Completed -> Color(0x26E5B24A) to Color(0xFFFFD88C)
        else -> Color(0x221B4E88) to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "状态：${state.label()}",
            style = MaterialTheme.typography.labelLarge,
            color = fg
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

