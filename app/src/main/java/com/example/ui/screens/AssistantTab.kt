package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.model.CycleStats
import com.example.ui.components.GlassCard
import com.example.ui.theme.AmberWarningBg
import com.example.ui.theme.AmberWarningBorder
import com.example.ui.theme.AmberWarningText
import com.example.ui.theme.PinkSecondary
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.SkyWater
import com.example.ui.theme.SkyWaterLight
import com.example.ui.theme.WhatsAppGreen

@Composable
fun AssistantTab(
    stats: CycleStats,
    todayWaterMl: Int,
    chatMessages: List<ChatMessage>,
    isAiStreaming: Boolean,
    showRedFlagBanner: Boolean,
    onSendPrompt: (String) -> Unit,
    onShareMessageToWhatsApp: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    fun submitCurrent() {
        val q = inputText.trim()
        if (q.isNotEmpty() && !isAiStreaming) {
            inputText = ""
            onSendPrompt(q)
        }
    }

    GlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(RosePrimary, PinkSecondary))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🤖", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AI Health Assistant",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Gemini AI Connected ✨",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RosePrimary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(50.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = "${todayWaterMl}ml • Day ${stats.currentCycleDay}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        AnimatedVisibility(visible = showRedFlagBanner) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = AmberWarningBg,
                border = BorderStroke(1.dp, AmberWarningBorder)
            ) {
                Text(
                    text = "⚠️ වෛද්‍ය උපදෙස් සිහිකැඳවීම: තදබල වේදනාවක් (4–5), උණ, කරකැවිල්ල හෝ දින කිහිපයක් පවතින අසනීපයක් ඇත්නම් කරුණාකර සුදුසුකම් ලත් වෛද්‍යවරයෙකු හමුවන්න. මෙය තොරතුරු සඳහා පමණි.",
                    fontSize = 11.sp,
                    color = AmberWarningText,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick prompt chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickPromptPill(
                label = "🩺 අද Health Check",
                bg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                enabled = !isAiStreaming,
                onClick = {
                    onSendPrompt("අද මගේ water intake, period phase සහ රෝග ලක්ෂණ අනුව care report එකක් දෙන්න")
                }
            )
            QuickPromptPill(
                label = "💧 Water Check",
                bg = SkyWaterLight.copy(alpha = 0.75f),
                textColor = SkyWater,
                enabled = !isAiStreaming,
                onClick = {
                    onSendPrompt("මගේ water intake මට්ටම හොඳද? තව කොපමණ වතුර බොන්න ඕනද?")
                }
            )
            QuickPromptPill(
                label = "🌸 Cycle Care Tips",
                bg = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                enabled = !isAiStreaming,
                onClick = {
                    onSendPrompt("Period cramps සහ mood swings අඩු කරගන්න සරල care tips දෙන්න")
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chat Thread
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            chatMessages.forEach { msg ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
                ) {
                    val bubbleShape = if (msg.isUser) {
                        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                    } else {
                        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
                    }
                    val bubbleColor = when {
                        msg.isUser -> RosePrimary
                        msg.isError -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    }
                    val textColor = when {
                        msg.isUser -> Color.White
                        msg.isError -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Surface(
                        shape = bubbleShape,
                        color = bubbleColor,
                        border = if (msg.isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.widthIn(max = 310.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (msg.isStreaming && msg.text == "...") {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = RosePrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = msg.text,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = textColor
                            )
                        }
                    }

                    if (!msg.isUser && !msg.isStreaming && !msg.isError) {
                        TextButton(
                            onClick = { onShareMessageToWhatsApp(msg.text) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = "📲 WhatsApp එකට යවන්න",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhatsAppGreen
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("සෞඛ්‍යය හෝ period ගැන ප්‍රශ්නයක් අසන්න...", fontSize = 12.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submitCurrent() }),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("ai_chat_input")
            )

            Button(
                onClick = { submitCurrent() },
                enabled = !isAiStreaming && inputText.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RosePrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .height(50.dp)
                    .testTag("ai_chat_send_button")
            ) {
                Text(
                    text = if (isAiStreaming) "..." else "යවන්න",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QuickPromptPill(
    label: String,
    bg: Color,
    textColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50.dp),
        color = bg,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.25f))
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
