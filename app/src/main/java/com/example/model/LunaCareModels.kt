package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val DEFAULT_CARE_NOTE_PERIOD =
    "මේ දිනවල හොඳින් විවේක ගන්න, උණුසුම් ජලය පානය කරන්න. අවශ්‍ය ඕනෑම වෙලාවක මම ළඟින් ඉන්නවා 🌸"
const val DEFAULT_CARE_NOTE_PMS =
    "මීළඟ ඔසප් දින ළං වෙමින් පවතී. සැහැල්ලු ආහාර, ප්‍රමාණවත් නින්ද සහ සන්සුන් බව වැදගත් වේ 🌷"
const val DEFAULT_CARE_NOTE_OVULATION =
    "මෙය ඔබේ චක්‍රයේ සරු කාලයයි. ජල පානය සහ සමබර ආහාර සමඟ සෞඛ්‍ය සම්පන්නව සිටින්න ✨"
const val DEFAULT_CARE_NOTE_NORMAL =
    "සෑම දිනකම ප්‍රමාණවත් තරම් ජලය පානය කරමින් ඔබේ සෞඛ්‍යය ගැන සැලකිලිමත් වන්න 🌿"

enum class PetalStyle(val key: String, val label: String, val emoji: String) {
    SAKURA("sakura", "Sakura", "🌸"),
    ROSE("rose", "Rose", "🌹"),
    LOTUS("lotus", "Lotus", "🪷"),
    BLOSSOM("blossom", "Blossom", "🌼");

    companion object {
        fun fromKey(raw: String?): PetalStyle =
            entries.firstOrNull { it.key == raw } ?: SAKURA
    }
}

enum class CyclePhase(val key: String) {
    PERIOD("period"),
    PMS("pms"),
    OVULATION("ovulation"),
    NORMAL("normal")
}

enum class MainTab(val title: String) {
    DASHBOARD("මුල්"),
    CALENDAR("දින දර්ශන"),
    LOGGER("සටහන්"),
    ASSISTANT("AI සහායක"),
    INSIGHTS("වාර්තා"),
    SETTINGS("සැකසුම්")
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val userName: String = "",
    val setupDone: Boolean = false,
    val pin: String = "1234",
    val themeMode: String = "light",
    val petalsEnabled: Boolean = true,
    val petalStyle: String = "sakura",
    val petalSize: Long = 22L,
    val petalOpacityPct: Long = 34L,
    val defaultCycleLength: Long = 28L,
    val defaultPeriodDuration: Long = 5L,
    val dailyWaterGoal: Long = 2000L,
    val periodStarts: List<String> = emptyList(),
    val bfWhatsapp: String = "",
    val bfEmail: String = "",
    val careNotePeriod: String = DEFAULT_CARE_NOTE_PERIOD,
    val careNotePms: String = DEFAULT_CARE_NOTE_PMS,
    val careNoteOvulation: String = DEFAULT_CARE_NOTE_OVULATION,
    val careNoteNormal: String = DEFAULT_CARE_NOTE_NORMAL,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isDarkTheme: Boolean
        get() = themeMode == "dark"

    fun careNoteForPhase(phase: CyclePhase): String = when (phase) {
        CyclePhase.PERIOD -> careNotePeriod.ifBlank { DEFAULT_CARE_NOTE_PERIOD }
        CyclePhase.PMS -> careNotePms.ifBlank { DEFAULT_CARE_NOTE_PMS }
        CyclePhase.OVULATION -> careNoteOvulation.ifBlank { DEFAULT_CARE_NOTE_OVULATION }
        CyclePhase.NORMAL -> careNoteNormal.ifBlank { DEFAULT_CARE_NOTE_NORMAL }
    }

    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "userName" to userName.trim().ifBlank { "User" }.take(80),
        "setupDone" to setupDone,
        "pin" to pin.take(4).ifBlank { "1234" },
        "themeMode" to if (themeMode == "dark") "dark" else "light",
        "petalsEnabled" to petalsEnabled,
        "petalStyle" to PetalStyle.fromKey(petalStyle).key,
        "petalSize" to petalSize.coerceIn(12L, 42L),
        "petalOpacityPct" to petalOpacityPct.coerceIn(8L, 80L),
        "defaultCycleLength" to defaultCycleLength.coerceIn(20L, 45L),
        "defaultPeriodDuration" to defaultPeriodDuration.coerceIn(2L, 10L),
        "dailyWaterGoal" to dailyWaterGoal.coerceIn(500L, 5000L),
        "periodStarts" to periodStarts.distinct().sorted().takeLast(240),
        "bfWhatsapp" to bfWhatsapp.trim().take(40),
        "bfEmail" to bfEmail.trim().take(120),
        "careNotePeriod" to careNotePeriod.trim().ifBlank { DEFAULT_CARE_NOTE_PERIOD }.take(500),
        "careNotePms" to careNotePms.trim().ifBlank { DEFAULT_CARE_NOTE_PMS }.take(500),
        "careNoteOvulation" to careNoteOvulation.trim().ifBlank { DEFAULT_CARE_NOTE_OVULATION }.take(500),
        "careNoteNormal" to careNoteNormal.trim().ifBlank { DEFAULT_CARE_NOTE_NORMAL }.take(500),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class DailyLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val date: String = "",
    val periodDay: Boolean = false,
    val flow: String = "නැත (None)",
    val pain: Long = 0L,
    val genSeverity: Long = 0L,
    val waterMl: Long = 0L,
    val moods: List<String> = emptyList(),
    val symptoms: List<String> = emptyList(),
    val genSymptoms: List<String> = emptyList(),
    val cravings: List<String> = emptyList(),
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String, dateStr: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "date" to dateStr,
        "periodDay" to periodDay,
        "flow" to flow.take(60),
        "pain" to pain.coerceIn(0L, 5L),
        "genSeverity" to genSeverity.coerceIn(0L, 5L),
        "waterMl" to waterMl.coerceIn(0L, 10000L),
        "moods" to moods.distinct().take(20),
        "symptoms" to symptoms.distinct().take(20),
        "genSymptoms" to genSymptoms.distinct().take(20),
        "cravings" to cravings.distinct().take(20),
        "note" to note.trim().take(1000),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "periodDay" to periodDay,
        "flow" to flow.take(60),
        "pain" to pain.coerceIn(0L, 5L),
        "genSeverity" to genSeverity.coerceIn(0L, 5L),
        "waterMl" to waterMl.coerceIn(0L, 10000L),
        "moods" to moods.distinct().take(20),
        "symptoms" to symptoms.distinct().take(20),
        "genSymptoms" to genSymptoms.distinct().take(20),
        "cravings" to cravings.distinct().take(20),
        "note" to note.trim().take(1000),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

data class CycleStats(
    val avgCycle: Int,
    val periodDuration: Int,
    val currentCycleDay: Int,
    val daysUntilNextPeriod: Int,
    val nextPeriodDate: String,
    val ovulationDate: String,
    val fertileStart: String,
    val fertileEnd: String,
    val phase: CyclePhase,
    val phaseBadge: String,
    val phaseTitle: String,
    val phaseSub: String,
    val predictedStarts: List<String>,
    val predictedPeriodDays: Set<String>
)

data class MonthlyReportSummary(
    val yearMonth: String,
    val loggedDaysCount: Int,
    val periodDaysCount: Int,
    val maxPain: Int,
    val avgWaterMl: Int,
    val waterLoggedDays: Int,
    val topSymptoms: String,
    val topMoods: String,
    val whatsappText: String
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false,
    val isError: Boolean = false
)

sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Success<T>(val data: T) : DataUiState<T>
    data class Error(val message: String) : DataUiState<Nothing>
}

data class LunaWorkspaceData(
    val profile: UserProfile?,
    val logsByDate: Map<String, DailyLog>
)
