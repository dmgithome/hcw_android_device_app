package com.hv.cabinet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hv.cabinet.domain.ConsumableUiModel
import com.hv.cabinet.ui.theme.*

@Immutable
data class TableColumn(
    val header: String,
    val weight: Float
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
    if (items.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无扫描项，请先扫码或通过 RFID 添加",
                color = TextDim,
                fontSize = 14.sp
            )
        }
        return
    }

    val columns = remember { buildColumnDefs() }
    val conflictSet = remember(conflictRfids) { conflictRfids.map { it.lowercase() }.toSet() }
    
    Column(modifier = modifier.fillMaxSize()) {
        // 表头
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TableHeaderBg)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            columns.forEach { col ->
                Text(
                    text = col.header,
                    color = TextDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(col.weight),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        HorizontalDivider(color = GlassBorder)

        // 表身
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(items, key = { _, item -> item.rfid }) { index, item ->
                val isConflict = conflictSet.contains(item.rfid.lowercase())
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when {
                                isConflict -> DangerStart.copy(alpha = 0.1f)
                                index % 2 == 0 -> Color.Transparent
                                else -> TableRowAlt
                            }
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    columns.forEachIndexed { colIndex, col ->
                        Box(modifier = Modifier.weight(col.weight)) {
                            CellContent(item, colIndex, variant, onRemove)
                        }
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.02f))
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
    when (variant) {
        TableVariant.Take -> when (columnIndex) {
            0 -> Text(item.name, color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            1 -> CellText(item.payload.spec)
            2 -> CellText(item.payload.batch)
            3 -> CellText("A-01") // 模拟货位
            4 -> Text("1", color = PrimaryStart, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            5 -> Text(item.rfid.takeLast(6).uppercase(), color = AccentMint, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            6 -> Text(
                "移除",
                color = DangerStart,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(DangerStart.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .clickable { onRemove(item.rfid) }
            )
        }
        TableVariant.Return -> when (columnIndex) {
            0 -> Text(item.name, color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            1 -> CellText(item.payload.spec)
            2 -> CellText(item.payload.batch)
            3 -> CellText("A-01")
            4 -> Text("1", color = PrimaryStart, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            5 -> Text(item.rfid.takeLast(6).uppercase(), color = AccentMint, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            6 -> Text(
                "移除",
                color = DangerStart,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(DangerStart.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .clickable { onRemove(item.rfid) }
            )
        }
    }
}

@Composable
private fun CellText(text: String) {
    Text(
        text = text.ifBlank { "-" },
        color = TextDim,
        fontSize = 14.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private fun buildColumnDefs(): List<TableColumn> = buildList {
    add(TableColumn("耗材名称", 2f))
    add(TableColumn("规格型号", 1.2f))
    add(TableColumn("生产批号", 1.2f))
    add(TableColumn("货位", 1f))
    add(TableColumn("数量", 0.8f))
    add(TableColumn("标识码", 1.2f))
    add(TableColumn("操作", 0.8f))
}
