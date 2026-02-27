package com.hv.cabinet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hv.cabinet.domain.MessageLevel
import com.hv.cabinet.domain.UiMessage

@Composable
fun StatusBanner(message: UiMessage, modifier: Modifier = Modifier) {
    if (!message.visible) return

    val palette = when (message.level) {
        MessageLevel.Info -> BannerPalette(
            start = Color(0x223A5D86),
            end = Color(0x18102037),
            border = Color(0x6652C7EA),
            accent = Color(0xFF52C7EA)
        )
        MessageLevel.Success -> BannerPalette(
            start = Color(0x2235865B),
            end = Color(0x1811221A),
            border = Color(0x6655C68E),
            accent = Color(0xFF55C68E)
        )
        MessageLevel.Warning -> BannerPalette(
            start = Color(0x223B2A12),
            end = Color(0x181E1408),
            border = Color(0x66E5B24A),
            accent = Color(0xFFE5B24A)
        )
        MessageLevel.Error -> BannerPalette(
            start = Color(0x223B181B),
            end = Color(0x181A0C0E),
            border = Color(0x66E16969),
            accent = Color(0xFFE16969)
        )
    }

    val icon = when (message.level) {
        MessageLevel.Info -> Icons.Outlined.Info
        MessageLevel.Success -> Icons.Outlined.CheckCircle
        MessageLevel.Warning -> Icons.Outlined.Warning
        MessageLevel.Error -> Icons.Outlined.Error
    }

    TransparentPanel(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        startColor = palette.start,
        endColor = palette.end,
        borderColor = palette.border,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(
                modifier = Modifier
                    .width(4.dp)
                    .height(38.dp)
                    .background(palette.accent, RoundedCornerShape(999.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private data class BannerPalette(
    val start: Color,
    val end: Color,
    val border: Color,
    val accent: Color
)
