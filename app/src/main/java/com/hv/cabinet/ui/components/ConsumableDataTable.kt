package com.hv.cabinet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import com.hv.cabinet.domain.ConsumableUiModel

// 顶层颜色常量
private val TableHeaderBg = Color(0x441B4E88)
private val RowEvenBg = Color(0x0DFFFFFF)
private val RowOddBg = Color.Transparent
private val ConflictRowBg = Color(0x33E16969)
private val ConflictAccent = Color(0xFFE16969)
private val DividerColor = Color(0x1AFFFFFF)
private val RemoveButtonColor = Color(0xFFE16969)

@Immutable
data class TableColumn(
    val header: String,
    val width: Dp
)

enum class TableVariant { Take, Return }

@Composable
fun ConsumableDataTable(
    items: List<ConsumableUiModel>,
    conflictRfids: List<String>,
    onRemove: (String) -> Unit,
    variant: TableVariant,
    modifier: Modifier = Modifier
) {
    val columns = remember(variant) { buildColumnDefs(variant) }
    val totalWidth = remember(columns) {
        columns.sumOf { it.width.value.toDouble() }.dp + (columns.size * 8).dp
    }
    val scrollState = rememberScrollState()

    if (items.isEmpty()) {
        EmptyState(
            title = "暂无耗材数据",
            subtitle = "请先扫码或通过 RFID 读码添加",
            icon = Icons.Outlined.Inventory2,
            modifier = modifier
        )
        return
    }

    // Single horizontalScroll wrapping the entire table
    Box(modifier = modifier.horizontalScroll(scrollState)) {
        Column(modifier = Modifier.width(totalWidth)) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TableHeaderBg)
                    .padding(vertical = 10.dp, horizontal = 4.dp)
            ) {
                columns.forEach { col ->
                    Box(
                        modifier = Modifier.width(col.width).padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = col.header,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            HorizontalDivider(color = DividerColor)

            // Body rows
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(items, key = { _, item -> item.rfid }) { index, item ->
                    val isConflict = conflictRfids.any { it.equals(item.rfid, ignoreCase = true) }
                    val rowBg = if (isConflict) ConflictRowBg else if (index % 2 == 0) RowEvenBg else RowOddBg

                    val conflictBorderModifier = if (isConflict) {
                        Modifier.drawBehind {
                            drawRect(
                                color = ConflictAccent,
                                topLeft = Offset.Zero,
                                size = Size(3.dp.toPx(), size.height)
                            )
                        }
                    } else {
                        Modifier
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                            .then(conflictBorderModifier)
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        columns.forEachIndexed { index, col ->
                            Box(
                                modifier = Modifier.width(col.width).padding(horizontal = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                CellContent(item, index, variant, onRemove)
                            }
                        }
                    }
                    HorizontalDivider(color = DividerColor)
                }
            }
        }
    }
}

@Composable
private fun CellContent(
    item: ConsumableUiModel,
    columnIndex: Int,
    variant: TableVariant,
    onRemove: (String) -> Unit
) {
    // Column order matches buildColumnDefs
    when (variant) {
        TableVariant.Take -> when (columnIndex) {
            0 -> CellText(item.name)
            1 -> CellText(item.code)
            2 -> CellText(item.payload.spec)
            3 -> CellText(item.payload.batch)
            4 -> CellText(item.payload.brand)
            5 -> CellText(item.payload.manufacturer)
            6 -> CellText(item.cabinetId.toString())
            7 -> CellText("1")
            8 -> CellText(item.rfid)
            9 -> RemoveButton(item.rfid, onRemove)
        }
        TableVariant.Return -> when (columnIndex) {
            0 -> CellText(item.name)
            1 -> CellText(item.code)
            2 -> CellText(item.payload.spec)
            3 -> CellText(item.payload.batch)
            4 -> CellText(item.payload.brand)
            5 -> CellText(item.payload.manufacturer)
            6 -> CellText(item.cabinetId.toString())
            7 -> CellText(item.payload.from_location_name)
            8 -> CellText(item.currentLocationName)
            9 -> CellText("1")
            10 -> CellText(item.rfid)
            11 -> RemoveButton(item.rfid, onRemove)
        }
    }
}

@Composable
private fun RemoveButton(rfid: String, onRemove: (String) -> Unit) {
    TextButton(onClick = { onRemove(rfid) }) {
        Text(
            text = "移除",
            style = MaterialTheme.typography.labelSmall,
            color = RemoveButtonColor
        )
    }
}

@Composable
private fun CellText(text: String) {
    Text(
        text = text.ifBlank { "-" },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private fun buildColumnDefs(variant: TableVariant): List<TableColumn> = buildList {
    add(TableColumn("耗材名称", 140.dp))
    add(TableColumn("耗材子编码", 130.dp))
    add(TableColumn("规格型号", 120.dp))
    add(TableColumn("批号", 100.dp))
    add(TableColumn("品牌", 80.dp))
    add(TableColumn("生产厂家", 130.dp))
    add(TableColumn("货柜名称", 80.dp))

    if (variant == TableVariant.Return) {
        add(TableColumn("来源仓库", 100.dp))
        add(TableColumn("当前所在地点", 100.dp))
    }

    val qtyHeader = if (variant == TableVariant.Take) "取用数量" else "归还数量"
    add(TableColumn(qtyHeader, 70.dp))
    add(TableColumn("唯一码", 170.dp))
    add(TableColumn("操作", 70.dp))
}
