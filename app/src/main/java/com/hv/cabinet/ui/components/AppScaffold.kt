package com.hv.cabinet.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hv.cabinet.ui.layout.CabinetWindowSpec
import com.hv.cabinet.ui.layout.rememberCabinetWindowSpec

enum class AppScaffoldVariant {
    Login,
    Home,
    Business
}

@Composable
fun AppScaffold(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    variant: AppScaffoldVariant = AppScaffoldVariant.Business,
    lightweight: Boolean = false,
    showTopBar: Boolean = true,
    snackbarHostState: SnackbarHostState? = null,
    @DrawableRes backgroundImageRes: Int? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val windowSpec = rememberCabinetWindowSpec()
    val perfLite = true
    val backgroundPainter = backgroundImageRes?.let { painterResource(id = it) }
    val backgroundBrush = remember(variant) {
        when (variant) {
            AppScaffoldVariant.Login -> Brush.linearGradient(
                listOf(Color(0xFF031026), Color(0xFF07172D))
            )
            AppScaffoldVariant.Home -> Brush.linearGradient(
                listOf(Color(0xFF031127), Color(0xFF06172C))
            )
            AppScaffoldVariant.Business -> Brush.linearGradient(
                listOf(Color(0xFF041126), Color(0xFF071A31))
            )
        }
    }

    val baseBackground = if (lightweight) Color(0xFF061327) else Color.Transparent

    val rootModifier = if (lightweight) {
        Modifier
            .fillMaxSize()
            .background(baseBackground)
    } else {
        Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    }

    Box(
        modifier = rootModifier
    ) {
        if (!lightweight) {
            BackgroundLayers(
                variant = variant,
                painter = backgroundPainter,
                perfLite = perfLite,
                windowSpec = windowSpec
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = {
                if (snackbarHostState != null) {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = Color(0xFF1B3A5C),
                            contentColor = Color.White
                        )
                    }
                }
            },
            topBar = {
                if (showTopBar) {
                    if (lightweight) {
                        SimpleTopBar(
                            title = title,
                            subtitle = subtitle,
                            onBack = onBack,
                            actions = actions,
                            windowSpec = windowSpec
                        )
                    } else {
                        AppTopBar(
                            title = title,
                            subtitle = subtitle,
                            onBack = onBack,
                            actions = actions,
                            variant = variant,
                            windowSpec = windowSpec
                        )
                    }
                }
            }
        ) { inner ->
            val baseTop = if (showTopBar) (windowSpec.panelSpacing / 2) else 0.dp
            content(
                PaddingValues(
                    start = windowSpec.contentHorizontalPadding,
                    end = windowSpec.contentHorizontalPadding,
                    top = inner.calculateTopPadding() + baseTop,
                    bottom = inner.calculateBottomPadding() + windowSpec.panelSpacing
                )
            )
        }
    }
}

@Composable
private fun SimpleTopBar(
    title: String,
    subtitle: String?,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit,
    windowSpec: CabinetWindowSpec
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = windowSpec.topBarOuterPaddingHorizontal,
                vertical = windowSpec.topBarOuterPaddingVertical
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (onBack != null) 6.dp else 0.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

@Composable
private fun BackgroundLayers(
    variant: AppScaffoldVariant,
    painter: Painter?,
    perfLite: Boolean,
    windowSpec: CabinetWindowSpec
) {
    val shouldShowImage = painter != null && (!perfLite || variant == AppScaffoldVariant.Login)
    if (shouldShowImage) {
        Image(
            painter = painter!!,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = if (variant == AppScaffoldVariant.Login) {
                if (windowSpec.isLandscape) 0.38f else 0.32f
            } else {
                if (windowSpec.isLandscape) 0.08f else 0.06f
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = when (variant) {
                    AppScaffoldVariant.Login -> Color(0x6A031026)
                    AppScaffoldVariant.Home -> Color(0x52041022)
                    AppScaffoldVariant.Business -> Color(0x5E061122)
                }
            )
    )
}

@Composable
private fun AppTopBar(
    title: String,
    subtitle: String?,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit,
    variant: AppScaffoldVariant,
    windowSpec: CabinetWindowSpec
) {
    val panelBorder = when (variant) {
        AppScaffoldVariant.Home -> Color(0x7752C7EA)
        AppScaffoldVariant.Login -> Color(0x556E9BE6)
        AppScaffoldVariant.Business -> Color(0x6652C7EA)
    }
    TransparentPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = windowSpec.topBarOuterPaddingHorizontal,
                vertical = windowSpec.topBarOuterPaddingVertical
            ),
        shape = RoundedCornerShape(18.dp),
        startColor = Color(0x331A3D61),
        endColor = Color(0x2210213A),
        borderColor = panelBorder,
        contentPadding = PaddingValues(
            horizontal = windowSpec.topBarInnerPaddingHorizontal,
            vertical = windowSpec.topBarInnerPaddingVertical
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1AFFFFFF))
                        .size(40.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (onBack != null) 8.dp else 0.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}
