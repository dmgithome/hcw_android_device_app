package com.hv.cabinet.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.shadow
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
enum class AuraSurfaceVariant {
    Neutral, SoftGlow, CoolGlow
}

@Composable
fun AuraSurface(
    modifier: Modifier = Modifier,
    variant: AuraSurfaceVariant = AuraSurfaceVariant.Neutral,
    contentPadding: PaddingValues = PaddingValues(CabinetSpacing.panelPadding),
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(CabinetSpacing.cardRadius)
    val glowColor = when (variant) {
        AuraSurfaceVariant.CoolGlow -> CoolWhiteGlow
        AuraSurfaceVariant.SoftGlow -> SoftWhiteGlow.copy(alpha = 0.78f)
        AuraSurfaceVariant.Neutral -> SoftWhiteGlow
    }
    val borderColor = when (variant) {
        AuraSurfaceVariant.CoolGlow -> HighlightStart.copy(alpha = 0.16f)
        AuraSurfaceVariant.SoftGlow -> GlassBorderStrong
        AuraSurfaceVariant.Neutral -> GlassBorder
    }
    val tintAlpha = when (variant) {
        AuraSurfaceVariant.CoolGlow -> 0.05f
        AuraSurfaceVariant.SoftGlow -> 0.04f
        AuraSurfaceVariant.Neutral -> 0.03f
    }
    val glowHeight = if (variant == AuraSurfaceVariant.SoftGlow) 40.dp else 52.dp
    val glowAlpha = when (variant) {
        AuraSurfaceVariant.CoolGlow -> 0.82f
        AuraSurfaceVariant.SoftGlow -> 0.66f
        AuraSurfaceVariant.Neutral -> 0.88f
    }
    Box(
        modifier = modifier
            .shadow(
                elevation = 20.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = 0.28f)
            )
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(PanelBg, PanelBgSoft)
                )
            )
            .border(
                width = CabinetSpacing.borderThin,
                color = borderColor,
                shape = shape
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            PanelGlassTop,
                            Color.White.copy(alpha = 0.07f),
                            Color.Transparent,
                            PanelGlassBottom
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            when (variant) {
                                AuraSurfaceVariant.CoolGlow -> HighlightStart.copy(alpha = tintAlpha)
                                AuraSurfaceVariant.SoftGlow -> PrimaryStart.copy(alpha = tintAlpha)
                                AuraSurfaceVariant.Neutral -> Color.White.copy(alpha = tintAlpha)
                            },
                            Color.Transparent,
                            Color.White.copy(alpha = 0.012f)
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
                    color = PanelInnerLine,
                    shape = RoundedCornerShape(CabinetSpacing.cardRadius - 1.dp)
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.42f)
                .height(glowHeight)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.42f * glowAlpha),
                            glowColor.copy(alpha = 0.16f * glowAlpha),
                            Color.White.copy(alpha = 0.04f * glowAlpha),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .padding(contentPadding),
            content = content
        )
    }
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

    val backgroundBrush = when (style) {
        AuraButtonStyle.Primary -> Brush.verticalGradient(listOf(PrimaryStart, PrimaryEnd))
        AuraButtonStyle.Highlight -> Brush.verticalGradient(listOf(HighlightStart, HighlightEnd))
        AuraButtonStyle.Danger -> Brush.verticalGradient(listOf(DangerStart, DangerEnd))
        AuraButtonStyle.Normal -> Brush.verticalGradient(
            listOf(
                BtnBaseStart.copy(alpha = 0.92f),
                BtnBaseEnd.copy(alpha = 0.84f)
            )
        )
    }
    val textColor = when (style) {
        AuraButtonStyle.Primary -> PrimaryInk
        AuraButtonStyle.Highlight -> HighlightInk
        AuraButtonStyle.Danger -> BgDark
        AuraButtonStyle.Normal -> BtnBaseText
    }
    val borderColor = when (style) {
        AuraButtonStyle.Normal -> Color.White.copy(alpha = if (enabled) 0.08f else 0.04f)
        else -> Color.Transparent
    }
    val innerBorderColor = when (style) {
        AuraButtonStyle.Primary, AuraButtonStyle.Highlight -> Color.White.copy(alpha = if (enabled) 0.30f else 0.12f)
        AuraButtonStyle.Danger -> Color.White.copy(alpha = if (enabled) 0.24f else 0.1f)
        AuraButtonStyle.Normal -> Color.White.copy(alpha = if (enabled) 0.10f else 0.04f)
    }
    val topSheenAlpha = when (style) {
        AuraButtonStyle.Primary, AuraButtonStyle.Highlight -> if (enabled) 0.26f else 0.1f
        AuraButtonStyle.Danger -> if (enabled) 0.22f else 0.1f
        AuraButtonStyle.Normal -> if (enabled) 0.10f else 0.04f
    }
    val buttonAlpha = when {
        enabled -> 1f
        style == AuraButtonStyle.Normal -> 0.42f
        else -> 0.48f
    }
    val shadowElevation = when (style) {
        AuraButtonStyle.Normal -> 10.dp
        else -> 14.dp
    }
    val shadowAmbient = when (style) {
        AuraButtonStyle.Primary -> PrimaryGlow.copy(alpha = 0.18f)
        AuraButtonStyle.Highlight -> HighlightGlow.copy(alpha = 0.16f)
        AuraButtonStyle.Danger -> DangerGlow.copy(alpha = 0.16f)
        AuraButtonStyle.Normal -> Color.Black.copy(alpha = 0.10f)
    }
    val shadowSpot = when (style) {
        AuraButtonStyle.Primary -> PrimaryGlow.copy(alpha = 0.32f)
        AuraButtonStyle.Highlight -> HighlightGlow.copy(alpha = 0.24f)
        AuraButtonStyle.Danger -> DangerGlow.copy(alpha = 0.24f)
        AuraButtonStyle.Normal -> Color.Black.copy(alpha = 0.18f)
    }

    val shape = RoundedCornerShape(CabinetSpacing.buttonRadius)

    Box(
        modifier = modifier
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                ambientColor = shadowAmbient,
                spotColor = shadowSpot
            )
            .graphicsLayer {
                val scale = if (isPressed && enabled) 0.992f else 1f
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(brush = backgroundBrush, alpha = buttonAlpha)
            .border(
                width = CabinetSpacing.borderThin,
                color = borderColor,
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
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = topSheenAlpha),
                            Color.Transparent
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
                    color = innerBorderColor,
                    shape = RoundedCornerShape(CabinetSpacing.buttonRadius - 1.dp)
                )
        )
        Text(
            text = text,
            color = textColor.copy(alpha = if (enabled) 1f else 0.42f),
            style = CabinetTypography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) ChipSelectedBg else ChipIdleBg,
        animationSpec = tween(durationMillis = 220),
        label = "locationChipBackground"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryStart.copy(alpha = 0.48f) else GlassBorder,
        animationSpec = tween(durationMillis = 220),
        label = "locationChipBorder"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) TextMain else TextDim,
        animationSpec = tween(durationMillis = 220),
        label = "locationChipText"
    )
    val chipScale by animateFloatAsState(
        targetValue = if (isPressed) 0.986f else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "locationChipScale"
    )
    val shape = RoundedCornerShape(CabinetSpacing.chipRadius)
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = chipScale
                scaleY = chipScale
            }
            .shadow(
                elevation = if (isSelected) 10.dp else 0.dp,
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
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = CabinetSpacing.lg, vertical = CabinetSpacing.sm + CabinetSpacing.xs),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )
        Text(
            text = text,
            color = textColor,
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
    AuraModalLayer(scrimAlpha = 0.78f) {
        AuraSurface(
            variant = AuraSurfaceVariant.SoftGlow,
            modifier = Modifier
                .widthIn(max = CabinetSpacing.confirmModalMaxWidth)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {}
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CabinetSpacing.md)
            ) {
                Text(
                    text = title,
                    color = TextMain,
                    style = CabinetTypography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = TextSubtle,
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
private fun AuraPillSurface(
    modifier: Modifier = Modifier,
    backgroundColor: Color = PanelBgSoft.copy(alpha = 0.84f),
    borderColor: Color = GlassBorder,
    topGlowColor: Color = Color.White.copy(alpha = 0.08f),
    contentPadding: PaddingValues = PaddingValues(horizontal = CabinetSpacing.md, vertical = CabinetSpacing.sm),
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.14f)
            )
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = CabinetSpacing.borderThin,
                color = borderColor,
                shape = shape
            )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(topGlowColor, Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(1.dp)
                .border(
                    width = CabinetSpacing.borderThin,
                    color = PanelInnerLine,
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

private enum class AuraPillTone {
    Neutral, Accent, Cool, Warn, Danger
}

@Composable
private fun AuraMetaPill(
    text: String,
    tone: AuraPillTone = AuraPillTone.Neutral,
    showDot: Boolean = true
) {
    val (backgroundColor, borderColor, textColor, dotColor) = when (tone) {
        AuraPillTone.Accent -> listOf(
            AccentMint.copy(alpha = 0.10f),
            AccentMint.copy(alpha = 0.18f),
            AccentMint,
            AccentMint
        )
        AuraPillTone.Cool -> listOf(
            HighlightStart.copy(alpha = 0.08f),
            HighlightStart.copy(alpha = 0.18f),
            HighlightStart,
            HighlightStart
        )
        AuraPillTone.Warn -> listOf(
            WarnStart.copy(alpha = 0.10f),
            WarnStart.copy(alpha = 0.18f),
            WarnStart,
            WarnStart
        )
        AuraPillTone.Danger -> listOf(
            DangerStart.copy(alpha = 0.10f),
            DangerStart.copy(alpha = 0.18f),
            DangerStart,
            DangerStart
        )
        AuraPillTone.Neutral -> listOf(
            Color.White.copy(alpha = 0.05f),
            Color.White.copy(alpha = 0.08f),
            TextSubtle,
            Color.White.copy(alpha = 0.36f)
        )
    }

    AuraPillSurface(
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        topGlowColor = Color.White.copy(alpha = 0.06f)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(dotColor)
                )
            }
            Text(
                text = text,
                color = textColor,
                style = CabinetTypography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AuraClockBadge() {
    val timeText = rememberClockText()
    AuraMetaPill(text = timeText, showDot = false)
}

@Composable
private fun AuraBackIconButton(onClick: () -> Unit) {
    AuraPillSurface(
        modifier = Modifier
            .size(38.dp)
            .clickable(onClick = onClick),
        backgroundColor = Color.White.copy(alpha = 0.05f),
        borderColor = Color.White.copy(alpha = 0.08f),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "←",
                color = TextSubtle,
                style = CabinetTypography.titleSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AuraMqttStatusBadge(
    mqttStatusText: String,
    mqttConnected: Boolean?
) {
    val tone = when (mqttConnected) {
        true -> AuraPillTone.Accent
        false -> AuraPillTone.Danger
        null -> AuraPillTone.Cool
    }
    AuraMetaPill(text = mqttStatusText, tone = tone)
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
    var topBarEntered by remember(title) { mutableStateOf(false) }

    LaunchedEffect(title) {
        topBarEntered = true
    }

    val topBarAlpha by animateFloatAsState(
        targetValue = if (topBarEntered) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "screenTopBarAlpha"
    )
    val topBarOffsetY by animateFloatAsState(
        targetValue = if (topBarEntered) 0f else 16f,
        animationSpec = tween(durationMillis = 380),
        label = "screenTopBarOffset"
    )
    val topBarScale by animateFloatAsState(
        targetValue = if (topBarEntered) 1f else 0.992f,
        animationSpec = tween(durationMillis = 320),
        label = "screenTopBarScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .background(
                brush = Brush.radialGradient(
                    0f to Color.White.copy(alpha = 0.024f),
                    1f to Color.Transparent,
                    radius = 720f,
                    center = androidx.compose.ui.geometry.Offset(520f, 0f)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(CabinetSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(CabinetSpacing.md)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = topBarAlpha
                        translationY = topBarOffsetY
                        scaleX = topBarScale
                        scaleY = topBarScale
                    }
                    .height(CabinetSpacing.topBarHeight)
                    .padding(horizontal = 4.dp),
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
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.35).sp
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                color = TextDim,
                                style = CabinetTypography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.4.sp
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CabinetSpacing.sm)
                ) {
                    if (operatorInfo != null) {
                        AuraMetaPill(text = operatorInfo)
                    }
                    if (!mqttStatusText.isNullOrBlank()) {
                        AuraMqttStatusBadge(
                            mqttStatusText = mqttStatusText,
                            mqttConnected = mqttConnected
                        )
                    }
                    AuraClockBadge()
                    if (topRightAction != null) {
                        topRightAction()
                    }
                }
            }

            content()
        }
    }
}
