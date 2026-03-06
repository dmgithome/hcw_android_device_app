package com.hv.cabinet.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Activity-level barcode scan event bus.
 * MainActivity.dispatchKeyEvent delegates ALL key events here.
 * Buffers printable chars, emits completed code on Enter.
 * Properly pairs ACTION_DOWN / ACTION_UP to keep Android input system happy.
 */
object BarcodeScanBus {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    private val buffer = StringBuilder()
    // Track whether we consumed ACTION_DOWN so we can match ACTION_UP
    private var lastEnterConsumed = false
    // Cooldown: consume all Enter events within 150ms after a barcode emit
    // to swallow trailing CR/LF from scanners
    private var lastEmitTimeNanos = 0L
    private const val COOLDOWN_NS = 150_000_000L // 150ms

    /**
     * Called from Activity.dispatchKeyEvent for EVERY key event.
     * Returns true if the event should be consumed (not passed to Views).
     */
    fun handleKeyEvent(event: android.view.KeyEvent): Boolean {
        when (event.action) {
            android.view.KeyEvent.ACTION_DOWN -> {
                when (event.keyCode) {
                    android.view.KeyEvent.KEYCODE_ENTER -> {
                        val code = buffer.toString().trim()
                        buffer.clear()
                        if (code.isNotBlank()) {
                            _events.tryEmit(code)
                            lastEnterConsumed = true
                            lastEmitTimeNanos = System.nanoTime()
                            return true
                        }
                        // Within cooldown after a recent emit — consume trailing Enter
                        if (System.nanoTime() - lastEmitTimeNanos < COOLDOWN_NS) {
                            lastEnterConsumed = true
                            return true
                        }
                        // Standalone Enter (no buffer, no cooldown) — let it pass
                        lastEnterConsumed = false
                        return false
                    }
                    else -> {
                        val char = event.unicodeChar
                        if (char > 0) {
                            buffer.append(Char(char))
                            return true
                        }
                    }
                }
            }
            android.view.KeyEvent.ACTION_UP -> {
                // Must consume ACTION_UP for keys whose ACTION_DOWN we consumed
                if (event.unicodeChar > 0) return true
                if (event.keyCode == android.view.KeyEvent.KEYCODE_ENTER && lastEnterConsumed) {
                    lastEnterConsumed = false
                    return true
                }
            }
        }
        return false
    }

    fun clearBuffer() {
        buffer.clear()
        lastEnterConsumed = false
    }
}

/**
 * Composable that subscribes to BarcodeScanBus and forwards scanned codes.
 * No UI, no focus, no TextField — zero performance impact.
 */
@Composable
fun ScanKeyboardHandler(
    onScanned: (String) -> Unit
) {
    val currentOnScanned by rememberUpdatedState(onScanned)

    LaunchedEffect(Unit) {
        BarcodeScanBus.events.collect { code ->
            currentOnScanned(code)
        }
    }
}
