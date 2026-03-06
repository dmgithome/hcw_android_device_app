package com.hv.cabinet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hv.cabinet.ui.theme.CabinetSpacing
import com.hv.cabinet.ui.theme.CabinetTypography
import com.hv.cabinet.ui.theme.PrimaryStart
import com.hv.cabinet.ui.theme.TextDim
import kotlin.math.ceil

const val DEFAULT_PAGE_SIZE = 50

@Composable
fun PaginationFooter(
    total: Int,
    currentPage: Int,
    pageSize: Int = DEFAULT_PAGE_SIZE,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPages = maxOf(1, ceil(total.toDouble() / pageSize).toInt())

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(CabinetSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "第 $currentPage / $totalPages 页",
            style = CabinetTypography.bodySmall,
            color = TextDim,
            modifier = Modifier.padding(end = CabinetSpacing.xs)
        )

        PageButton("首", enabled = currentPage > 1) { onPageChange(1) }
        PageButton("上", enabled = currentPage > 1) { onPageChange(currentPage - 1) }
        PageButton("下", enabled = currentPage < totalPages) { onPageChange(currentPage + 1) }
        PageButton("尾", enabled = currentPage < totalPages) { onPageChange(totalPages) }
    }
}

@Composable
private fun PageButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val bgColor = if (enabled) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }
    val borderColor = if (enabled) {
        PrimaryStart.copy(alpha = 0.48f)
    } else {
        TextDim.copy(alpha = 0.6f)
    }
    val textColor = if (enabled) {
        PrimaryStart
    } else {
        TextDim.copy(alpha = 0.92f)
    }

    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(999.dp))
            .border(CabinetSpacing.borderThin, borderColor, RoundedCornerShape(999.dp))
    ) {
        Text(
            text = text,
            style = CabinetTypography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}
