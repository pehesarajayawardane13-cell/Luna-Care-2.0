package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.components.SelectableRoseChip
import com.example.ui.theme.PinkSecondary
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.SkyWater
import com.example.ui.theme.SkyWaterLight
import com.example.ui.theme.WhatsAppGreen
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LoggerTab(
    draft: DailyLog,
    isSaving: Boolean,
    onDatePicked: (String) -> Unit,
    onUpdateDraft: ((DailyLog) -> DailyLog) -> Unit,
    onMarkCycleStart: () -> Unit,
    onSaveLog: () -> Unit,
    onSaveAndWhatsApp: () -> Unit,
    onSaveAndEmail: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = millis
                            }
                            val picked = CycleCalculator.formatYmd(
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH) + 1,
                                cal.get(Calendar.DAY_OF_MONTH)
                            )
                            onDatePicked(picked)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("තෝරන්න")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("අවලංගු")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    fun toggleListItem(list: List<String>, value: String): List<String> =
        if (list.contains(value)) list - value else list + value

    GlassCard {
        // Header + Date Picker pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "දෛනික සටහන් & රෝග ලක්ෂණ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "ඕනෑම දිනයක් තෝරා සටහන් කළ හැක",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                modifier = Modifier
                    .clickable { showDatePicker = true }
                    .testTag("logger_date_picker_button"),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = "📅 ${draft.date}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Period Toggle + Mark Cycle Start
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🩸 මෙදින ඔසප් වීම සිදුවේ (Period Day)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = draft.periodDay,
                        onCheckedChange = { checked ->
                            onUpdateDraft { it.copy(periodDay = checked) }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = RosePrimary),
                        modifier = Modifier.testTag("logger_period_switch")
                    )
                }

                OutlinedButton(
                    onClick = onMarkCycleStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("logger_mark_cycle_start_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = RosePrimary
                    )
                ) {
                    Text(
                        text = "✨ මෙම දිනය නව ඔසප් චක්‍රයේ ආරම්භය ලෙස සලකුණු කරන්න",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 100ml Water Meter for Selected Date
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SkyWaterLight.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, SkyWater.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💧 ජල පාන ප්‍රමාණය (100ml කොටස්)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SkyWater
                    )
                    Text(
                        text = "${draft.waterMl} ml",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SkyWater
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-100, 100, 250, 500).forEach { delta ->
                        val lbl = if (delta > 0) "+${delta}ml" else "${delta}ml"
                        WaterIncrementButton(
                            label = lbl,
                            isPrimary = delta == 100,
                            testTagName = "logger_water_$delta",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onUpdateDraft {
                                    it.copy(waterMl = (it.waterMl + delta).coerceIn(0L, 10000L))
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Blood Flow
        Text(
            text = "රුධිර වහන මට්ටම (Blood Flow)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CycleCalculator.FLOW_OPTIONS.forEach { flowLabel ->
                SelectableRoseChip(
                    label = flowLabel,
                    selected = draft.flow == flowLabel,
                    onClick = { onUpdateDraft { it.copy(flow = flowLabel) } }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pain Level 0-5
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "වේදනා මට්ටම (Pain Level 0–5)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = CycleCalculator.PAIN_DESCRIPTIONS[draft.pain.toInt()] ?: "${draft.pain}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = RosePrimary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        val painEmojis = listOf("😊\nනැත", "🙂\nසුළු", "😐\nමද", "😣\nසැලකිය", "😭\nතදබල", "🤒\nඉතා තද")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (lvl in 0..5) {
                val active = draft.pain.toInt() == lvl
                Surface(
                    onClick = { onUpdateDraft { it.copy(pain = lvl.toLong()) } },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("logger_pain_$lvl"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (active) RosePrimary else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (active) Color.Transparent else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$lvl ${painEmojis[lvl].substringBefore('\n')}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = painEmojis[lvl].substringAfter('\n'),
                            fontSize = 9.sp,
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // General Health Symptoms + Severity 0-5
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, PinkSecondary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🩺 සාමාන්‍ය රෝග ලක්ෂණ (General Health)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Pattern Check",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = RosePrimary
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CycleCalculator.GENERAL_SYMPTOM_OPTIONS.forEach { (storedVal, chipLabel) ->
                        SelectableRoseChip(
                            label = chipLabel,
                            selected = draft.genSymptoms.contains(storedVal),
                            onClick = {
                                onUpdateDraft {
                                    it.copy(genSymptoms = toggleListItem(it.genSymptoms, storedVal))
                                }
                            }
                        )
                    }
                }

                Text(
                    text = "රෝග ලක්ෂණ තීව්‍රතාව (Severity 0–5)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (sev in 0..5) {
                        val active = draft.genSeverity.toInt() == sev
                        Surface(
                            onClick = { onUpdateDraft { it.copy(genSeverity = sev.toLong()) } },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("logger_sev_$sev"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) RosePrimary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (active) Color.Transparent else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = sev.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Moods
        Text(
            text = "මනෝභාවය (Moods)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CycleCalculator.MOOD_OPTIONS.forEach { mood ->
                SelectableRoseChip(
                    label = mood,
                    selected = draft.moods.contains(mood),
                    onClick = { onUpdateDraft { it.copy(moods = toggleListItem(it.moods, mood)) } }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cycle Symptoms
        Text(
            text = "ඔසප් රෝග ලක්ෂණ (Cycle Symptoms)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CycleCalculator.CYCLE_SYMPTOM_OPTIONS.forEach { (storedVal, chipLabel) ->
                SelectableRoseChip(
                    label = chipLabel,
                    selected = draft.symptoms.contains(storedVal),
                    onClick = {
                        onUpdateDraft { it.copy(symptoms = toggleListItem(it.symptoms, storedVal)) }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cravings
        Text(
            text = "කෑමට ආසා දේවල් (Cravings)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CycleCalculator.CRAVING_OPTIONS.forEach { craving ->
                SelectableRoseChip(
                    label = craving,
                    selected = draft.cravings.contains(craving),
                    onClick = {
                        onUpdateDraft { it.copy(cravings = toggleListItem(it.cravings, craving)) }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Personal Note
        OutlinedTextField(
            value = draft.note,
            onValueChange = { txt -> onUpdateDraft { it.copy(note = txt.take(1000)) } },
            label = { Text("පුද්ගලික සටහනක් (Note)") },
            placeholder = { Text("අද දවස ගැන පොඩි සටහනක්...") },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("logger_note_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        GradientPrimaryButton(
            text = if (isSaving) "සුරකිමින්..." else "සටහන සුරකින්න (Save Log) 💾",
            enabled = !isSaving,
            testTagName = "logger_save_button",
            onClick = onSaveLog
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onSaveAndWhatsApp,
                enabled = !isSaving,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("logger_save_whatsapp_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WhatsAppGreen,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text("💬 Save & WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onSaveAndEmail,
                enabled = !isSaving,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("logger_save_email_button"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text("✉️ Save & Email", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
