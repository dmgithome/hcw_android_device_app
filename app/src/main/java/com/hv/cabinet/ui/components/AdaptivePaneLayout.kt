package com.hv.cabinet.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AdaptivePaneLayout(
    modifier: Modifier = Modifier,
    twoPane: Boolean,
    paneSpacing: Dp = 12.dp,
    leftWeight: Float = 1f,
    rightWeight: Float = 1f,
    singlePaneMaxWidth: Dp = Dp.Unspecified,
    leftPane: @Composable (Modifier) -> Unit,
    rightPane: @Composable (Modifier) -> Unit
) {
    if (twoPane) {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(paneSpacing)
        ) {
            leftPane(Modifier.weight(leftWeight).fillMaxSize())
            rightPane(Modifier.weight(rightWeight).fillMaxSize())
        }
        return
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = singlePaneMaxWidth),
            verticalArrangement = Arrangement.spacedBy(paneSpacing)
        ) {
            leftPane(Modifier.fillMaxWidth())
            rightPane(Modifier.fillMaxWidth())
        }
    }
}
