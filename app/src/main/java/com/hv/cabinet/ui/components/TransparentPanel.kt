package com.hv.cabinet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.hv.cabinet.ui.theme.CabinetSpacing
import com.hv.cabinet.ui.theme.LegacyPanelBorder
import com.hv.cabinet.ui.theme.LegacyPanelFill

@Composable
fun TransparentPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(CabinetSpacing.cardRadius),
    startColor: Color = LegacyPanelFill,
    endColor: Color = LegacyPanelFill,
    borderColor: Color = LegacyPanelBorder,
    contentPadding: PaddingValues = PaddingValues(CabinetSpacing.sectionGap),
    content: @Composable () -> Unit
) {
    // 性能优先：避免每个面板都使用渐变画刷，降低 GPU 绘制压力。
    val fillColor = if (endColor.alpha > startColor.alpha) endColor else startColor
    Box(
        modifier = modifier
            .background(
                color = fillColor,
                shape = shape
            )
            .border(
                BorderStroke(1.dp, borderColor),
                shape = shape
            )
            .padding(contentPadding)
    ) {
        content()
    }
}
