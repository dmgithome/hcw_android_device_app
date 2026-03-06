package com.hv.cabinet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.hv.cabinet.ui.layout.rememberCabinetWindowSpec

@Composable
fun PrimaryButton(
    text: String,
    enabled: Boolean,
    loading: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val spec = rememberCabinetWindowSpec()
    val shape = RoundedCornerShape(999.dp)
    Button(
        modifier = modifier
            .height(spec.buttonHeight)
            .widthIn(min = 128.dp),
        enabled = enabled && !loading,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        onClick = onClick
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(text)
        }
    }
}

@Composable
fun SecondaryActionButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val spec = rememberCabinetWindowSpec()
    OutlinedButton(
        modifier = modifier
            .height(spec.buttonHeight)
            .widthIn(min = 128.dp),
        enabled = enabled,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        ),
        onClick = onClick
    ) {
        Text(text)
    }
}

@Composable
fun ConfirmButton(
    text: String,
    enabled: Boolean,
    loading: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val spec = rememberCabinetWindowSpec()
    val tone = MaterialTheme.colorScheme.tertiary
    Button(
        modifier = modifier
            .height(spec.buttonHeight)
            .widthIn(min = 128.dp),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = tone,
            contentColor = MaterialTheme.colorScheme.onTertiary
        ),
        onClick = onClick
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(text)
        }
    }
}

@Composable
fun OutlinedConfirmButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val spec = rememberCabinetWindowSpec()
    val tone = MaterialTheme.colorScheme.tertiary
    OutlinedButton(
        modifier = modifier
            .height(spec.buttonHeight)
            .widthIn(min = 128.dp),
        enabled = enabled,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, SolidColor(tone.copy(alpha = 0.7f))),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = tone
        ),
        onClick = onClick
    ) {
        Text(text)
    }
}

@Composable
fun DangerActionButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val spec = rememberCabinetWindowSpec()
    Button(
        modifier = modifier
            .height(spec.buttonHeight)
            .widthIn(min = 128.dp),
        enabled = enabled,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        onClick = onClick
    ) {
        Text(text)
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
            modifier = Modifier.weight(1f),
            onClick = onPrimaryClick
        )

        if (!secondaryText.isNullOrBlank() && onSecondaryClick != null) {
            SecondaryActionButton(
                text = secondaryText,
                enabled = secondaryEnabled,
                modifier = Modifier.weight(1f),
                onClick = onSecondaryClick
            )
        }

        if (!dangerText.isNullOrBlank() && onDangerClick != null) {
            DangerActionButton(
                text = dangerText,
                enabled = dangerEnabled,
                modifier = Modifier.weight(1f),
                onClick = onDangerClick
            )
        }
    }
}
