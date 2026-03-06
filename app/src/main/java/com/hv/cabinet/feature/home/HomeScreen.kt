package com.hv.cabinet.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hv.cabinet.ui.components.AuraScreen
import com.hv.cabinet.ui.components.AuraSurface
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
                color = DangerStart.copy(alpha = 0.85f),
                style = CabinetTypography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(
                        color = DangerStart.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(999.dp)
                    )
                    .border(
                        width = CabinetSpacing.borderThin,
                        color = DangerStart.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = CabinetSpacing.md, vertical = CabinetSpacing.sm)
                    .clickable { viewModel.logout(onLogout) }
            )
        },
        onCancel = null // 主页不需要底部返回
    ) {
        // 功能选择区（居中且收窄，避免双卡片占满屏幕）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(max = CabinetSpacing.homeCardMaxHeight),
                horizontalArrangement = Arrangement.spacedBy(CabinetSpacing.homeCardGap)
            ) {
                AuraFeatureCard(
                    title = "耗材取用",
                    tag = "取用流程",
                    description = "扫描已在库耗材，选择目标位置后提交取用记录。",
                    accentColor = PrimaryStart,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = { viewModel.toTake(onNavigateTake) }
                )

                AuraFeatureCard(
                    title = "耗材归还",
                    tag = "归还流程",
                    description = "扫描需归还耗材，系统自动匹配归还位置后提交记录。",
                    accentColor = AccentMint,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = { viewModel.toReturn(onNavigateReturn) }
                )
            }
        }

        // ADS 统一页脚
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = CabinetSpacing.lg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "终端编号：HCW-TERM-082 · 系统运行正常",
                color = TextDim.copy(alpha = 0.65f),
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuraSurface(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
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
                    text = tag,
                    color = accentColor.copy(alpha = 0.78f),
                    style = CabinetTypography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = if (title.contains("取用")) "01" else "02",
                    color = accentColor.copy(alpha = 0.38f),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            HorizontalDivider(
                color = accentColor.copy(alpha = 0.22f),
                thickness = CabinetSpacing.borderThin
            )
            Text(
                text = title,
                color = accentColor,
                fontSize = 44.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Text(
                text = description,
                color = TextDim,
                style = CabinetTypography.bodyMedium,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 3
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "点击进入",
                color = accentColor.copy(alpha = 0.9f),
                style = CabinetTypography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
