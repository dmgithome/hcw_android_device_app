package com.hv.cabinet.ui.layout

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class CabinetWidthClass {
    Narrow,
    Medium,
    Wide
}

enum class CabinetButtonLayoutMode {
    SingleRow,
    SplitRows
}

data class CabinetWindowSpec(
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val isLandscape: Boolean,
    val widthClass: CabinetWidthClass,
    val contentHorizontalPadding: Dp,
    val panelSpacing: Dp,
    val buttonHeight: Dp,
    val featureCardMinHeight: Dp,
    val portraitMaxContentWidth: Dp,
    val useTwoPaneBusiness: Boolean,
    val preferWrappedChips: Boolean,
    val topBarOuterPaddingHorizontal: Dp,
    val topBarOuterPaddingVertical: Dp,
    val topBarInnerPaddingHorizontal: Dp,
    val topBarInnerPaddingVertical: Dp,
    val topActionBarButtonLayout: CabinetButtonLayoutMode
) {
    val isWideLandscape: Boolean = isLandscape && widthClass == CabinetWidthClass.Wide
    val constrainContentWidthInPortrait: Boolean = !isLandscape
}

@Composable
fun rememberCabinetWindowSpec(): CabinetWindowSpec {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp
    val heightDp = configuration.screenHeightDp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    return remember(widthDp, heightDp, isLandscape) {
        buildCabinetWindowSpec(
            widthDp = widthDp,
            heightDp = heightDp,
            isLandscape = isLandscape
        )
    }
}

private fun buildCabinetWindowSpec(
    widthDp: Int,
    heightDp: Int,
    isLandscape: Boolean
): CabinetWindowSpec {
    val widthClass = when {
        widthDp < 900 -> CabinetWidthClass.Narrow
        widthDp < 1280 -> CabinetWidthClass.Medium
        else -> CabinetWidthClass.Wide
    }

    val isWideLandscape = isLandscape && widthClass == CabinetWidthClass.Wide
    val contentHorizontalPadding = if (isWideLandscape) 24.dp else 16.dp
    val panelSpacing = if (isWideLandscape) 16.dp else 12.dp
    val buttonHeight = if (isLandscape) 56.dp else 52.dp
    val featureCardMinHeight = if (isWideLandscape) 210.dp else 176.dp
    val portraitMaxContentWidth = when (widthClass) {
        CabinetWidthClass.Narrow -> 640.dp
        CabinetWidthClass.Medium -> 760.dp
        CabinetWidthClass.Wide -> 840.dp
    }

    return CabinetWindowSpec(
        screenWidthDp = widthDp,
        screenHeightDp = heightDp,
        isLandscape = isLandscape,
        widthClass = widthClass,
        contentHorizontalPadding = contentHorizontalPadding,
        panelSpacing = panelSpacing,
        buttonHeight = buttonHeight,
        featureCardMinHeight = featureCardMinHeight,
        portraitMaxContentWidth = portraitMaxContentWidth,
        useTwoPaneBusiness = isWideLandscape,
        preferWrappedChips = isWideLandscape,
        topBarOuterPaddingHorizontal = if (isWideLandscape) 16.dp else 12.dp,
        topBarOuterPaddingVertical = if (isWideLandscape) 12.dp else 10.dp,
        topBarInnerPaddingHorizontal = if (isWideLandscape) 14.dp else 12.dp,
        topBarInnerPaddingVertical = if (isWideLandscape) 12.dp else 10.dp,
        topActionBarButtonLayout = if (isLandscape) {
            CabinetButtonLayoutMode.SingleRow
        } else {
            CabinetButtonLayoutMode.SplitRows
        }
    )
}
