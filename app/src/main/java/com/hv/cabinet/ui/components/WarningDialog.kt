package com.hv.cabinet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hv.cabinet.domain.InventoryMode
import com.hv.cabinet.domain.ScanWarning
import com.hv.cabinet.domain.ScanWarningType
import com.hv.cabinet.ui.theme.CabinetSpacing
import com.hv.cabinet.ui.theme.CabinetTypography
import com.hv.cabinet.ui.theme.DangerStart
import com.hv.cabinet.ui.theme.PanelBgSoft
import com.hv.cabinet.ui.theme.PrimaryStart
import com.hv.cabinet.ui.theme.TextDim
import com.hv.cabinet.ui.theme.TextMain
import com.hv.cabinet.ui.theme.WarnStart

private enum class WarningSectionKey {
    ModeConflict,
    NotInStock,
    Error
}

private data class WarningSection(
    val title: String,
    val lines: List<String>,
    val accentColor: Color
)

/**
 * 异常提醒弹窗：沿用老项目的分组语义，但统一为 ADS 视觉风格。
 */
@Composable
fun WarningDialog(
    warnings: List<ScanWarning>,
    mode: InventoryMode,
    onDismiss: () -> Unit
) {
    if (warnings.isEmpty()) return

    val sections = buildWarningSections(warnings = warnings, mode = mode)
    if (sections.none { it.lines.isNotEmpty() }) return

    val totalCount = warnings.sumOf { it.count }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.64f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {}
            )
            .padding(CabinetSpacing.massive),
        contentAlignment = Alignment.Center
    ) {
        AuraSurface(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {}
                )
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(CabinetSpacing.md)) {
                Text(
                    text = "耗材异常提示",
                    color = TextMain,
                    style = CabinetTypography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "本次扫描共发现 $totalCount 个异常项：",
                    color = TextDim,
                    style = CabinetTypography.bodyMedium
                )

                sections.forEach { section ->
                    WarningSectionCard(section = section)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    AuraButton(
                        text = "确认",
                        style = AuraButtonStyle.Primary,
                        modifier = Modifier
                            .widthIn(min = 140.dp)
                            .height(CabinetSpacing.modalButtonHeight),
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun WarningSectionCard(section: WarningSection) {
    val shape = RoundedCornerShape(CabinetSpacing.buttonRadius)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelBgSoft.copy(alpha = 0.9f), shape)
            .border(
                width = CabinetSpacing.borderThin,
                color = section.accentColor.copy(alpha = 0.45f),
                shape = shape
            )
            .padding(horizontal = CabinetSpacing.md, vertical = CabinetSpacing.sm)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CabinetSpacing.xs)) {
            Text(
                text = section.title,
                color = section.accentColor,
                style = CabinetTypography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            section.lines.forEach { line ->
                Text(
                    text = "• $line",
                    color = TextDim,
                    style = CabinetTypography.bodySmall
                )
            }
        }
    }
}

private fun buildWarningSections(
    warnings: List<ScanWarning>,
    mode: InventoryMode
): List<WarningSection> {
    val grouped = mutableMapOf(
        WarningSectionKey.ModeConflict to mutableListOf<String>(),
        WarningSectionKey.NotInStock to mutableListOf<String>(),
        WarningSectionKey.Error to mutableListOf<String>()
    )

    warnings.forEach { warning ->
        val key = sectionKey(warning.type, mode)
        grouped.getValue(key).add(warning.message.ifBlank { defaultWarningMessage(warning) })
    }

    val modeConflictTitle = if (mode == InventoryMode.TAKE) "已取用无法再次出库" else "未取用无法归还"

    return listOf(
        WarningSection(
            title = modeConflictTitle,
            lines = grouped.getValue(WarningSectionKey.ModeConflict).distinct(),
            accentColor = WarnStart
        ),
        WarningSection(
            title = "未入库耗材",
            lines = grouped.getValue(WarningSectionKey.NotInStock).distinct(),
            accentColor = DangerStart
        ),
        WarningSection(
            title = "异常耗材",
            lines = grouped.getValue(WarningSectionKey.Error).distinct(),
            accentColor = PrimaryStart
        )
    ).filter { it.lines.isNotEmpty() }
}

private fun sectionKey(type: ScanWarningType, mode: InventoryMode): WarningSectionKey {
    return when (mode) {
        InventoryMode.TAKE -> when (type) {
            ScanWarningType.AlreadyTaken -> WarningSectionKey.ModeConflict
            ScanWarningType.NotInStock -> WarningSectionKey.NotInStock
            else -> WarningSectionKey.Error
        }

        InventoryMode.RETURN -> when (type) {
            ScanWarningType.AlreadyInStock -> WarningSectionKey.ModeConflict
            ScanWarningType.NotInStock -> WarningSectionKey.NotInStock
            else -> WarningSectionKey.Error
        }
    }
}

private fun defaultWarningMessage(warning: ScanWarning): String =
    when (warning.type) {
        ScanWarningType.AlreadyTaken -> "${warning.count} 个耗材已在取用流程中"
        ScanWarningType.AlreadyInStock -> "${warning.count} 个耗材已在库内"
        ScanWarningType.NotInStock -> "${warning.count} 个耗材未入库"
        ScanWarningType.Consumed -> "${warning.count} 个耗材已消耗"
        ScanWarningType.UsedReturn -> "${warning.count} 个耗材为消耗退回"
        ScanWarningType.Unknown -> "${warning.count} 个耗材状态异常"
    }
