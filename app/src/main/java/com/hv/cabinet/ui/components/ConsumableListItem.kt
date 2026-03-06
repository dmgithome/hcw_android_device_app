package com.hv.cabinet.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hv.cabinet.domain.ConsumableUiModel
import com.hv.cabinet.ui.layout.rememberCabinetWindowSpec

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConsumableListItem(
    item: ConsumableUiModel,
    onRemove: (String) -> Unit,
    conflictHighlighted: Boolean = false
) {
    val spec = rememberCabinetWindowSpec()
    TransparentPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        startColor = if (conflictHighlighted) Color(0x22251216) else Color(0x1A1D3550),
        endColor = if (conflictHighlighted) Color(0x16110809) else Color(0x12091320),
        borderColor = if (conflictHighlighted) Color(0x99E16969) else Color(0x4452C7EA),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(if (spec.isWideLandscape) 12.dp else 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.name.ifBlank { "未命名耗材" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                MetaChipRow(
                    values = buildList {
                        add("编码 ${item.code.ifBlank { "-" }}")
                        if (item.payload.spec.isNotBlank()) add("规格 ${item.payload.spec}")
                        else if (item.payload.brand.isNotBlank()) add("品牌 ${item.payload.brand}")
                        add("柜体 ${item.cabinetId}")
                    }
                )

                Text(
                    text = "RFID: ${item.rfid}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (conflictHighlighted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (conflictHighlighted) {
                    Text(
                        text = "提交冲突：位置已变化，请刷新后重试",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = "当前位置: ${item.currentLocationName.ifBlank { item.currentLocationId.ifBlank { "-" } }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (item.payload.from_location_name.isNotBlank() || item.payload.from_location_id.isNotBlank()) {
                    Text(
                        text = "来源位置: ${item.payload.from_location_name.ifBlank { item.payload.from_location_id }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { onRemove(item.rfid) }) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "移除",
                    tint = if (conflictHighlighted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetaChipRow(values: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        values.filter { it.isNotBlank() }.take(3).forEach { value ->
            Surface(
                color = Color(0x22354963),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
