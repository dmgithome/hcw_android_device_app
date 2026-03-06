package com.hv.cabinet.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hv.cabinet.ui.theme.CabinetSpacing
import com.hv.cabinet.ui.theme.CabinetTypography
import com.hv.cabinet.ui.theme.GlassBorder
import com.hv.cabinet.ui.theme.GlassBorderStrong
import com.hv.cabinet.ui.theme.PanelBg
import com.hv.cabinet.ui.theme.PanelBgSoft
import com.hv.cabinet.ui.theme.PanelGlassBottom
import com.hv.cabinet.ui.theme.PanelGlassTop
import com.hv.cabinet.ui.theme.PanelInnerLine
import com.hv.cabinet.ui.theme.TextDim
import com.hv.cabinet.ui.theme.TextMain
import com.hv.cabinet.ui.theme.TextSubtle
import kotlin.math.ceil

const val DEFAULT_PAGE_SIZE = 50

@Composable
fun PaginationFooter(
    total: Int,
    currentPage: Int,
    pageSize: Int = DEFAULT_PAGE_SIZE,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPages = maxOf(1, ceil(total.toDouble() / pageSize).toInt())

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(CabinetSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PageIndicatorPill(
            currentPage = currentPage,
            totalPages = totalPages
        )
        PageActionGroup {
            PageButton("首", enabled = currentPage > 1) { onPageChange(1) }
            PageButton("上", enabled = currentPage > 1) { onPageChange(currentPage - 1) }
            PageButton("下", enabled = currentPage < totalPages) { onPageChange(currentPage + 1) }
            PageButton("尾", enabled = currentPage < totalPages) { onPageChange(totalPages) }
        }
    }
}

@Composable
private fun PageIndicatorPill(currentPage: Int, totalPages: Int) {
    PaginationGlassPill(
        backgroundColor = PanelBg.copy(alpha = 0.7f),
        borderColor = GlassBorder.copy(alpha = 0.58f),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = "第 $currentPage / $totalPages 页",
            style = CabinetTypography.labelMedium,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.32.sp,
            color = TextDim.copy(alpha = 0.92f)
        )
    }
}

@Composable
private fun PageActionGroup(content: @Composable () -> Unit) {
    PaginationGlassPill(
        backgroundColor = PanelBg.copy(alpha = 0.64f),
        borderColor = GlassBorder.copy(alpha = 0.52f),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

@Composable
private fun PageButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "pageButtonScale"
    )
    val surfaceAlpha by animateFloatAsState(
        targetValue = when {
            !enabled -> 0.1f
            isPressed -> 0.08f
            else -> 0.035f
        },
        animationSpec = tween(durationMillis = 140),
        label = "pageButtonSurfaceAlpha"
    )
    val textColor = if (enabled) {
        TextSubtle.copy(alpha = 0.82f)
    } else {
        TextDim.copy(alpha = 0.62f)
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = surfaceAlpha))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (enabled) 0.022f else 0.012f),
                        Color.Transparent
                    )
                )
            )
            .border(
                width = CabinetSpacing.borderThin,
                color = if (enabled) GlassBorder.copy(alpha = 0.26f) else GlassBorder.copy(alpha = 0.14f),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .defaultMinSize(minWidth = 36.dp, minHeight = 28.dp)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = CabinetTypography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
private fun PaginationGlassPill(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    borderColor: Color,
    contentPadding: PaddingValues,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = CabinetSpacing.borderThin,
                color = borderColor,
                shape = shape
            )
            .heightIn(min = 36.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            PanelGlassTop.copy(alpha = 0.4f),
                            Color.Transparent,
                            PanelGlassBottom.copy(alpha = 0.32f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(1.dp)
                .border(
                    width = CabinetSpacing.borderThin,
                    color = PanelInnerLine.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(999.dp)
                )
        )
        Box(
            modifier = Modifier
                .padding(contentPadding),
            content = content
        )
    }
}
