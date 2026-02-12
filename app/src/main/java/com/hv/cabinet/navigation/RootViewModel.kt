package com.hv.cabinet.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hv.cabinet.data.store.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {
    private val _state = MutableStateFlow(RootUiState())
    val state: StateFlow<RootUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val session = appPreferences.sessionFlow.first()
            _state.value = RootUiState(
                loading = false,
                startDestination = if (session.isLoggedIn) AppRoutes.HOME else AppRoutes.LOGIN
            )
        }
    }
}

data class RootUiState(
    val loading: Boolean = true,
    val startDestination: String = AppRoutes.LOGIN
)
