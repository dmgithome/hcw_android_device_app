package com.hv.cabinet.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hv.cabinet.core.PerfMonitor
import com.hv.cabinet.data.api.CabinetRepository
import com.hv.cabinet.data.store.AppPreferences
import com.hv.cabinet.domain.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CabinetRepository,
    appPreferences: AppPreferences
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            appPreferences.sessionFlow.collectLatest { session ->
                _state.value = _state.value.copy(
                    userName = session.userName,
                    userRole = session.userRole
                )
            }
        }
    }

    fun markCardPressed(key: String) {
        PerfMonitor.mark("home_card_${key}_pointerdown")
    }

    fun toTake(navigate: () -> Unit) {
        launchNavigation("take", navigate)
    }

    fun toReturn(navigate: () -> Unit) {
        launchNavigation("return", navigate)
    }

    fun consumeRouteLock() {
        _state.value = _state.value.copy(routeLocked = false)
    }

    fun showLogoutDialog(show: Boolean) {
        _state.value = _state.value.copy(showLogoutDialog = show)
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            _state.value = _state.value.copy(showLogoutDialog = false)
            onDone()
        }
    }

    private fun launchNavigation(key: String, navigate: () -> Unit) {
        if (_state.value.routeLocked) return
        _state.value = _state.value.copy(routeLocked = true)
        PerfMonitor.mark("home_card_${key}_navigate_start")
        navigate()
        // routeLocked 由目标页面的 consumeRouteLock() 解锁，无需人为延迟
    }
}

data class HomeUiState(
    val userName: String = "",
    val userRole: String = "",
    val routeLocked: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val message: UiMessage = UiMessage()
)
