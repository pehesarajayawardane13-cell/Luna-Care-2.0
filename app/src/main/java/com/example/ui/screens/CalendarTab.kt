package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CycleCalculator
import com.example.model.CycleStats
import com.example.model.DailyLog
import com.example.ui.components.GlassCard
import com.example.ui.theme.PinkSecondary
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.RoseSoftContainer
import com.example.ui.theme.SkyWater
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarTab(
    year: Int,
    month: Int,
    selectedDate: String,
    stats: CycleStats,
    logsByDate: Map<String, DailyLog>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToday: () -> Unit,
    onSelectDate: (String) -> Unit,
    onEditDateInLogger: (String) -> Unit
) {
    val todayStr = CycleCalculator.getTodayStr()
    val monthTitle = "$year ${CycleCalculator.SINHALA_FULL_MONTHS.getOrElse(month - 1) { "" }}"

    val cal = Calendar.getInstance().apply {
        set(year, month - 1, 1)
    }
    val firstDayOffset = cal.get(Calendar.DAY_OF_WEEK) - 1 // Sunday = 0
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onPrevMonth,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("cal_prev_month"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("‹", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = RosePrimary)
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    TextButton(
                        onClick = onJumpToday,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.testTag("cal_today_button")
                    ) {
                        Text(
                            text = "අද මාසයට යන්න",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RosePrimary
                        )
                    }
                }

                Surface(
                    onClick = onNextMonth,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("cal_next_month"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("›", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = RosePrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val weekdays = listOf("ඉරි", "සඳු", "අඟ", "බදා", "බ්‍රහ", "සිකු", "සෙන")
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdays.forEach { dayName ->
                    Text(
                        text = dayName,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val totalCells = firstDayOffset + daysInMonth
            val rowsCount = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (r in 0 until rowsCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (c in 0 until 7) {
                            val idx = r * 7 + c
                            val dayNum = idx - firstDayOffset + 1
                            if (dayNum in 1..daysInMonth) {
                                val cellDate = CycleCalculator.formatYmd(year, month, dayNum)
                                val log = logsByDate[cellDate]
                                val isLoggedPeriod = log?.periodDay == true
                                val isPredictedPeriod = !isLoggedPeriod && stats.predictedPeriodDays.contains(cellDate)
                                val isOvulation = cellDate == stats.ovulationDate
                                val isSelected = cellDate == selectedDate
                                val isToday = cellDate == todayStr
                                val hasLogDot = log != null && (
                                    log.genSymptoms.isNotEmpty() ||
                                        log.symptoms.isNotEmpty() ||
                                        log.pain > 0L ||
                                        log.waterMl > 0L
                                    )

                                val bgColor = when {
                                    isLoggedPeriod -> RosePrimary
                                    isPredictedPeriod -> RoseSoftContainer
                                    isOvulation -> PinkSecondary
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                }
                                val textColor = when {
                                    isLoggedPeriod || isOvulation -> Color.White
                                    isPredictedPeriod -> Color(0xFF9F1239)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                val borderStroke = when {
                                    isSelected -> BorderStroke(2.dp, RosePrimary)
                                    isToday -> BorderStroke(1.5.dp, PinkSecondary)
                                    isPredictedPeriod -> BorderStroke(1.dp, RosePrimary.copy(alpha = 0.5f))
                                    else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSelectDate(cellDate) }
                                        .testTag("cal_day_$cellDate"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = bgColor,
                                    border = borderStroke
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = if (isLoggedPeriod || isPredictedPeriod || isToday || isSelected) {
                                                FontWeight.ExtraBold
                                            } else {
                                                FontWeight.Medium
                                            },
                                            color = textColor
                                        )
                                        if (hasLogDot && !isLoggedPeriod) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .background(
                                                        color = if (isOvulation) Color.White else SkyWater,
                                                        shape = CircleShape
                                                    )
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f).height(44.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LegendChip("සටහන් කළ Period", RosePrimary)
                LegendChip("අනුමාන Period", RoseSoftContainer)
                LegendChip("Ovulation", PinkSecondary)
                LegendChip("Water/Health Log", SkyWater)
            }
        }

        // Selected Date Detail Card
        val detailLog = logsByDate[selectedDate]
        val syms = (detailLog?.genSymptoms ?: emptyList()) + (detailLog?.symptoms ?: emptyList())
        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "${CycleCalculator.formatReadableSinhalaDate(selectedDate)} ($selectedDate)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Water: ${detailLog?.waterMl ?: 0}ml • Pain: ${detailLog?.pain ?: 0}/5 • Flow: ${detailLog?.flow ?: "නැත"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { onEditDateInLogger(selectedDate) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RosePrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("cal_edit_selected_date")
                ) {
                    Text("සටහන් කරන්න ✍️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "රෝග ලක්ෂණ: ${if (syms.isNotEmpty()) syms.joinToString(", ") else "සටහන් නැත"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Moods: ${if (!detailLog?.moods.isNullOrEmpty()) detailLog!!.moods.joinToString(", ") else "සටහන් නැත"}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (!detailLog?.note.isNullOrBlank()) {
                Text(
                    text = "සටහන: ${detailLog!!.note}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun LegendChip(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
