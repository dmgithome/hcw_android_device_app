package com.hv.cabinet

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hv.cabinet.navigation.CabinetApp
import com.hv.cabinet.ui.components.BarcodeScanBus
import com.hv.cabinet.ui.theme.CabinetTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CabinetTheme {
                CabinetApp()
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (BarcodeScanBus.handleKeyEvent(event)) return true
        return super.dispatchKeyEvent(event)
    }
}
