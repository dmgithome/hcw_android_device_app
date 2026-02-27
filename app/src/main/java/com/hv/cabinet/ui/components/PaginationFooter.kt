package com.hv.cabinet.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "共有 $total 条记录，$totalPages 页，当前是第 $currentPage 页",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            PageButton("首页", enabled = currentPage > 1) { onPageChange(1) }
            PageButton("上一页", enabled = currentPage > 1) { onPageChange(currentPage - 1) }
            PageButton("下一页", enabled = currentPage < totalPages) { onPageChange(currentPage + 1) }
            PageButton("尾页", enabled = currentPage < totalPages) { onPageChange(totalPages) }
        }
    }
}

@Composable
private fun PageButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (enabled) Color(0xFF52C7EA) else Color(0x6652C7EA)
        )
    }
}
