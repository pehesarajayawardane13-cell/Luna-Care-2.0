package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CycleCalculator
import com.example.model.DailyLog
import com.example.model.UserProfile
import com.example.ui.components.GlassCard
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.WhatsAppGreen
import java.util.Calendar

@Composable
fun InsightsTab(
    profile: UserProfile,
    logsByDate: Map<String, DailyLog>,
    selectedYearMonth: String,
    onChangeReportMonth: (String) -> Unit,
    onShareDailyWhatsApp: () -> Unit,
    onShareMonthlyWhatsApp: (String) -> Unit
) {
    val todayStr = CycleCalculator.getTodayStr()
    val todayLog = logsByDate[todayStr] ?: DailyLog(date = todayStr)
    val waterMl = todayLog.waterMl.toInt()
    val goalMl = profile.dailyWaterGoal.toInt().coerceAtLeast(500)
    val syms = todayLog.genSymptoms + todayLog.symptoms
    val monthlyReport = CycleCalculator.buildMonthlyReport(profile, logsByDate, selectedYearMonth)

    fun stepMonth(delta: Int) {
        val parts = selectedYearMonth.split("-").mapNotNull { it.toIntOrNull() }
        val y = parts.getOrNull(0) ?: 2026
        val m = parts.getOrNull(1) ?: 1
        val cal = Calendar.getInstance().apply {
            set(y, m - 1, 1)
            add(Calendar.MONTH, delta)
        }
        val nextYm = String.format(
            java.util.Locale.US,
            "%04d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1
        )
        onChangeReportMonth(nextYm)
    }

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
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "📊 Daily & Monthly Health Report",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Water, Period, Pain, General Symptoms & Monthly Trends",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onShareDailyWhatsApp,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("insights_daily_wa_button")
                ) {
                    Text("💬 Daily WA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Daily Report Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("💧 ජල පානය: $waterMl ml / $goalMl ml", fontSize = 12.sp)
                    Text(
                        "🩸 Period & Flow: ${if (todayLog.periodDay) "ඔව්" else "නැත"} (${todayLog.flow.ifBlank { "නැත" }}) • Pain: ${todayLog.pain}/5",
                        fontSize = 12.sp
                    )
                    Text(
                        "🩺 රෝග ලක්ෂණ: ${if (syms.isNotEmpty()) "${syms.joinToString(", ")} (Severity ${todayLog.genSeverity}/5)" else "සටහන් නැත"}",
                        fontSize = 12.sp
                    )
                    Text(
                        "😊 Moods: ${if (todayLog.moods.isNotEmpty()) todayLog.moods.joinToString(", ") else "සටහන් නැත"}",
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(14.dp))

            // Monthly Picker Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🗓️ Monthly Analysis",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = { stepMonth(-1) },
                        modifier = Modifier.size(34.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("‹", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RosePrimary)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = selectedYearMonth,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Surface(
                        onClick = { stepMonth(1) },
                        modifier = Modifier.size(34.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("›", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RosePrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Monthly Report Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        "මාසය: ${monthlyReport.yearMonth}  |  සටහන් දින: ${monthlyReport.loggedDaysCount}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Period දින: ${monthlyReport.periodDaysCount}  |  උපරිම Pain: ${monthlyReport.maxPain}/5",
                        fontSize = 12.sp
                    )
                    Text(
                        "සාමාන්‍ය Water Intake: ${monthlyReport.avgWaterMl} ml / දිනකට",
                        fontSize = 12.sp
                    )
                    Text(
                        "ප්‍රධාන රෝග ලක්ෂණ: ${monthlyReport.topSymptoms}",
                        fontSize = 12.sp
                    )
                    Text(
                        "ප්‍රධාන Moods: ${monthlyReport.topMoods}",
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onShareMonthlyWhatsApp(monthlyReport.whatsappText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("insights_monthly_wa_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WhatsAppGreen,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "💬 Monthly Report එක WhatsApp වෙත යවන්න",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Medical Disclaimer Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text(
                text = "*මෙය තොරතුරු දැනගැනීමේ අරමුණින් පමණි. වෛද්‍ය උපදෙස් හෝ රෝග විනිශ්චයක් සඳහා සුදුසුකම් ලත් වෘත්තිකයෙකු හමුවන්න.*",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}
