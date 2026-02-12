package com.hv.cabinet.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PrimaryButton(
    text: String,
    enabled: Boolean,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        modifier = Modifier
            .height(46.dp)
            .widthIn(min = 128.dp),
        enabled = enabled && !loading,
        onClick = onClick
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.height(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(text)
        }
    }
}

@Composable
fun FooterActionRow(
    primaryText: String,
    onPrimaryClick: () -> Unit,
    primaryEnabled: Boolean,
    primaryLoading: Boolean = false,
    secondaryText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    secondaryEnabled: Boolean = false,
    dangerText: String? = null,
    onDangerClick: (() -> Unit)? = null,
    dangerEnabled: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PrimaryButton(
            text = primaryText,
            enabled = primaryEnabled,
            loading = primaryLoading,
            onClick = onPrimaryClick
        )

        if (!secondaryText.isNullOrBlank() && onSecondaryClick != null) {
            OutlinedButton(
                modifier = Modifier
                    .height(46.dp)
                    .widthIn(min = 128.dp),
                enabled = secondaryEnabled,
                onClick = onSecondaryClick
            ) {
                Text(secondaryText)
            }
        }

        if (!dangerText.isNullOrBlank() && onDangerClick != null) {
            Button(
                modifier = Modifier
                    .height(46.dp)
                    .widthIn(min = 128.dp),
                enabled = dangerEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                onClick = onDangerClick
            ) {
                Text(dangerText)
            }
        }
    }
}
