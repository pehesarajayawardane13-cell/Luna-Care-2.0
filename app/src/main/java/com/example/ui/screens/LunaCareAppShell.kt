package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.AiHealthRepository
import com.example.data.LunaCareRepository
import com.example.domain.CycleCalculator
import com.example.domain.ShareIntents
import com.example.firebase.authStateFlow
import com.example.firebase.signOutUser
import com.example.model.DataUiState
import com.example.model.DailyLog
import com.example.model.LunaWorkspaceData
import com.example.model.MainTab
import com.example.model.PetalStyle
import com.example.model.UserProfile
import com.example.ui.components.FloatingPetalsBackground
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkBgBottom
import com.example.ui.theme.DarkBgMid
import com.example.ui.theme.DarkBgTop
import com.example.ui.theme.LightBgBottom
import com.example.ui.theme.LightBgTop
import com.example.ui.theme.LunaCareTheme
import com.example.ui.theme.RosePrimary
import com.example.ui.viewmodel.LunaCareViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    val scope = rememberCoroutineScope()

    if (user == null) {
        LunaCareTheme(darkTheme = false) {
            LunaAtmosphereContainer(
                isDark = false,
                petalsEnabled = true,
                petalStyle = PetalStyle.SAKURA,
                petalSizeDp = 22,
                petalOpacityPct = 34
            ) {
                AuthScreen(
                    onAuthSuccess = {
                        scope.launch {
                            try {
                                auth.signInAnonymously().await()
                            } catch (_: Exception) {
                                // Anonymous sign-in enable කර නැතිනම් හෝ network ගැටලුවක් නම් මෙතැනට එයි
                            }
                        }
                    }
                )
            }
        }
    } else {
        key(user.uid) {
            val viewModel: LunaCareViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        LunaCareViewModel(
                            repository = LunaCareRepository(db),
                            aiRepository = AiHealthRepository(),
                            currentUserId = user.uid
                        )
                    }
                }
            )
            AuthenticatedLunaCareRoot(
                userEmail = user.email.orEmpty(),
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun AuthenticatedLunaCareRoot(
    userEmail: String,
    viewModel: LunaCareViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val workspaceState by viewModel.workspaceState.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val calYear by viewModel.calendarYear.collectAsStateWithLifecycle()
    val calMonth by viewModel.calendarMonth.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val loggerDraft by viewModel.loggerDraft.collectAsStateWithLifecycle()
    val selectedReportMonth by viewModel.selectedReportMonth.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isAiStreaming by viewModel.isAiStreaming.collectAsStateWithLifecycle()
    val redFlagVisible by viewModel.redFlagVisible.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2600)
            viewModel.clearToast()
        }
    }

    val activeProfile = (workspaceState as? DataUiState.Success)?.data?.profile
    val isDark = activeProfile?.isDarkTheme ?: false
    val petalsEnabled = activeProfile?.petalsEnabled ?: true
    val petalStyle = PetalStyle.fromKey(activeProfile?.petalStyle)
    val petalSize = (activeProfile?.petalSize ?: 22L).toInt()
    val petalOpacity = (activeProfile?.petalOpacityPct ?: 34L).toInt()

    fun triggerSignOut() {
        signOutUser(credentialManager = credentialManager, scope = scope)
    }

    LunaCareTheme(darkTheme = isDark) {
        LunaAtmosphereContainer(
            isDark = isDark,
            petalsEnabled = petalsEnabled,
            petalStyle = petalStyle,
            petalSizeDp = petalSize,
            petalOpacityPct = petalOpacity
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = workspaceState) {
                    is DataUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = RosePrimary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "LunaCare සූදානම් වෙමින්...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is DataUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassCard(modifier = Modifier.widthIn(max = 420.dp)) {
                                Text(
                                    text = "දත්ත ලබාගැනීමේ දෝෂයක්",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = { triggerSignOut() }) {
                                    Text("Sign Out")
                                }
                            }
                        }
                    }

                    is DataUiState.Success -> {
                        val data: LunaWorkspaceData = state.data
                        val profile = data.profile

                        if (profile == null || !profile.setupDone) {
                            SetupScreen(
                                isSaving = isSaving,
                                onSubmitSetup = { name, pin, wa, em, lastDate, cycleLen, periodLen, waterGoal ->
                                    viewModel.completeInitialSetup(
                                        name, pin, wa, em, lastDate, cycleLen, periodLen, waterGoal
                                    )
                                },
                                onImportBackupJson = { json -> viewModel.importBackupJson(json) },
                                onShowError = { msg -> viewModel.showToast(msg, "⚠️") }
                            )
                        } else if (!isUnlocked) {
                            PinLockScreen(
                                userName = profile.userName,
                                expectedPin = profile.pin,
                                onUnlockSuccess = { viewModel.unlockWithWelcome(profile.userName) },
                                onEmergencyResetPin = { viewModel.emergencyResetPin() },
                                onSignOutClick = { triggerSignOut() }
                            )
                        } else {
                            // Back button returns from secondary tabs to Dashboard
                            BackHandler(enabled = currentTab != MainTab.DASHBOARD) {
                                viewModel.switchTab(MainTab.DASHBOARD)
                            }

                            MainLunaCareShell(
                                profile = profile,
                                logsByDate = data.logsByDate,
                                userEmail = userEmail,
                                currentTab = currentTab,
                                calYear = calYear,
                                calMonth = calMonth,
                                selectedDate = selectedDate,
                                loggerDraft = loggerDraft,
                                selectedReportMonth = selectedReportMonth,
                                chatMessages = chatMessages,
                                isAiStreaming = isAiStreaming,
                                redFlagVisible = redFlagVisible,
                                isSaving = isSaving,
                                onSelectTab = { tab ->
                                    if (tab == MainTab.LOGGER) {
                                        viewModel.loadLoggerDate(selectedDate, data.logsByDate)
                                    }
                                    viewModel.switchTab(tab)
                                },
                                onToggleTheme = { viewModel.toggleTheme(profile) },
                                onLockApp = { viewModel.lockApp() },
                                onQuickPeriodToday = { viewModel.markQuickPeriodToday(profile, data.logsByDate) },
                                onQuickLogToday = {
                                    viewModel.openLoggerForDate(CycleCalculator.getTodayStr(), data.logsByDate)
                                },
                                onAdjustWater = { delta -> viewModel.adjustTodayWater(delta, data.logsByDate) },
                                onShareDailyWhatsApp = { dateStr ->
                                    val txt = CycleCalculator.buildDailySummaryText(profile, data.logsByDate, dateStr)
                                    ShareIntents.sendViaWhatsApp(context, profile.bfWhatsapp, txt) { err ->
                                        viewModel.showToast(err, "⚠️")
                                    }
                                },
                                onShareDailyEmail = { dateStr ->
                                    val body = CycleCalculator.buildDailySummaryText(profile, data.logsByDate, dateStr)
                                    val subject = "LunaCare Health Summary (${profile.userName.ifBlank { "User" }} - $dateStr)"
                                    ShareIntents.sendViaEmail(context, profile.bfEmail, subject, body) { err ->
                                        viewModel.showToast(err, "⚠️")
                                    }
                                },
                                onShareRawWhatsApp = { text ->
                                    ShareIntents.sendViaWhatsApp(context, profile.bfWhatsapp, text) { err ->
                                        viewModel.showToast(err, "⚠️")
                                    }
                                },
                                onPrevMonth = { viewModel.changeCalendarMonth(-1) },
                                onNextMonth = { viewModel.changeCalendarMonth(1) },
                                onJumpToday = { viewModel.jumpCalendarToToday() },
                                onSelectCalendarDate = { d -> viewModel.selectCalendarDate(d) },
                                onEditDateInLogger = { d -> viewModel.openLoggerForDate(d, data.logsByDate) },
                                onLoggerDatePicked = { d -> viewModel.loadLoggerDate(d, data.logsByDate) },
                                onUpdateLoggerDraft = { fn -> viewModel.updateLoggerDraft(fn) },
                                onMarkCycleStart = { viewModel.markLoggerDateAsCycleStart(profile) },
                                onSaveLog = { viewModel.saveCurrentLoggerDraft(profile, data.logsByDate) },
                                onSaveAndWhatsApp = {
                                    viewModel.saveCurrentLoggerDraft(profile, data.logsByDate, silent = true) { savedDate ->
                                        val updatedMap = data.logsByDate + (savedDate to loggerDraft)
                                        val txt = CycleCalculator.buildDailySummaryText(profile, updatedMap, savedDate)
                                        ShareIntents.sendViaWhatsApp(context, profile.bfWhatsapp, txt) { err ->
                                            viewModel.showToast(err, "⚠️")
                                        }
                                    }
                                },
                                onSaveAndEmail = {
                                    viewModel.saveCurrentLoggerDraft(profile, data.logsByDate, silent = true) { savedDate ->
                                        val updatedMap = data.logsByDate + (savedDate to loggerDraft)
                                        val body = CycleCalculator.buildDailySummaryText(profile, updatedMap, savedDate)
                                        val subject = "LunaCare Health Summary (${profile.userName.ifBlank { "User" }} - $savedDate)"
                                        ShareIntents.sendViaEmail(context, profile.bfEmail, subject, body) { err ->
                                            viewModel.showToast(err, "⚠️")
                                        }
                                    }
                                },
                                onSendAiPrompt = { prompt ->
                                    viewModel.sendAiPrompt(prompt, profile, data.logsByDate)
                                },
                                onChangeReportMonth = { ym -> viewModel.setReportMonth(ym) },
                                onUpdatePetals = { en, st, sz, op -> viewModel.updatePetalSettings(en, st, sz, op) },
                                onSaveContacts = { wa, em -> viewModel.saveContacts(wa, em) },
                                onSaveCareNotes = { p, pms, ov, norm -> viewModel.saveCareNotes(p, pms, ov, norm) },
                                onSaveProfile = { nm, wg, pin -> viewModel.saveProfileSettings(nm, wg, pin) },
                                onImportBackupJson = { json -> viewModel.importBackupJson(json) },
                                onShowToast = { m, ic -> viewModel.showToast(m, ic) },
                                onSignOut = { triggerSignOut() }
                            )
                        }
                    }
                }

                // Floating Toast Banner
                AnimatedVisibility(
                    visible = toastMessage != null,
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 12.dp, start = 20.dp, end = 20.dp)
                ) {
                    toastMessage?.let { (msg, icon) ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            border = BorderStroke(1.dp, RosePrimary.copy(alpha = 0.35f)),
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(icon, fontSize = 16.sp)
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LunaAtmosphereContainer(
    isDark: Boolean,
    petalsEnabled: Boolean,
    petalStyle: PetalStyle,
    petalSizeDp: Int,
    petalOpacityPct: Int,
    content: @Composable () -> Unit
) {
    val bgBrush = if (isDark) {
        Brush.verticalGradient(listOf(DarkBgTop, DarkBgMid, DarkBgBottom))
    } else {
        Brush.verticalGradient(listOf(LightBgTop, LightBgBottom))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        FloatingPetalsBackground(
            enabled = petalsEnabled,
            style = petalStyle,
            sizeDp = petalSizeDp,
            opacityPct = petalOpacityPct,
            isDark = isDark
        )
        content()
    }
}

@Composable
private fun MainLunaCareShell(
    profile: UserProfile,
    logsByDate: Map<String, DailyLog>,
    userEmail: String,
    currentTab: MainTab,
    calYear: Int,
    calMonth: Int,
    selectedDate: String,
    loggerDraft: DailyLog,
    selectedReportMonth: String,
    chatMessages: List<com.example.model.ChatMessage>,
    isAiStreaming: Boolean,
    redFlagVisible: Boolean,
    isSaving: Boolean,
    onSelectTab: (MainTab) -> Unit,
    onToggleTheme: () -> Unit,
    onLockApp: () -> Unit,
    onQuickPeriodToday: () -> Unit,
    onQuickLogToday: () -> Unit,
    onAdjustWater: (Int) -> Unit,
    onShareDailyWhatsApp: (String) -> Unit,
    onShareDailyEmail: (String) -> Unit,
    onShareRawWhatsApp: (String) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToday: () -> Unit,
    onSelectCalendarDate: (String) -> Unit,
    onEditDateInLogger: (String) -> Unit,
    onLoggerDatePicked: (String) -> Unit,
    onUpdateLoggerDraft: ((DailyLog) -> DailyLog) -> Unit,
    onMarkCycleStart: () -> Unit,
    onSaveLog: () -> Unit,
    onSaveAndWhatsApp: () -> Unit,
    onSaveAndEmail: () -> Unit,
    onSendAiPrompt: (String) -> Unit,
    onChangeReportMonth: (String) -> Unit,
    onUpdatePetals: (Boolean?, PetalStyle?, Int?, Int?) -> Unit,
    onSaveContacts: (String, String) -> Unit,
    onSaveCareNotes: (String, String, String, String) -> Unit,
    onSaveProfile: (String, Int, String) -> Unit,
    onImportBackupJson: (String) -> Unit,
    onShowToast: (String, String) -> Unit,
    onSignOut: () -> Unit
) {
    val todayStr = CycleCalculator.getTodayStr()
    val stats = remember(profile, logsByDate) {
        CycleCalculator.calculateCycleStats(profile, logsByDate)
    }
    val todayLog = logsByDate[todayStr]

    val tabIcons = mapOf(
        MainTab.DASHBOARD to "🏠",
        MainTab.CALENDAR to "📅",
        MainTab.LOGGER to "✍️",
        MainTab.ASSISTANT to "🤖",
        MainTab.INSIGHTS to "📊",
        MainTab.SETTINGS to "⚙️"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                LunaTopBar(
                    userName = profile.userName,
                    todayReadable = CycleCalculator.formatReadableSinhalaDate(todayStr),
                    isDark = profile.isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onLockApp = onLockApp
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    LunaBottomNavBar(
                        currentTab = currentTab,
                        tabIcons = tabIcons,
                        onSelectTab = onSelectTab
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        MainTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = currentTab == tab,
                                onClick = { onSelectTab(tab) },
                                icon = { Text(tabIcons[tab] ?: "•", fontSize = 18.sp) },
                                label = { Text(tab.title, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        when (currentTab) {
                            MainTab.DASHBOARD -> DashboardTab(
                                profile = profile,
                                stats = stats,
                                todayLog = todayLog,
                                onQuickPeriodToday = onQuickPeriodToday,
                                onQuickLogToday = onQuickLogToday,
                                onAdjustWater = onAdjustWater,
                                onShareWhatsApp = { onShareDailyWhatsApp(todayStr) },
                                onShareEmail = { onShareDailyEmail(todayStr) }
                            )

                            MainTab.CALENDAR -> CalendarTab(
                                year = calYear,
                                month = calMonth,
                                selectedDate = selectedDate,
                                stats = stats,
                                logsByDate = logsByDate,
                                onPrevMonth = onPrevMonth,
                                onNextMonth = onNextMonth,
                                onJumpToday = onJumpToday,
                                onSelectDate = onSelectCalendarDate,
                                onEditDateInLogger = onEditDateInLogger
                            )

                            MainTab.LOGGER -> LoggerTab(
                                draft = loggerDraft,
                                isSaving = isSaving,
                                onDatePicked = onLoggerDatePicked,
                                onUpdateDraft = onUpdateLoggerDraft,
                                onMarkCycleStart = onMarkCycleStart,
                                onSaveLog = onSaveLog,
                                onSaveAndWhatsApp = onSaveAndWhatsApp,
                                onSaveAndEmail = onSaveAndEmail
                            )

                            MainTab.ASSISTANT -> AssistantTab(
                                stats = stats,
                                todayWaterMl = (todayLog?.waterMl ?: 0L).toInt(),
                                chatMessages = chatMessages,
                                isAiStreaming = isAiStreaming,
                                showRedFlagBanner = redFlagVisible,
                                onSendPrompt = onSendAiPrompt,
                                onShareMessageToWhatsApp = { msgText ->
                                    val formatted = "🤖 *LunaCare AI Assistant Note (${profile.userName.ifBlank { "User" }})*\n\n$msgText"
                                    onShareRawWhatsApp(formatted)
                                }
                            )

                            MainTab.INSIGHTS -> InsightsTab(
                                profile = profile,
                                logsByDate = logsByDate,
                                selectedYearMonth = selectedReportMonth,
                                onChangeReportMonth = onChangeReportMonth,
                                onShareDailyWhatsApp = { onShareDailyWhatsApp(todayStr) },
                                onShareMonthlyWhatsApp = onShareRawWhatsApp
                            )

                            MainTab.SETTINGS -> SettingsTab(
                                profile = profile,
                                logsByDate = logsByDate,
                                userEmailDisplay = userEmail,
                                onUpdatePetals = onUpdatePetals,
                                onSaveContacts = onSaveContacts,
                                onSaveCareNotes = onSaveCareNotes,
                                onToggleTheme = onToggleTheme,
                                onSaveProfile = onSaveProfile,
                                onImportBackupJson = onImportBackupJson,
                                onShowToast = onShowToast,
                                onSignOut = onSignOut
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LunaTopBar(
    userName: String,
    todayReadable: String,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onLockApp: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.lunacare_icon_1795071670324),
                    contentDescription = "LunaCare Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = buildAnnotatedString {
                            append("Luna")
                            withStyle(SpanStyle(color = RosePrimary)) { append("Care") }
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Text(
                        text = "${userName.ifBlank { "LunaCare" }} • $todayReadable",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("top_theme_toggle"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = if (isDark) "☀️" else "🌙",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Surface(
                        onClick = onLockApp,
                        modifier = Modifier.testTag("top_lock_button"),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent
                    ) {
                        Text(
                            text = "🔒",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LunaBottomNavBar(
    currentTab: MainTab,
    tabIcons: Map<MainTab, String>,
    onSelectTab: (MainTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MainTab.entries.forEach { tab ->
                    val active = currentTab == tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onSelectTab(tab) }
                            .testTag("nav_tab_${tab.name.lowercase()}"),
                        shape = RoundedCornerShape(18.dp),
                        color = if (active) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = tabIcons[tab] ?: "•",
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.title,
                                fontSize = 9.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) RosePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
