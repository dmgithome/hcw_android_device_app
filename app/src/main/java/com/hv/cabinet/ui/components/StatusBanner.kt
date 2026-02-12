package com.hv.cabinet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hv.cabinet.domain.MessageLevel
import com.hv.cabinet.domain.UiMessage

@Composable
fun StatusBanner(message: UiMessage, modifier: Modifier = Modifier) {
    if (!message.visible) return

    val background = when (message.level) {
        MessageLevel.Info -> MaterialTheme.colorScheme.surfaceVariant
        MessageLevel.Success -> MaterialTheme.colorScheme.secondaryContainer
        MessageLevel.Warning -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
        MessageLevel.Error -> MaterialTheme.colorScheme.error.copy(alpha = 0.22f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
