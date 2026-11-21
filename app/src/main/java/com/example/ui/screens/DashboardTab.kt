package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CycleCalculator
import com.example.model.CycleStats
import com.example.model.DailyLog
import com.example.model.UserProfile
import com.example.ui.components.GlassCard
import com.example.ui.theme.PinkSecondary
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.SkyWater
import com.example.ui.theme.SkyWaterDarkText
import com.example.ui.theme.SkyWaterLight
import com.example.ui.theme.WhatsAppGreen
import kotlin.math.roundToInt

@Composable
fun DashboardTab(
    profile: UserProfile,
    stats: CycleStats,
    todayLog: DailyLog?,
    onQuickPeriodToday: () -> Unit,
    onQuickLogToday: () -> Unit,
    onAdjustWater: (Int) -> Unit,
    onShareWhatsApp: () -> Unit,
    onShareEmail: () -> Unit
) {
    val waterMl = (todayLog?.waterMl ?: 0L).toInt()
    val goalMl = profile.dailyWaterGoal.toInt().coerceAtLeast(500)
    val rawProgress = (waterMl.toFloat() / goalMl.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = rawProgress, label = "water_progress")
    val waterPct = ((waterMl * 100f) / goalMl).roundToInt().coerceIn(0, 100)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hero Cycle Prediction Card
        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = stats.phaseBadge,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stats.phaseTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stats.phaseSub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "චක්‍ර දිනය",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = RosePrimary
                        )
                        Text(
                            text = "Day ${stats.currentCycleDay}",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RosePrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onQuickPeriodToday,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("dash_quick_period_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RosePrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text(
                        text = "🩸 අද Period හැදුණා",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onQuickLogToday,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("dash_quick_log_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text(
                        text = "✍️ අද සටහන් / රෝග",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 2. 100ml Water Drink Meter Card
        GlassCard(borderColor = SkyWater.copy(alpha = 0.35f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SkyWaterLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💧", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Water Drink Meter (100ml කොටස්)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (waterMl >= goalMl) {
                                "අද දෛනික ජල ඉලක්කය සම්පූර්ණයි! 🎉"
                            } else {
                                "ඉලක්කයට තව ${goalMl - waterMl} ml අවශ්‍යයි"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$waterMl ml",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SkyWater
                    )
                    Text(
                        text = "/ $goalMl ml ($waterPct%)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50.dp)),
                color = SkyWater,
                trackColor = SkyWaterLight.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WaterIncrementButton(
                    label = "-100ml",
                    isPrimary = false,
                    testTagName = "dash_water_minus_100",
                    modifier = Modifier.weight(1f),
                    onClick = { onAdjustWater(-100) }
                )
                WaterIncrementButton(
                    label = "+100ml",
                    isPrimary = true,
                    testTagName = "dash_water_plus_100",
                    modifier = Modifier.weight(1f),
                    onClick = { onAdjustWater(100) }
                )
                WaterIncrementButton(
                    label = "+250ml",
                    isPrimary = false,
                    testTagName = "dash_water_plus_250",
                    modifier = Modifier.weight(1f),
                    onClick = { onAdjustWater(250) }
                )
                WaterIncrementButton(
                    label = "+500ml",
                    isPrimary = false,
                    testTagName = "dash_water_plus_500",
                    modifier = Modifier.weight(1f),
                    onClick = { onAdjustWater(500) }
                )
            }
        }

        // 3. Key Predictions 2x2 Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PredictionMiniCard(
                label = "🗓️ මීළඟ ඔසප් දිනය",
                labelColor = RosePrimary,
                value = CycleCalculator.formatReadableSinhalaDate(stats.nextPeriodDate),
                sub = if (stats.daysUntilNextPeriod >= 0) {
                    "තව දින ${stats.daysUntilNextPeriod}කින්"
                } else {
                    "සටහන් අනුව යාවත්කාලීන වේ"
                },
                modifier = Modifier.weight(1f)
            )
            PredictionMiniCard(
                label = "🥚 ඩිම්බ මෝචනය",
                labelColor = PinkSecondary,
                value = CycleCalculator.formatReadableSinhalaDate(stats.ovulationDate),
                sub = "${stats.fertileStart} සිට ${stats.fertileEnd}",
                modifier = Modifier.weight(1f)
            )
        }

        val allSym = (todayLog?.genSymptoms ?: emptyList()) + (todayLog?.symptoms ?: emptyList())
        val todayHealthValue = if (allSym.isNotEmpty()) {
            "${allSym.first()} (Pain ${todayLog?.pain ?: 0}/5)"
        } else {
            "Pain මට්ටම: ${todayLog?.pain ?: 0}/5"
        }
        val todayHealthSub = if (todayLog?.periodDay == true) {
            "Period • ${todayLog.flow.ifBlank { "සාමාන්‍යයි" }}"
        } else {
            "Mood: ${todayLog?.moods?.firstOrNull() ?: "සටහන් නැත"}"
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PredictionMiniCard(
                label = "🔄 සාමාන්‍ය චක්‍රය",
                labelColor = RosePrimary,
                value = "දින ${stats.avgCycle}",
                sub = "Lifetime Calendar යාවත්කාලීනයි",
                modifier = Modifier.weight(1f)
            )
            PredictionMiniCard(
                label = "🩺 අද සෞඛ්‍ය / වේදනාව",
                labelColor = PinkSecondary,
                value = todayHealthValue,
                sub = todayHealthSub,
                modifier = Modifier.weight(1f)
            )
        }

        // 4. Daily Care Note Card
        GlassCard(borderColor = PinkSecondary.copy(alpha = 0.3f)) {
            Text(
                text = "💌 දෛනික Care පණිවිඩය",
                style = MaterialTheme.typography.labelLarge,
                color = RosePrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = profile.careNoteForPhase(stats.phase),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // 5. Quick WhatsApp / Email Summary Card
        GlassCard {
            Text(
                text = "📲 දෛනික සාරාංශය යවන්න",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Water + Period + රෝග ලක්ෂණ Summary එක WhatsApp / Email වෙත",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onShareWhatsApp,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("dash_whatsapp_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text("💬 WhatsApp යවන්න", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onShareEmail,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("dash_email_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("✉️ Email යවන්න", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PredictionMiniCard(
    label: String,
    labelColor: Color,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = sub,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun WaterIncrementButton(
    label: String,
    isPrimary: Boolean,
    testTagName: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(42.dp)
            .testTag(testTagName),
        shape = RoundedCornerShape(12.dp),
        color = if (isPrimary) SkyWater else SkyWaterLight.copy(alpha = 0.65f),
        border = if (isPrimary) null else BorderStroke(1.dp, SkyWater.copy(alpha = 0.35f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPrimary) Color.White else SkyWaterDarkText
            )
        }
    }
}
