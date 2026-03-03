package com.hv.cabinet.data.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugConfigProvider @Inject constructor(
    appPreferences: AppPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(DebugConfig())
    val state: StateFlow<DebugConfig> = _state.asStateFlow()

    init {
        scope.launch {
            appPreferences.debugConfigFlow.collectLatest { cfg ->
                _state.value = cfg
            }
        }
    }

    fun isDebugLogEnabled(): Boolean = _state.value.debugLogEnabled

    fun isHttpBodyLogEnabled(): Boolean = _state.value.httpBodyLogEnabled
}
