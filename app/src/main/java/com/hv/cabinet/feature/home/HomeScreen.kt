package com.hv.cabinet.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hv.cabinet.ui.components.AuraScreen
import com.hv.cabinet.ui.components.AuraSurface
import com.hv.cabinet.ui.components.AuraSurfaceVariant
import com.hv.cabinet.ui.theme.*

@Composable
fun HomeScreen(
    onNavigateTake: () -> Unit,
    onNavigateReturn: () -> Unit,
    onLogout: () -> Unit,
    active: Boolean = true,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

    LaunchedEffect(active) {
        if (active) {
            viewModel.consumeRouteLock()
        }
    }

    AuraScreen(
        title = "耗材屋",
        subtitle = "智能管理终端",
        operatorInfo = "${state.userName} - ${state.userRole}",
        mqttStatusText = state.mqttStatusText,
        mqttConnected = state.mqttConnected,
        topRightAction = {
            Text(
                text = "退出登录",
                color = TextSubtle,
                style = CabinetTypography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(999.dp))
                    .border(
                        width = CabinetSpacing.borderThin,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = CabinetSpacing.lg, vertical = CabinetSpacing.sm)
                    .clickable { viewModel.logout(onLogout) }
            )
        },
        onCancel = null // 主页不需要底部返回
    ) {
        // 保持原有信息结构，只做视觉层优化
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .padding(top = CabinetSpacing.lg)
                    .heightIn(max = CabinetSpacing.homeCardMaxHeight),
                horizontalArrangement = Arrangement.spacedBy(CabinetSpacing.homeCardGap)
            ) {
                AuraFeatureCard(
                    title = "耗材取用",
                    tag = "取用流程",
                    description = "扫描已在库耗材，选择目标位置后提交取用记录。",
                    accentColor = HighlightStart,
                    surfaceVariant = AuraSurfaceVariant.CoolGlow,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = { viewModel.toTake(onNavigateTake) }
                )

                AuraFeatureCard(
                    title = "耗材归还",
                    tag = "归还流程",
                    description = "扫描需归还耗材，系统自动匹配归还位置后提交记录。",
                    accentColor = PrimaryStart,
                    surfaceVariant = AuraSurfaceVariant.SoftGlow,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = { viewModel.toReturn(onNavigateReturn) }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = CabinetSpacing.lg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "终端编号：HCW-TERM-082 · 系统运行正常",
                color = TextSubtle.copy(alpha = 0.7f),
                style = CabinetTypography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun AuraFeatureCard(
    title: String,
    tag: String,
    description: String,
    accentColor: Color,
    surfaceVariant: AuraSurfaceVariant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var appeared by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        appeared = true
    }

    val cardAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 280, delayMillis = if (title.contains("归还")) 60 else 0),
        label = "homeCardAlpha"
    )
    val cardOffsetY by animateFloatAsState(
        targetValue = if (appeared) 0f else 24f,
        animationSpec = tween(durationMillis = 360, delayMillis = if (title.contains("归还")) 60 else 0),
        label = "homeCardOffset"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.992f else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "homeCardScale"
    )

    AuraSurface(
        variant = surfaceVariant,
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                alpha = cardAlpha
                translationY = cardOffsetY
                scaleX = cardScale
                scaleY = cardScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(CabinetSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (title.contains("取用")) "01 / TAKE" else "02 / RETURN",
                    color = accentColor.copy(alpha = 0.78f),
                    style = CabinetTypography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.1.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            HorizontalDivider(
                color = GlassBorder,
                thickness = CabinetSpacing.borderThin
            )
            Text(
                text = title,
                color = TextMain,
                fontSize = 34.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.8).sp
            )
            Text(
                text = description,
                color = TextSubtle,
                style = CabinetTypography.bodySmall,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 3
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "进入工作流",
                    color = TextSubtle,
                    style = CabinetTypography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "→",
                    color = accentColor.copy(alpha = 0.92f),
                    style = CabinetTypography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
