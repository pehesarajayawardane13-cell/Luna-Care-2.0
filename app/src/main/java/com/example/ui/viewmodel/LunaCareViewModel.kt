package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiHealthRepository
import com.example.data.LunaCareRepository
import com.example.domain.BackupSerializer
import com.example.domain.CycleCalculator
import com.example.model.ChatMessage
import com.example.model.DailyLog
import com.example.model.DataUiState
import com.example.model.LunaWorkspaceData
import com.example.model.MainTab
import com.example.model.PetalStyle
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

private const val VM_TAG = "LunaCareVM"

class LunaCareViewModel(
    private val repository: LunaCareRepository,
    private val aiRepository: AiHealthRepository,
    private val currentUserId: String
) : ViewModel() {

    val workspaceState: StateFlow<DataUiState<LunaWorkspaceData>> = combine(
        repository.observeProfile(currentUserId),
        repository.observeDailyLogs(currentUserId)
    ) { profile, logs ->
        LunaWorkspaceData(profile = profile, logsByDate = logs)
    }
        .map<LunaWorkspaceData, DataUiState<LunaWorkspaceData>> { DataUiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(VM_TAG, "Error observing LunaCare data", rootError)
            emit(DataUiState.Error(rootError.localizedMessage ?: "Failed to load data"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DataUiState.Loading
        )

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _currentTab = MutableStateFlow(MainTab.DASHBOARD)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val today = CycleCalculator.getTodayStr()
    private val nowCal = Calendar.getInstance()

    private val _calendarYear = MutableStateFlow(nowCal.get(Calendar.YEAR))
    val calendarYear: StateFlow<Int> = _calendarYear.asStateFlow()

    private val _calendarMonth = MutableStateFlow(nowCal.get(Calendar.MONTH) + 1)
    val calendarMonth: StateFlow<Int> = _calendarMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow(today)
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _loggerDraft = MutableStateFlow(DailyLog(userId = currentUserId, date = today))
    val loggerDraft: StateFlow<DailyLog> = _loggerDraft.asStateFlow()

    private val _selectedReportMonth = MutableStateFlow(today.take(7))
    val selectedReportMonth: StateFlow<String> = _selectedReportMonth.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiStreaming = MutableStateFlow(false)
    val isAiStreaming: StateFlow<Boolean> = _isAiStreaming.asStateFlow()

    private val _redFlagVisible = MutableStateFlow(false)
    val redFlagVisible: StateFlow<Boolean> = _redFlagVisible.asStateFlow()

    private val _toastMessage = MutableStateFlow<Pair<String, String>?>(null)
    val toastMessage: StateFlow<Pair<String, String>?> = _toastMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private var aiStreamJob: Job? = null

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String, icon: String = "🌸") {
        _toastMessage.value = msg to icon
    }

    fun unlockWithWelcome(userName: String) {
        _isUnlocked.value = true
        ensureWelcomeChat(userName)
    }

    fun lockApp() {
        _isUnlocked.value = false
    }

    fun switchTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun selectCalendarDate(dateStr: String) {
        _selectedDate.value = dateStr
    }

    fun changeCalendarMonth(delta: Int) {
        val cal = Calendar.getInstance().apply {
            set(_calendarYear.value, _calendarMonth.value - 1, 1)
            add(Calendar.MONTH, delta)
        }
        _calendarYear.value = cal.get(Calendar.YEAR)
        _calendarMonth.value = cal.get(Calendar.MONTH) + 1
    }

    fun jumpCalendarToToday() {
        val cal = Calendar.getInstance()
        _calendarYear.value = cal.get(Calendar.YEAR)
        _calendarMonth.value = cal.get(Calendar.MONTH) + 1
        _selectedDate.value = CycleCalculator.getTodayStr()
    }

    fun openLoggerForDate(dateStr: String, logsByDate: Map<String, DailyLog>) {
        _selectedDate.value = dateStr
        val existing = logsByDate[dateStr] ?: DailyLog(userId = currentUserId, date = dateStr)
        _loggerDraft.value = existing.copy(date = dateStr, userId = currentUserId)
        _currentTab.value = MainTab.LOGGER
    }

    fun loadLoggerDate(dateStr: String, logsByDate: Map<String, DailyLog>) {
        _selectedDate.value = dateStr
        val existing = logsByDate[dateStr] ?: DailyLog(userId = currentUserId, date = dateStr)
        _loggerDraft.value = existing.copy(date = dateStr, userId = currentUserId)
    }

    fun updateLoggerDraft(transform: (DailyLog) -> DailyLog) {
        _loggerDraft.value = transform(_loggerDraft.value)
    }

    fun setReportMonth(ym: String) {
        _selectedReportMonth.value = ym
    }

    private fun ensureWelcomeChat(userName: String) {
        if (_chatMessages.value.isEmpty()) {
            val display = userName.ifBlank { "ඔබ" }
            _chatMessages.value = listOf(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = "ආයුබෝවන් $display! 🌸 මම ඔබේ AI Health Assistant. අද water intake, period phase සහ රෝග ලක්ෂණ බලලා care tips දෙන්න මම සූදානම්. (මෙය තොරතුරු සඳහා පමණි; වෛද්‍ය උපදෙස් සඳහා වෛද්‍යවරයෙකු හමුවන්න.)"
                )
            )
        }
    }

    fun completeInitialSetup(
        name: String,
        pin: String,
        whatsapp: String,
        email: String,
        lastPeriodDate: String,
        cycleLen: Int,
        periodLen: Int,
        waterGoal: Int
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val starts = if (lastPeriodDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
                listOf(lastPeriodDate)
            } else {
                emptyList()
            }
            val newProfile = UserProfile(
                userId = currentUserId,
                userName = name.trim(),
                setupDone = true,
                pin = pin,
                defaultCycleLength = cycleLen.toLong().coerceIn(20L, 45L),
                defaultPeriodDuration = periodLen.toLong().coerceIn(2L, 10L),
                dailyWaterGoal = waterGoal.toLong().coerceIn(500L, 5000L),
                periodStarts = starts,
                bfWhatsapp = whatsapp.trim(),
                bfEmail = email.trim()
            )
            val profileResult = repository.upsertProfile(currentUserId, newProfile)
            if (profileResult.isSuccess && starts.isNotEmpty()) {
                val periodLog = DailyLog(
                    userId = currentUserId,
                    date = lastPeriodDate,
                    periodDay = true,
                    flow = "සාමාන්‍යයි (Medium)"
                )
                repository.upsertDailyLog(currentUserId, periodLog)
            }
            _isSaving.value = false
            if (profileResult.isSuccess) {
                showToast("LunaCare සාර්ථකව සකසන ලදී! 🌸")
                unlockWithWelcome(newProfile.userName)
            } else {
                showToast(
                    profileResult.exceptionOrNull()?.localizedMessage ?: "සුරැකීම අසාර්ථක විය",
                    "⚠️"
                )
            }
        }
    }

    fun emergencyResetPin() {
        viewModelScope.launch {
            val res = repository.updateProfileFields(currentUserId, mapOf("pin" to "1234"))
            if (res.isSuccess) {
                showToast("PIN අංකය '1234' ලෙස යළි සකසන ලදී", "🔐")
            } else {
                showToast("PIN යළි සැකසීම අසාර්ථක විය", "⚠️")
            }
        }
    }

    fun toggleTheme(currentProfile: UserProfile) {
        val nextTheme = if (currentProfile.themeMode == "dark") "light" else "dark"
        viewModelScope.launch {
            val res = repository.updateProfileFields(currentUserId, mapOf("themeMode" to nextTheme))
            if (res.isSuccess) {
                showToast(
                    if (nextTheme == "dark") "Dark Mode සක්‍රීයයි" else "Light Mode සක්‍රීයයි",
                    if (nextTheme == "dark") "🌙" else "☀️"
                )
            } else {
                showToast("Theme වෙනස් කිරීම අසාර්ථක විය", "⚠️")
            }
        }
    }

    fun adjustTodayWater(deltaMl: Int, logsByDate: Map<String, DailyLog>) {
        val todayStr = CycleCalculator.getTodayStr()
        val currentLog = logsByDate[todayStr] ?: DailyLog(userId = currentUserId, date = todayStr)
        val nextMl = (currentLog.waterMl + deltaMl).coerceIn(0L, 10000L)
        viewModelScope.launch {
            val res = repository.upsertDailyLog(
                currentUserId,
                currentLog.copy(userId = currentUserId, date = todayStr, waterMl = nextMl)
            )
            if (res.isSuccess) {
                if (_selectedDate.value == todayStr) {
                    _loggerDraft.value = _loggerDraft.value.copy(waterMl = nextMl)
                }
                val sign = if (deltaMl >= 0) "+${deltaMl}ml" else "${deltaMl}ml"
                showToast("ජල ප්‍රමාණය $sign ($nextMl ml)", "💧")
            } else {
                showToast("ජල සටහන සුරැකීම අසාර්ථක විය", "⚠️")
            }
        }
    }

    fun markQuickPeriodToday(profile: UserProfile, logsByDate: Map<String, DailyLog>) {
        val todayStr = CycleCalculator.getTodayStr()
        val currentLog = logsByDate[todayStr] ?: DailyLog(
            userId = currentUserId,
            date = todayStr,
            flow = "සාමාන්‍යයි (Medium)",
            pain = 1L
        )
        viewModelScope.launch {
            val updatedStarts = (profile.periodStarts + todayStr).distinct().sorted().takeLast(240)
            val pRes = repository.updateProfileFields(currentUserId, mapOf("periodStarts" to updatedStarts))
            val lRes = repository.upsertDailyLog(
                currentUserId,
                currentLog.copy(userId = currentUserId, date = todayStr, periodDay = true)
            )
            if (pRes.isSuccess && lRes.isSuccess) {
                showToast("අද දිනය ඔසප් ආරම්භක දිනය ලෙස සටහන් විය! 🩸")
            } else {
                showToast("සටහන් කිරීම අසාර්ථක විය", "⚠️")
            }
        }
    }

    fun markLoggerDateAsCycleStart(profile: UserProfile) {
        val targetDate = _loggerDraft.value.date
        val updatedDraft = _loggerDraft.value.copy(periodDay = true)
        _loggerDraft.value = updatedDraft
        viewModelScope.launch {
            val updatedStarts = (profile.periodStarts + targetDate).distinct().sorted().takeLast(240)
            repository.updateProfileFields(currentUserId, mapOf("periodStarts" to updatedStarts))
            val res = repository.upsertDailyLog(currentUserId, updatedDraft)
            if (res.isSuccess) {
                showToast("$targetDate නව ඔසප් චක්‍රයේ ආරම්භය ලෙස සටහන් විය! 🩸")
            } else {
                showToast("සුරැකීම අසාර්ථක විය", "⚠️")
            }
        }
    }

    fun saveCurrentLoggerDraft(
        profile: UserProfile,
        logsByDate: Map<String, DailyLog>,
        silent: Boolean = false,
        onSaved: (String) -> Unit = {}
    ) {
        val draft = _loggerDraft.value
        val dStr = draft.date
        viewModelScope.launch {
            _isSaving.value = true
            if (draft.periodDay) {
                val prevDay = CycleCalculator.addDays(dStr, -1)
                val prevWasPeriod = logsByDate[prevDay]?.periodDay == true
                if (!prevWasPeriod && !profile.periodStarts.contains(dStr)) {
                    val nearExisting = profile.periodStarts.any {
                        kotlin.math.abs(CycleCalculator.diffDays(it, dStr)) <= 3
                    }
                    if (!nearExisting) {
                        val updatedStarts = (profile.periodStarts + dStr).distinct().sorted().takeLast(240)
                        repository.updateProfileFields(currentUserId, mapOf("periodStarts" to updatedStarts))
                    }
                }
            }
            val res = repository.upsertDailyLog(currentUserId, draft.copy(userId = currentUserId))
            _isSaving.value = false
            if (res.isSuccess) {
                if (!silent) showToast("$dStr සටහන සුරකින ලදී! 💾")
                onSaved(dStr)
            } else {
                showToast("සටහන සුරැකීම අසාර්ථක විය", "⚠️")
            }
        }
    }

    fun updatePetalSettings(
        enabled: Boolean? = null,
        style: PetalStyle? = null,
        size: Int? = null,
        opacityPct: Int? = null
    ) {
        val updates = mutableMapOf<String, Any>()
        if (enabled != null) updates["petalsEnabled"] = enabled
        if (style != null) updates["petalStyle"] = style.key
        if (size != null) updates["petalSize"] = size.coerceIn(12, 42).toLong()
        if (opacityPct != null) updates["petalOpacityPct"] = opacityPct.coerceIn(8, 80).toLong()
        if (updates.isEmpty()) return
        viewModelScope.launch {
            repository.updateProfileFields(currentUserId, updates)
        }
    }

    fun saveContacts(whatsapp: String, email: String) {
        viewModelScope.launch {
            val res = repository.updateProfileFields(
                currentUserId,
                mapOf(
                    "bfWhatsapp" to whatsapp.trim().take(40),
                    "bfEmail" to email.trim().take(120)
                )
            )
            if (res.isSuccess) {
                showToast("සම්බන්ධතා විස්තර සුරකින ලදී! 💑")
            } else {
                showToast("සම්බන්ධතා සුරැකීම අසාර්ථක විය", "⚠️")
            }
        }
    }

    fun saveCareNotes(period: String, pms: String, ovulation: String, normal: String) {
        viewModelScope.launch {
            val res = repository.updateProfileFields(
                currentUserId,
                mapOf(
                    "careNotePeriod" to period.trim().take(500),
                    "careNotePms" to pms.trim().take(500),
                    "careNoteOvulation" to ovulation.trim().take(500),
                    "careNoteNormal" to normal.trim().take(500)
                )
            )
            if (res.isSuccess) {
                showToast("Care පණිවිඩ යාවත්කාලීන විය! 💌")
            } else {
                showToast("පණිවිඩ සුරැකීම අසාර්ථක විය", "⚠️")
            }
        }
    }

    fun saveProfileSettings(name: String, waterGoal: Int, newPin: String) {
        if (newPin.isNotBlank() && !newPin.matches(Regex("^\\d{4}$"))) {
            showToast("PIN අංකය ඉලක්කම් 4ක් විය යුතුය!", "⚠️")
            return
        }
        val updates = mutableMapOf<String, Any>()
        if (name.isNotBlank()) updates["userName"] = name.trim().take(80)
        if (waterGoal in 500..5000) updates["dailyWaterGoal"] = waterGoal.toLong()
        if (newPin.matches(Regex("^\\d{4}$"))) updates["pin"] = newPin
        if (updates.isEmpty()) return

        viewModelScope.launch {
            val res = repository.updateProfileFields(currentUserId, updates)
            if (res.isSuccess) {
                showToast("සැකසුම් සාර්ථකව යාවත්කාලීන විය! 🔐")
            } else {
                showToast("සැකසුම් යාවත්කාලීන කිරීම අසාර්ථක විය", "⚠️")
            }
        }
    }

    fun importBackupJson(jsonText: String) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val parsed = BackupSerializer.parseFromJson(jsonText, currentUserId)
                val res = repository.importBackup(currentUserId, parsed.profile, parsed.logs)
                if (res.isSuccess) {
                    showToast("දත්ත සාර්ථකව ප්‍රතිස්ථාපනය විය! ✨")
                    unlockWithWelcome(parsed.profile.userName)
                } else {
                    showToast("Backup ප්‍රතිස්ථාපනය අසාර්ථක විය", "❌")
                }
            } catch (e: Exception) {
                Log.e(VM_TAG, "Backup JSON parse error", e)
                showToast("වැරදි Backup ගොනුවකි!", "❌")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun sendAiPrompt(promptText: String, profile: UserProfile, logsByDate: Map<String, DailyLog>) {
        val cleanPrompt = promptText.trim()
        if (cleanPrompt.isBlank() || _isAiStreaming.value) return

        val todayStr = CycleCalculator.getTodayStr()
        val stats = CycleCalculator.calculateCycleStats(profile, logsByDate)
        val todayLog = logsByDate[todayStr] ?: DailyLog(userId = currentUserId, date = todayStr)

        _redFlagVisible.value = CycleCalculator.hasRedFlagCondition(cleanPrompt, todayLog)

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = cleanPrompt
        )
        val assistantMsgId = UUID.randomUUID().toString()
        val placeholderMsg = ChatMessage(
            id = assistantMsgId,
            isUser = false,
            text = "...",
            isStreaming = true
        )
        _chatMessages.value = _chatMessages.value + userMsg + placeholderMsg
        _isAiStreaming.value = true

        val contextJson = JSONObject().apply {
            put("date", todayStr)
            put("userName", profile.userName)
            put("cycleDay", stats.currentCycleDay)
            put("phase", stats.phaseBadge)
            put("nextPeriodDate", stats.nextPeriodDate)
            put("waterMl", todayLog.waterMl)
            put("waterGoalMl", profile.dailyWaterGoal)
            put("painLevel", todayLog.pain)
            put("generalSeverity", todayLog.genSeverity)
            put("generalSymptoms", JSONArray(todayLog.genSymptoms))
            put("cycleSymptoms", JSONArray(todayLog.symptoms))
            put("moods", JSONArray(todayLog.moods))
        }.toString()

        aiStreamJob?.cancel()
        aiStreamJob = viewModelScope.launch {
            try {
                aiRepository.streamHealthAdvice(contextJson, cleanPrompt).collect { partialText ->
                    updateChatBubble(assistantMsgId, partialText, isStreaming = true, isError = false)
                }
                val finalBubbleText = _chatMessages.value.firstOrNull { it.id == assistantMsgId }?.text.orEmpty()
                updateChatBubble(assistantMsgId, finalBubbleText, isStreaming = false, isError = false)
            } catch (e: TimeoutCancellationException) {
                updateChatBubble(
                    assistantMsgId,
                    "AI පිළිතුර ලබා ගැනීමේ කාලය ඉක්මවා ගියේය (Timeout). කරුණාකර නැවත උත්සාහ කරන්න.",
                    isStreaming = false,
                    isError = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VM_TAG, "AI Assistant error", e)
                updateChatBubble(
                    assistantMsgId,
                    "AI සේවාව සමඟ සම්බන්ධ වීමට නොහැකි විය: ${e.localizedMessage ?: "Network / App Check error"}",
                    isStreaming = false,
                    isError = true
                )
            } finally {
                _isAiStreaming.value = false
            }
        }
    }

    private fun updateChatBubble(id: String, text: String, isStreaming: Boolean, isError: Boolean) {
        _chatMessages.value = _chatMessages.value.map { msg ->
            if (msg.id == id) msg.copy(text = text, isStreaming = isStreaming, isError = isError) else msg
        }
    }
}
