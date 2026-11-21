package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.CycleCalculator
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientPrimaryButton
import com.example.ui.theme.RosePrimary
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    isSaving: Boolean,
    onSubmitSetup: (
        name: String,
        pin: String,
        whatsapp: String,
        email: String,
        lastPeriodDate: String,
        cycleLen: Int,
        periodLen: Int,
        waterGoal: Int
    ) -> Unit,
    onImportBackupJson: (String) -> Unit,
    onShowError: (String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var lastPeriodDate by remember { mutableStateOf(CycleCalculator.getTodayStr()) }
    var cycleLen by remember { mutableStateOf("28") }
    var periodLen by remember { mutableStateOf("5") }
    var waterGoal by remember { mutableStateOf("2000") }
    var showDatePicker by remember { mutableStateOf(false) }

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
                onShowError("Backup ගොනුව කියවීමට නොහැකි විය!")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = millis
                            }
                            lastPeriodDate = CycleCalculator.formatYmd(
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH) + 1,
                                cal.get(Calendar.DAY_OF_MONTH)
                            )
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
            DatePicker(state = datePickerState)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.widthIn(max = 480.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.lunacare_icon_1795071670324),
                    contentDescription = "LunaCare Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp))
                )

                Text(
                    text = buildAnnotatedString {
                        append("Luna")
                        withStyle(SpanStyle(color = RosePrimary)) { append("Care") }
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "පෞද්ගලික සෞඛ්‍ය, ජල පාන සහ ඔසප් චක්‍ර සටහන් පොත",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ඔබගේ නම") },
                    placeholder = { Text("ඔබගේ නම ඇතුළත් කරන්න") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_name_input")
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }.take(4)
                        pin = filtered
                    },
                    label = { Text("ආරක්ෂිත PIN අංකය (ඉලක්කම් 4ක්)") },
                    placeholder = { Text("••••") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    supportingText = { Text("App එක විවෘත කරන සෑම විටම මෙම PIN අංකය අවශ්‍ය වේ.") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_pin_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = whatsapp,
                        onValueChange = { whatsapp = it },
                        label = { Text("WhatsApp අංකය") },
                        placeholder = { Text("947XXXXXXXX") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("setup_whatsapp_input")
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email ලිපිනය") },
                        placeholder = { Text("name@example.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("setup_email_input")
                    )
                }

                OutlinedTextField(
                    value = lastPeriodDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("අවසන් වරට ඔසප් වීම ආරම්භ වූ දිනය") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Select Date"
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .testTag("setup_last_period_date")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = cycleLen,
                        onValueChange = { cycleLen = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("චක්‍රය (දින)", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = periodLen,
                        onValueChange = { periodLen = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("ඔසප් දින", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = waterGoal,
                        onValueChange = { waterGoal = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("ජල (ml)", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                GradientPrimaryButton(
                    text = if (isSaving) "සුරකිමින්..." else "ආරම්භ කරන්න ✨",
                    enabled = !isSaving,
                    testTagName = "setup_submit_button",
                    onClick = {
                        when {
                            name.trim().isEmpty() -> onShowError("කරුණාකර ඔබගේ නම ඇතුළත් කරන්න")
                            !pin.matches(Regex("^\\d{4}$")) -> onShowError("කරුණාකර ඉලක්කම් 4ක PIN අංකයක් ඇතුළත් කරන්න")
                            whatsapp.trim().isEmpty() && email.trim().isEmpty() ->
                                onShowError("Report යැවීමට WhatsApp අංකයක් හෝ Email ලිපිනයක් ඇතුළත් කරන්න")
                            else -> onSubmitSetup(
                                name.trim(),
                                pin,
                                whatsapp.trim(),
                                email.trim(),
                                lastPeriodDate,
                                (cycleLen.toIntOrNull() ?: 28).coerceIn(20, 45),
                                (periodLen.toIntOrNull() ?: 5).coerceIn(2, 10),
                                (waterGoal.toIntOrNull() ?: 2000).coerceIn(500, 5000)
                            )
                        }
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                TextButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier.testTag("setup_import_backup_button")
                ) {
                    Text(
                        text = "📂 කලින් ගත් Backup (JSON) ගොනුවක් ඇත්නම් Import කරන්න",
                        style = MaterialTheme.typography.labelMedium,
                        color = RosePrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
