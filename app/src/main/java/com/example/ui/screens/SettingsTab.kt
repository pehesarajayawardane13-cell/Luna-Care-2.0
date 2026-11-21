package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.BackupSerializer
import com.example.domain.CycleCalculator
import com.example.model.DailyLog
import com.example.model.PetalStyle
import com.example.model.UserProfile
import com.example.ui.components.GlassCard
import com.example.ui.components.SelectableRoseChip
import com.example.ui.theme.RosePrimary
import kotlin.math.roundToInt

@Composable
fun SettingsTab(
    profile: UserProfile,
    logsByDate: Map<String, DailyLog>,
    userEmailDisplay: String,
    onUpdatePetals: (enabled: Boolean?, style: PetalStyle?, size: Int?, opacityPct: Int?) -> Unit,
    onSaveContacts: (whatsapp: String, email: String) -> Unit,
    onSaveCareNotes: (period: String, pms: String, ovulation: String, normal: String) -> Unit,
    onToggleTheme: () -> Unit,
    onSaveProfile: (name: String, waterGoal: Int, newPin: String) -> Unit,
    onImportBackupJson: (String) -> Unit,
    onShowToast: (String, String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current

    var whatsapp by remember(profile.bfWhatsapp) { mutableStateOf(profile.bfWhatsapp) }
    var email by remember(profile.bfEmail) { mutableStateOf(profile.bfEmail) }

    var notePeriod by remember(profile.careNotePeriod) { mutableStateOf(profile.careNotePeriod) }
    var notePms by remember(profile.careNotePms) { mutableStateOf(profile.careNotePms) }
    var noteOvulation by remember(profile.careNoteOvulation) { mutableStateOf(profile.careNoteOvulation) }
    var noteNormal by remember(profile.careNoteNormal) { mutableStateOf(profile.careNoteNormal) }

    var userName by remember(profile.userName) { mutableStateOf(profile.userName) }
    var waterGoal by remember(profile.dailyWaterGoal) { mutableStateOf(profile.dailyWaterGoal.toString()) }
    var newPin by remember { mutableStateOf("") }

    var localPetalSize by remember(profile.petalSize) { mutableStateOf(profile.petalSize.toFloat()) }
    var localPetalOpacity by remember(profile.petalOpacityPct) { mutableStateOf(profile.petalOpacityPct.toFloat()) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val json = BackupSerializer.exportToJson(profile, logsByDate)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                onShowToast("Backup ගොනුව Export විය! ⬇️", "💾")
            } catch (_: Exception) {
                onShowToast("Backup Export කිරීමට නොහැකි විය!", "❌")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!text.isNullOrBlank()) {
                    onImportBackupJson(text)
                }
            } catch (_: Exception) {
                onShowToast("වැරදි Backup ගොනුවකි!", "❌")
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Falling Flowers & Optical Controls
        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "🌸 වැටෙන මල් & Optical සැකසුම්",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Background එකේ වැටෙන මල් වර්ග, ප්‍රමාණය සහ පෙනෙන මට්ටම",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = profile.petalsEnabled,
                        onCheckedChange = { onUpdatePetals(it, null, null, null) },
                        colors = SwitchDefaults.colors(checkedTrackColor = RosePrimary),
                        modifier = Modifier.testTag("settings_petal_toggle")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "මල් වර්ගය තෝරන්න (Flower Style)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val currentStyle = PetalStyle.fromKey(profile.petalStyle)
                PetalStyle.entries.forEach { st ->
                    SelectableRoseChip(
                        label = "${st.emoji} ${st.label}",
                        selected = currentStyle == st,
                        onClick = { onUpdatePetals(null, st, null, null) },
                        modifier = Modifier.weight(1f),
                        testTagName = "petal_style_${st.key}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Size", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${localPetalSize.roundToInt()}dp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RosePrimary)
                        }
                        Slider(
                            value = localPetalSize,
                            onValueChange = { localPetalSize = it },
                            onValueChangeFinished = {
                                onUpdatePetals(null, null, localPetalSize.roundToInt(), null)
                            },
                            valueRange = 12f..42f,
                            colors = SliderDefaults.colors(thumbColor = RosePrimary, activeTrackColor = RosePrimary)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Opacity", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${localPetalOpacity.roundToInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RosePrimary)
                        }
                        Slider(
                            value = localPetalOpacity,
                            onValueChange = { localPetalOpacity = it },
                            onValueChangeFinished = {
                                onUpdatePetals(null, null, null, localPetalOpacity.roundToInt())
                            },
                            valueRange = 8f..80f,
                            colors = SliderDefaults.colors(thumbColor = RosePrimary, activeTrackColor = RosePrimary)
                        )
                    }
                }
            }
        }

        // 2. Report Receiver Contact Settings
        GlassCard {
            Text(
                text = "💑 Report Receiver Contact",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = whatsapp,
                onValueChange = { whatsapp = it },
                label = { Text("WhatsApp අංකය (Country code සමඟ)") },
                placeholder = { Text("947XXXXXXXX") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email ලිපිනය") },
                placeholder = { Text("name@example.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { onSaveContacts(whatsapp, email) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("settings_save_contacts_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)
            ) {
                Text("සම්බන්ධතා සුරකින්න", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 3. Custom Care Notes
        GlassCard {
            Text(
                text = "💌 විශේෂ Care Notes වෙනස් කරන්න",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = notePeriod,
                onValueChange = { notePeriod = it },
                label = { Text("Period දින සඳහා") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = notePms,
                onValueChange = { notePms = it },
                label = { Text("PMS දින සඳහා") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = noteOvulation,
                onValueChange = { noteOvulation = it },
                label = { Text("Ovulation දින සඳහා") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = noteNormal,
                onValueChange = { noteNormal = it },
                label = { Text("සාමාන්‍ය දින සඳහා") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { onSaveCareNotes(notePeriod, notePms, noteOvulation, noteNormal) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("settings_save_notes_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)
            ) {
                Text("පණිවිඩ සුරකින්න", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 4. Appearance, Profile, Water Goal & PIN
        GlassCard {
            Text(
                text = "🔐 Appearance, Profile, Water Goal & PIN",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Display Theme (${if (profile.isDarkTheme) "Dark" else "Light"})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "රාත්‍රී Dark Mode හෝ දිවා Light Mode",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onToggleTheme,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RosePrimary),
                        modifier = Modifier.testTag("settings_theme_toggle_button")
                    ) {
                        Text(
                            text = if (profile.isDarkTheme) "☀️ Light" else "🌙 Dark",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("නම") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = waterGoal,
                    onValueChange = { waterGoal = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = { Text("ජල ඉලක්කය (ml)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = newPin,
                onValueChange = { newPin = it.filter { ch -> ch.isDigit() }.take(4) },
                label = { Text("නව PIN අංකය (වෙනස් කිරීමට පමණි)") },
                placeholder = { Text("••••") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    val goal = waterGoal.toIntOrNull() ?: 2000
                    onSaveProfile(userName, goal, newPin)
                    if (newPin.matches(Regex("^\\d{4}$"))) {
                        newPin = ""
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("settings_save_profile_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)
            ) {
                Text("සැකසුම් යාවත්කාලීන කරන්න", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 5. Backup & Restore (JSON) + Account Sign Out
        GlassCard {
            Text(
                text = "💾 Backup & Restore (JSON)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        exportLauncher.launch("LunaCare_Backup_${CycleCalculator.getTodayStr()}.json")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("settings_export_json_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("⬇️ Export Backup", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("settings_import_json_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("⬆️ Import Backup", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Google Cloud Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = userEmailDisplay.ifBlank { "Signed In" },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("settings_sign_out_button")
                    ) {
                        Text("Sign Out", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Medical Disclaimer
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
