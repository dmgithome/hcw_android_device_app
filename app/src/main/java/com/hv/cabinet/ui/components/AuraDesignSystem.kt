package com.hv.cabinet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hv.cabinet.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

@Composable
private fun rememberClockText(): String {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(1_000)
        }
    }
    return now.format(TimeFormatter)
}

/**
 * ADS (Aura Design System) 核心容器
 */
@Composable
fun AuraSurface(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(CabinetSpacing.panelPadding),
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(CabinetSpacing.cardRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(PanelBg, PanelBgSoft)
                )
            )
            .border(
                width = CabinetSpacing.borderThin,
                color = GlassBorder,
                shape = shape
            )
            .padding(contentPadding),
        content = content
    )
}

/**
 * ADS 按钮样式
 */
enum class AuraButtonStyle {
    Normal, Primary, Highlight, Danger
}

/**
 * ADS 核心按钮
 */
@Composable
fun AuraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AuraButtonStyle = AuraButtonStyle.Normal,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val (startColor, endColor) = when (style) {
        AuraButtonStyle.Primary -> PrimaryStart to PrimaryEnd
        AuraButtonStyle.Highlight -> WarnStart to WarnEnd
        AuraButtonStyle.Danger -> DangerStart to DangerEnd
        AuraButtonStyle.Normal -> BtnBaseStart to BtnBaseEnd
    }

    val shape = RoundedCornerShape(CabinetSpacing.buttonRadius)

    Box(
        modifier = modifier
            .graphicsLayer {
                val scale = if (isPressed && enabled) 0.992f else 1f
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(colors = listOf(startColor, endColor)),
                alpha = if (enabled) 1f else 0.42f
            )
            .border(
                width = CabinetSpacing.borderThin,
                color = Color.White.copy(alpha = if (enabled) 0.2f else 0.08f),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = if (enabled) 1f else 0.55f),
            style = CabinetTypography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )
    }
}

/**
 * ADS 精密文字标签 (术间选择)
 */
@Composable
fun AuraLocationChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(CabinetSpacing.chipRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isSelected) ChipSelectedBg else Color.Transparent)
            .border(
                width = CabinetSpacing.borderThin,
                color = if (isSelected) PrimaryStart.copy(alpha = 0.7f) else GlassBorder,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = CabinetSpacing.lg, vertical = CabinetSpacing.sm + CabinetSpacing.xs),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) PrimaryStart else TextDim,
            style = CabinetTypography.labelMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

/**
 * ADS 统一确认弹窗
 */
@Composable
fun AuraConfirmModal(
    title: String,
    description: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.64f))
            .padding(CabinetSpacing.massive),
        contentAlignment = Alignment.Center
    ) {
        AuraSurface(modifier = Modifier.widthIn(max = CabinetSpacing.confirmModalMaxWidth)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CabinetSpacing.md)
            ) {
                Text(
                    text = title,
                    color = TextMain,
                    style = CabinetTypography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = description,
                    color = TextDim,
                    style = CabinetTypography.bodyMedium,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(CabinetSpacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CabinetSpacing.md)
                ) {
                    AuraButton(
                        text = "返回",
                        modifier = Modifier
                            .weight(1f)
                            .height(CabinetSpacing.modalButtonHeight),
                        onClick = onCancel
                    )
                    AuraButton(
                        text = "确认提交",
                        style = AuraButtonStyle.Primary,
                        modifier = Modifier
                            .weight(1f)
                            .height(CabinetSpacing.modalButtonHeight),
                        onClick = onConfirm
                    )
                }
            }
        }
    }
}

@Composable
private fun AuraClockBadge(timeText: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.09f))
            .border(
                width = CabinetSpacing.borderThin,
                color = GlassBorder,
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = CabinetSpacing.md, vertical = CabinetSpacing.sm)
    ) {
        Text(
            text = timeText,
            color = PrimaryStart,
            style = CabinetTypography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun AuraBackIconButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(
                width = CabinetSpacing.borderThin,
                color = GlassBorder,
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "←",
            color = TextMain,
            style = CabinetTypography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AuraMqttStatusBadge(
    mqttStatusText: String,
    mqttConnected: Boolean?
) {
    val (bgColor, borderColor, textColor) = when (mqttConnected) {
        true -> Triple(AccentMint.copy(alpha = 0.14f), AccentMint.copy(alpha = 0.42f), AccentMint)
        false -> Triple(DangerStart.copy(alpha = 0.14f), DangerStart.copy(alpha = 0.42f), DangerStart)
        null -> Triple(PrimaryStart.copy(alpha = 0.14f), PrimaryStart.copy(alpha = 0.42f), PrimaryStart)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .border(
                width = CabinetSpacing.borderThin,
                color = borderColor,
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = CabinetSpacing.md, vertical = CabinetSpacing.sm)
    ) {
        Text(
            text = mqttStatusText,
            color = textColor,
            style = CabinetTypography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * ADS 页面脚手架
 */
@Composable
fun AuraScreen(
    title: String,
    subtitle: String? = null,
    operatorInfo: String? = null,
    mqttStatusText: String? = null,
    mqttConnected: Boolean? = null,
    topRightAction: (@Composable RowScope.() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val timeText = rememberClockText()
    val topBarShape = RoundedCornerShape(CabinetSpacing.cardRadius)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .background(
                brush = Brush.radialGradient(
                    0f to BgAuraCore.copy(alpha = 0.9f),
                    1f to BgDark,
                    radius = 1_100f,
                    center = androidx.compose.ui.geometry.Offset(220f, 80f)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(CabinetSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(CabinetSpacing.md)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(topBarShape)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(PanelBg, PanelBgSoft)
                        )
                    )
                    .border(
                        width = CabinetSpacing.borderThin,
                        color = GlassBorder,
                        shape = topBarShape
                    )
                    .padding(horizontal = CabinetSpacing.lg, vertical = CabinetSpacing.sm)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CabinetSpacing.topBarHeight),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CabinetSpacing.md)
                    ) {
                        if (onCancel != null) {
                            AuraBackIconButton(onClick = onCancel)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(CabinetSpacing.xs)) {
                            Text(
                                text = title,
                                color = TextMain,
                                style = CabinetTypography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    color = PrimaryStart.copy(alpha = 0.92f),
                                    style = CabinetTypography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CabinetSpacing.md)
                    ) {
                        if (operatorInfo != null) {
                            Text(
                                text = operatorInfo,
                                color = TextMain,
                                style = CabinetTypography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!mqttStatusText.isNullOrBlank()) {
                            AuraMqttStatusBadge(
                                mqttStatusText = mqttStatusText,
                                mqttConnected = mqttConnected
                            )
                        }
                        AuraClockBadge(timeText)
                        if (topRightAction != null) {
                            topRightAction()
                        }
                    }
                }
            }

            content()
        }
    }
}
