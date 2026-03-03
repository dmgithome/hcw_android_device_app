package com.hv.cabinet.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import com.hv.cabinet.feature.home.HomeScreen
import com.hv.cabinet.feature.returning.ReturnScreen
import com.hv.cabinet.feature.take.TakeScreen

private enum class WorkspacePage {
    Home,
    Take,
    Return
}

@Composable
fun WorkspaceScreen(
    onLogout: () -> Unit
) {
    var currentPage by rememberSaveable { mutableStateOf(WorkspacePage.Home) }
    val mountedPages = remember { mutableStateListOf(WorkspacePage.Home) }

    fun switchPage(page: WorkspacePage) {
        if (!mountedPages.contains(page)) {
            mountedPages.add(page)
        }
        currentPage = page
    }

    fun pageModifier(page: WorkspacePage): Modifier {
        val active = currentPage == page
        return Modifier.layout { measurable, constraints ->
            if (active) {
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            } else {
                measurable.measure(
                    constraints.copy(
                        minWidth = 0,
                        maxWidth = 0,
                        minHeight = 0,
                        maxHeight = 0
                    )
                )
                layout(0, 0) { }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LaunchedEffect(Unit) {
            // 后台预热业务页，切页时只做显示切换。
            withFrameNanos { }
            if (!mountedPages.contains(WorkspacePage.Take)) {
                mountedPages.add(WorkspacePage.Take)
            }
            withFrameNanos { }
            if (!mountedPages.contains(WorkspacePage.Return)) {
                mountedPages.add(WorkspacePage.Return)
            }
        }

        BackHandler(enabled = currentPage != WorkspacePage.Home) {
            switchPage(WorkspacePage.Home)
        }

        if (mountedPages.contains(WorkspacePage.Home)) {
            Box(modifier = pageModifier(WorkspacePage.Home)) {
                HomeScreen(
                    onNavigateTake = { switchPage(WorkspacePage.Take) },
                    onNavigateReturn = { switchPage(WorkspacePage.Return) },
                    onLogout = onLogout,
                    active = currentPage == WorkspacePage.Home
                )
            }
        }

        if (mountedPages.contains(WorkspacePage.Take)) {
            Box(modifier = pageModifier(WorkspacePage.Take)) {
                TakeScreen(
                    onBack = { switchPage(WorkspacePage.Home) },
                    onLogout = onLogout,
                    active = currentPage == WorkspacePage.Take
                )
            }
        }

        if (mountedPages.contains(WorkspacePage.Return)) {
            Box(modifier = pageModifier(WorkspacePage.Return)) {
                ReturnScreen(
                    onBack = { switchPage(WorkspacePage.Home) },
                    onLogout = onLogout,
                    active = currentPage == WorkspacePage.Return
                )
            }
        }
    }
}
