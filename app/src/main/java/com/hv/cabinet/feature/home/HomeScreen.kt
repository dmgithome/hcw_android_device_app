package com.hv.cabinet.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hv.cabinet.core.PerfMonitor
import com.hv.cabinet.ui.components.AppScaffold
import com.hv.cabinet.ui.components.FeatureCard
import com.hv.cabinet.ui.components.StatusBanner

@Composable
fun HomeScreen(
    onNavigateTake: () -> Unit,
    onNavigateReturn: () -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.consumeRouteLock()
        PerfMonitor.mark("home_interactive")
    }

    AppScaffold(
        title = "设备主控台",
        subtitle = "${state.userName}  ${state.userRole}",
        actions = {
            TextButton(
                onClick = { viewModel.showLogoutDialog(true) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("退出登录")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatusBanner(message = state.message)

            FeatureCard(
                title = "耗材取用",
                subtitle = "进入取用流程，支持 RFID 与扫码",
                onClick = { viewModel.toTake(onNavigateTake) },
                onPress = { viewModel.markCardPressed("take") },
                modifier = Modifier.weight(1f)
            )

            FeatureCard(
                title = "耗材归还",
                subtitle = "进入归还流程，支持 RFID 与扫码",
                onClick = { viewModel.toReturn(onNavigateReturn) },
                onPress = { viewModel.markCardPressed("return") },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (state.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showLogoutDialog(false) },
            title = { Text("确认退出登录") },
            text = { Text("退出后需要重新登录，是否继续？") },
            confirmButton = {
                TextButton(onClick = { viewModel.logout(onLogout) }) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showLogoutDialog(false) }) {
                    Text("取消")
                }
            }
        )
    }
}
