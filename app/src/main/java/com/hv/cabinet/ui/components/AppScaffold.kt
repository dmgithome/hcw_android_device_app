package com.hv.cabinet.ui.components

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
    content: @Composable (PaddingValues) -> Unit
) {
    val windowSpec = rememberCabinetWindowSpec()
    val backgroundBrush = remember(variant) {
        when (variant) {
            AppScaffoldVariant.Login -> Brush.verticalGradient(
                colors = listOf(Color(0xFF171D26), Color(0xFF10151D))
            )
            AppScaffoldVariant.Home -> Brush.verticalGradient(
                colors = listOf(Color(0xFF162030), Color(0xFF0F151D))
            )
            AppScaffoldVariant.Business -> Brush.verticalGradient(
                colors = listOf(Color(0xFF151B26), Color(0xFF10151C))
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = {
                if (snackbarHostState != null) {
                    SnackbarHost(hostState = snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = Color(0xFF2A3240),
                            contentColor = MaterialTheme.colorScheme.onSurface
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
private fun AppTopBar(
    title: String,
    subtitle: String?,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit,
    variant: AppScaffoldVariant,
    windowSpec: CabinetWindowSpec
) {
    val panelBorder = when (variant) {
        AppScaffoldVariant.Home -> Color(0x4D94A0B0)
        AppScaffoldVariant.Login -> Color(0x4D94A0B0)
        AppScaffoldVariant.Business -> Color(0x4D94A0B0)
    }
    TransparentPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = windowSpec.topBarOuterPaddingHorizontal,
                vertical = windowSpec.topBarOuterPaddingVertical
            ),
        shape = RoundedCornerShape(18.dp),
        startColor = Color(0xFF242A34),
        endColor = Color(0xFF242A34),
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
                        .background(Color(0x1A94A0B0))
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
