package com.hv.cabinet.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hv.cabinet.data.api.LocationOption
import com.hv.cabinet.ui.layout.rememberCabinetWindowSpec

enum class LocationChoiceLayoutMode {
    Auto,
    HorizontalScroll,
    Wrapped
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocationChoiceStrip(
    options: List<LocationOption>,
    selectedId: String,
    loading: Boolean,
    onSelect: (String) -> Unit,
    layoutMode: LocationChoiceLayoutMode = LocationChoiceLayoutMode.Auto,
    modifier: Modifier = Modifier
) {
    val spec = rememberCabinetWindowSpec()
    val resolvedLayout = when (layoutMode) {
        LocationChoiceLayoutMode.Auto -> {
            if (spec.preferWrappedChips && options.size <= 12) {
                LocationChoiceLayoutMode.Wrapped
            } else {
                LocationChoiceLayoutMode.HorizontalScroll
            }
        }
        else -> layoutMode
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (loading) {
            CircularProgressIndicator(strokeWidth = 2.dp)
            return@Column
        }
        if (options.isEmpty()) {
            Text(
                text = "目标位置（选择）：暂无可选地点（仅显示房间，已排除默认地点）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }
        Text(
            text = "目标位置（选择）",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        if (resolvedLayout == LocationChoiceLayoutMode.Wrapped) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { item ->
                    LocationChoiceChip(
                        option = item,
                        selected = item.id == selectedId,
                        onSelect = onSelect
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                items(options, key = { it.id }) { item ->
                    LocationChoiceChip(
                        option = item,
                        selected = item.id == selectedId,
                        onSelect = onSelect
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationChoiceChip(
    option: LocationOption,
    selected: Boolean,
    onSelect: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color(0x223A4F68),
        tonalElevation = if (selected) 4.dp else 0.dp,
        modifier = Modifier.clickable { onSelect(option.id) }
    ) {
        Text(
            text = option.name,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
