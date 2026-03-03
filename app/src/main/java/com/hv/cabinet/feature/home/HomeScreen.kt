package com.hv.cabinet.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hv.cabinet.core.PerfMonitor
import com.hv.cabinet.ui.components.AppScaffold
import com.hv.cabinet.ui.components.AppScaffoldVariant
import com.hv.cabinet.ui.components.FeatureCard
import com.hv.cabinet.ui.layout.rememberCabinetWindowSpec

@Composable
fun HomeScreen(
    onNavigateTake: () -> Unit,
    onNavigateReturn: () -> Unit,
    onLogout: () -> Unit,
    active: Boolean = true,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val windowSpec = rememberCabinetWindowSpec()

    LaunchedEffect(active) {
        if (active) {
            viewModel.consumeRouteLock()
            PerfMonitor.mark("home_interactive")
        }
    }

    AppScaffold(
        title = "耗材屋",
        subtitle = listOf(state.userName, state.userRole).filter { it.isNotBlank() }.joinToString("  ·  ").ifBlank { "设备主界面" },
        variant = AppScaffoldVariant.Home,
        actions = {
            TextButton(
                onClick = { viewModel.showLogoutDialog(true) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("退出登录")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(
                        max = if (windowSpec.constrainContentWidthInPortrait) {
                            windowSpec.portraitMaxContentWidth
                        } else {
                            Dp.Unspecified
                        }
                    )
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(windowSpec.panelSpacing)
            ) {
                if (windowSpec.isWideLandscape) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(windowSpec.panelSpacing)
                    ) {
                        FeatureCard(
                            title = "耗材取用",
                            subtitle = "进入取用流程，支持 RFID 与扫码；需先选择目标位置再确认提交",
                            onClick = { viewModel.toTake(onNavigateTake) },
                            onPress = { viewModel.markCardPressed("take") },
                            modifier = Modifier.weight(1f),
                            accent = Color(0xFF52C7EA),
                            minHeight = windowSpec.featureCardMinHeight + 28.dp
                        )

                        FeatureCard(
                            title = "耗材归还",
                            subtitle = "进入归还流程，支持 RFID 与扫码；归还位置由系统自动推断",
                            onClick = { viewModel.toReturn(onNavigateReturn) },
                            onPress = { viewModel.markCardPressed("return") },
                            modifier = Modifier.weight(1f),
                            accent = Color(0xFF55C68E),
                            minHeight = windowSpec.featureCardMinHeight + 28.dp
                        )
                    }
                } else {
                    FeatureCard(
                        title = "耗材取用",
                        subtitle = "进入取用流程，支持 RFID 与扫码；目标位置为选择，不是填写",
                        onClick = { viewModel.toTake(onNavigateTake) },
                        onPress = { viewModel.markCardPressed("take") },
                        accent = Color(0xFF52C7EA),
                        minHeight = windowSpec.featureCardMinHeight
                    )

                    FeatureCard(
                        title = "耗材归还",
                        subtitle = "进入归还流程，支持 RFID 与扫码；归还位置由后端自动推断",
                        onClick = { viewModel.toReturn(onNavigateReturn) },
                        onPress = { viewModel.markCardPressed("return") },
                        accent = Color(0xFF55C68E),
                        minHeight = windowSpec.featureCardMinHeight
                    )
                }
            }
        }
    }

    // Logout confirmation overlay (Box overlay instead of AlertDialog)
    if (active && state.showLogoutDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x88000000))
                .clickable { viewModel.showLogoutDialog(false) },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .clickable(enabled = false, onClick = {}),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF0E1E34),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4452C7EA))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "确认退出登录",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "退出后需要重新登录，是否继续？",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { viewModel.showLogoutDialog(false) }) {
                            Text("取消")
                        }
                        TextButton(
                            onClick = { viewModel.logout(onLogout) },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("确认")
                        }
                    }
                }
            }
        }
    }
}
