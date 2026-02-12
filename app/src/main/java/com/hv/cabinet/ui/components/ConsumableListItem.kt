package com.hv.cabinet.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hv.cabinet.domain.ConsumableUiModel

@Composable
fun ConsumableListItem(
    item: ConsumableUiModel,
    onRemove: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.name.ifBlank { "未命名耗材" },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "编码: ${item.code.ifBlank { "-" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "RFID: ${item.rfid}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "柜体: ${item.cabinetId}  位置: ${item.currentLocationName.ifBlank { item.currentLocationId.ifBlank { "-" } }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { onRemove(item.rfid) }) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "移除"
                )
            }
        }
    }
}
