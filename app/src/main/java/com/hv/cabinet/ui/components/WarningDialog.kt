package com.hv.cabinet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hv.cabinet.domain.ScanWarning
import com.hv.cabinet.domain.ScanWarningType

/**
 * Warning overlay rendered in the SAME window as the Activity (not a Dialog window).
 * This ensures Activity.dispatchKeyEvent continues to intercept barcode scanner
 * key events even while warnings are visible.
 */
@Composable
fun WarningDialog(
    warnings: List<ScanWarning>,
    onDismiss: () -> Unit
) {
    if (warnings.isEmpty()) return

    val totalCount = warnings.sumOf { it.count }

    // Full-screen scrim — blocks touches but stays in the Activity window
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {} // swallow clicks on scrim
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0E1E30),
            modifier = Modifier
                .widthIn(min = 320.dp, max = 480.dp)
                .padding(24.dp)
                // prevent scrim click-through
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {}
                )
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Title
                Text(
                    text = "耗材异常提示",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Summary
                Text(
                    text = "本次扫描共发现 $totalCount 个异常项：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFAABBCC),
                    modifier = Modifier.padding(top = 12.dp, bottom = 10.dp)
                )

                // Warning cards
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    warnings.forEach { warning ->
                        val (label, color) = warningStyle(warning.type)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = color.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = color,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = warning.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCCDDEE)
                                )
                            }
                            Text(
                                text = "${warning.count}",
                                style = MaterialTheme.typography.titleMedium,
                                color = color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Confirm button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("确认", color = Color(0xFF52C7EA))
                    }
                }
            }
        }
    }
}

private fun warningStyle(type: ScanWarningType): Pair<String, Color> = when (type) {
    ScanWarningType.AlreadyTaken -> "已在取用流程" to Color(0xFFE5B24A)
    ScanWarningType.AlreadyInStock -> "已在库内" to Color(0xFF55C68E)
    ScanWarningType.NotInStock -> "未入库" to Color(0xFFE16969)
    ScanWarningType.Consumed -> "已消耗" to Color(0xFFE16969)
    ScanWarningType.UsedReturn -> "消耗退回" to Color(0xFFE5B24A)
    ScanWarningType.Unknown -> "未知异常" to Color(0xFF9E9E9E)
}
