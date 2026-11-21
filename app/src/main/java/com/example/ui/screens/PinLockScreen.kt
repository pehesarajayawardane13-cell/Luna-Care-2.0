package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.theme.RosePrimary
import kotlinx.coroutines.delay

@Composable
fun PinLockScreen(
    userName: String,
    expectedPin: String,
    onUnlockSuccess: () -> Unit,
    onEmergencyResetPin: () -> Unit,
    onSignOutClick: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    var showRecovery by remember { mutableStateOf(false) }

    LaunchedEffect(enteredPin) {
        if (enteredPin.length == 4) {
            delay(140)
            if (enteredPin == expectedPin) {
                enteredPin = ""
                errorText = ""
                onUnlockSuccess()
            } else {
                errorText = "වැරදි PIN අංකයකි! නැවත උත්සාහ කරන්න."
                enteredPin = ""
            }
        }
    }

    fun appendDigit(digit: String) {
        if (enteredPin.length < 4) {
            errorText = ""
            enteredPin += digit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 3.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🔒", fontSize = 26.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (userName.isNotBlank()) "ආයුබෝවන්, $userName!" else "ආයුබෝවන්!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "ඇතුළු වීමට ඉලක්කම් 4ක PIN අංකය ඇතුළත් කරන්න",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 22.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val filled = i < enteredPin.length
                    val scale by animateFloatAsState(
                        targetValue = if (filled) 1.25f else 1f,
                        label = "pin_dot_scale"
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .scale(scale)
                            .background(
                                color = if (filled) RosePrimary else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = errorText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.height(18.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("RESET", "0", "BACK")
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "RESET" -> PinPadButton(
                                    label = "Reset",
                                    isAction = true,
                                    testTagName = "pin_reset_button",
                                    modifier = Modifier.weight(1f),
                                    onClick = { showRecovery = !showRecovery }
                                )
                                "BACK" -> PinPadButton(
                                    label = "⌫",
                                    isAction = true,
                                    testTagName = "pin_backspace_button",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (enteredPin.isNotEmpty()) {
                                            enteredPin = enteredPin.dropLast(1)
                                        }
                                    }
                                )
                                else -> PinPadButton(
                                    label = key,
                                    isAction = false,
                                    testTagName = "pin_key_$key",
                                    modifier = Modifier.weight(1f),
                                    onClick = { appendDigit(key) }
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showRecovery) {
                GlassCard(
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = "PIN අමතක වුණාද? Default PIN (1234) බවට Reset කරන්න:",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onEmergencyResetPin()
                                enteredPin = ""
                                showRecovery = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RosePrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("confirm_emergency_pin_reset"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("PIN -> 1234", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = { showRecovery = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("අවලංගු", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            TextButton(
                onClick = onSignOutClick,
                modifier = Modifier.testTag("lock_sign_out_button")
            ) {
                Text(
                    text = "ගිණුම මාරු කරන්න (Sign Out)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PinPadButton(
    label: String,
    isAction: Boolean,
    testTagName: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .testTag(testTagName),
        shape = RoundedCornerShape(16.dp),
        color = if (isAction) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = if (isAction) 0.dp else 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = if (isAction) 13.sp else 20.sp,
                fontWeight = if (isAction) FontWeight.SemiBold else FontWeight.Bold,
                color = if (isAction) RosePrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
