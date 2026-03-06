package com.hv.cabinet.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.hv.cabinet.ui.theme.CabinetSpacing

@Composable
fun AuraEntrance(
    modifier: Modifier = Modifier,
    entranceKey: Any? = Unit,
    delayMillis: Int = 0,
    durationMillis: Int = 420,
    initialAlpha: Float = 0f,
    initialOffsetY: Float = 18f,
    initialScale: Float = 0.985f,
    content: @Composable BoxScope.() -> Unit
) {
    var entered by remember(entranceKey) { mutableStateOf(false) }

    LaunchedEffect(entranceKey) {
        entered = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else initialAlpha,
        animationSpec = tween(durationMillis = durationMillis, delayMillis = delayMillis),
        label = "auraEntranceAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (entered) 0f else initialOffsetY,
        animationSpec = tween(durationMillis = durationMillis + 60, delayMillis = delayMillis),
        label = "auraEntranceOffset"
    )
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else initialScale,
        animationSpec = tween(durationMillis = durationMillis, delayMillis = delayMillis),
        label = "auraEntranceScale"
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offsetY
            scaleX = scale
            scaleY = scale
        },
        content = content
    )
}

@Composable
fun AuraModalLayer(
    modifier: Modifier = Modifier,
    scrimAlpha: Float,
    onScrimClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(CabinetSpacing.massive),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    var entered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        entered = true
    }

    val overlayAlpha by animateFloatAsState(
        targetValue = if (entered) scrimAlpha else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "auraModalOverlayAlpha"
    )
    val modalAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 260, delayMillis = 40),
        label = "auraModalAlpha"
    )
    val modalOffsetY by animateFloatAsState(
        targetValue = if (entered) 0f else 28f,
        animationSpec = tween(durationMillis = 300, delayMillis = 20),
        label = "auraModalOffset"
    )
    val modalScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.96f,
        animationSpec = tween(durationMillis = 280, delayMillis = 20),
        label = "auraModalScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = overlayAlpha))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { onScrimClick?.invoke() }
            )
            .padding(contentPadding),
        contentAlignment = contentAlignment
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                alpha = modalAlpha
                translationY = modalOffsetY
                scaleX = modalScale
                scaleY = modalScale
            },
            content = content
        )
    }
}
