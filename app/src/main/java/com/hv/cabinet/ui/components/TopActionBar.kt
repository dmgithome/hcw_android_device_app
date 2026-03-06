package com.hv.cabinet.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hv.cabinet.ui.layout.CabinetButtonLayoutMode
import com.hv.cabinet.ui.layout.rememberCabinetWindowSpec

enum class TopActionBarButtonLayoutMode {
    Auto,
    SingleRow,
    SplitRows
}

@Composable
fun TopActionBar(
    title: String,
    subtitle: String? = null,
    primaryText: String,
    onPrimaryClick: () -> Unit,
    primaryEnabled: Boolean,
    primaryLoading: Boolean = false,
    primaryDisabledReason: String = "",
    secondaryText: String,
    onSecondaryClick: () -> Unit,
    secondaryEnabled: Boolean,
    secondaryDisabledReason: String = "",
    dangerText: String,
    onDangerClick: () -> Unit,
    dangerEnabled: Boolean,
    buttonLayoutMode: TopActionBarButtonLayoutMode = TopActionBarButtonLayoutMode.Auto,
    modifier: Modifier = Modifier
) {
    val spec = rememberCabinetWindowSpec()
    val resolvedLayout = when (buttonLayoutMode) {
        TopActionBarButtonLayoutMode.Auto -> when (spec.topActionBarButtonLayout) {
            CabinetButtonLayoutMode.SingleRow -> TopActionBarButtonLayoutMode.SingleRow
            CabinetButtonLayoutMode.SplitRows -> TopActionBarButtonLayoutMode.SplitRows
        }
        else -> buttonLayoutMode
    }

    TransparentPanel(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = if (spec.isWideLandscape) 14.dp else 12.dp,
            vertical = if (spec.isWideLandscape) 14.dp else 12.dp
        )
    ) {
        Column(
            modifier = Modifier.animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(spec.panelSpacing.coerceAtMost(12.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (resolvedLayout == TopActionBarButtonLayoutMode.SingleRow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrimaryButton(
                        text = primaryText,
                        enabled = primaryEnabled,
                        loading = primaryLoading,
                        modifier = Modifier.weight(1f),
                        onClick = onPrimaryClick
                    )
                    SecondaryActionButton(
                        text = secondaryText,
                        enabled = secondaryEnabled,
                        modifier = Modifier.weight(1f),
                        onClick = onSecondaryClick
                    )
                    DangerActionButton(
                        text = dangerText,
                        enabled = dangerEnabled,
                        modifier = Modifier.weight(1f),
                        onClick = onDangerClick
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrimaryButton(
                        text = primaryText,
                        enabled = primaryEnabled,
                        loading = primaryLoading,
                        modifier = Modifier.weight(1f),
                        onClick = onPrimaryClick
                    )
                    SecondaryActionButton(
                        text = secondaryText,
                        enabled = secondaryEnabled,
                        modifier = Modifier.weight(1f),
                        onClick = onSecondaryClick
                    )
                }
                DangerActionButton(
                    text = dangerText,
                    enabled = dangerEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDangerClick
                )
            }
            val reasons = listOf(primaryDisabledReason, secondaryDisabledReason)
                .map { it.trim() }
                .filter { it.isNotBlank() }
            if (reasons.isNotEmpty()) {
                Text(
                    text = reasons.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
