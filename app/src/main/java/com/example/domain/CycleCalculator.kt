package com.example.domain

import com.example.model.CyclePhase
import com.example.model.CycleStats
import com.example.model.DailyLog
import com.example.model.MonthlyReportSummary
import com.example.model.UserProfile
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

object CycleCalculator {

    val FLOW_OPTIONS = listOf(
        "නැත (None)",
        "බිංදු කිහිපයක් (Spotting)",
        "අඩුයි (Light)",
        "සාමාන්‍යයි (Medium)",
        "වැඩියි (Heavy)"
    )

    val PAIN_DESCRIPTIONS = mapOf(
        0 to "0 - වේදනාවක් නැත",
        1 to "1 - සුළු අපහසුතාවක්",
        2 to "2 - මධ්‍යස්ථ වේදනාවක්",
        3 to "3 - සැලකිය යුතු වේදනාවක්",
        4 to "4 - තදබල වේදනාවක්",
        5 to "5 - ඉතා තදබල වේදනාවක්"
    )

    val GENERAL_SYMPTOM_OPTIONS = listOf(
        "හිසරදය (Headache)" to "🤕 හිසරදය",
        "උණ / සෙම්ප්‍රතිශ්‍යාව (Fever/Cold)" to "🤧 උණ/සෙම",
        "බඩේ අමාරුව / ගැස්ට්‍රයිටිස් (Stomach)" to "🤢 බඩේ අමාරුව",
        "ඇඟේ මහන්සිය (Fatigue)" to "😴 මහන්සිය",
        "කරකැවිල්ල (Dizziness)" to "💫 කරකැවිල්ල",
        "උගුරේ අමාරුව (Sore Throat)" to "🗣️ උගුරේ අමාරුව"
    )

    val MOOD_OPTIONS = listOf(
        "සතුටින් 😊",
        "ආදරණීය 🥰",
        "සන්සුන් 😌",
        "තරහින් / Irritated 😤",
        "දුකෙන් / Sensitive 🥺",
        "වෙහෙසට පත්ව 🥱"
    )

    val CYCLE_SYMPTOM_OPTIONS = listOf(
        "බඩේ කැක්කුම (Cramps)" to "බඩේ කැක්කුම",
        "කොන්දේ කැක්කුම (Backache)" to "කොන්දේ කැක්කුම",
        "පියයුරු සංවේදීතාව" to "පියයුරු සංවේදීතාව",
        "Bloating" to "Bloating"
    )

    val CRAVING_OPTIONS = listOf(
        "චොකලට් 🍫",
        "අයිස්ක්‍රීම් 🍨",
        "උණුසුම් බීම ☕",
        "පලතුරු / Snacks 🍓"
    )

    private val SINHALA_SHORT_MONTHS = listOf(
        "ජන", "පෙබ", "මාර්", "අප්‍රේ", "මැයි", "ජූනි",
        "ජූලි", "අගෝ", "සැප්", "ඔක්", "නොවැ", "දෙසැ"
    )

    val SINHALA_FULL_MONTHS = listOf(
        "ජනවාරි", "පෙබරවාරි", "මාර්තු", "අප්‍රේල්", "මැයි", "ජූනි",
        "ජූලි", "අගෝස්තු", "සැප්තැම්බර්", "ඔක්තෝබර්", "නොවැම්බර්", "දෙසැම්බර්"
    )

    fun getTodayStr(): String {
        val cal = Calendar.getInstance()
        return formatYmd(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    fun formatYmd(year: Int, month1to12: Int, day: Int): String =
        String.format(Locale.US, "%04d-%02d-%02d", year, month1to12, day)

    fun parseYmd(dateStr: String): Triple<Int, Int, Int> {
        val parts = dateStr.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size == 3) {
            return Triple(parts[0], parts[1].coerceIn(1, 12), parts[2].coerceIn(1, 31))
        }
        val today = getTodayStr().split("-").map { it.toInt() }
        return Triple(today[0], today[1], today[2])
    }

    fun addDays(dateStr: String, days: Int): String {
        val (y, m, d) = parseYmd(dateStr)
        val cal = Calendar.getInstance()
        cal.set(y, m - 1, d, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_MONTH, days)
        return formatYmd(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    fun diffDays(startStr: String, endStr: String): Int {
        val (y1, m1, d1) = parseYmd(startStr)
        val (y2, m2, d2) = parseYmd(endStr)
        val c1 = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(y1, m1 - 1, d1)
        }
        val c2 = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(y2, m2 - 1, d2)
        }
        return ((c2.timeInMillis - c1.timeInMillis) / 86_400_000L).toInt()
    }

    fun formatReadableSinhalaDate(dateStr: String): String {
        val (y, m, d) = parseYmd(dateStr)
        val monthLabel = SINHALA_SHORT_MONTHS.getOrElse(m - 1) { "" }
        return "$y $monthLabel $d"
    }

    fun getAllPeriodStartDates(profile: UserProfile, logsByDate: Map<String, DailyLog>): List<String> {
        val set = profile.periodStarts.toMutableSet()
        logsByDate.forEach { (dateStr, log) ->
            if (log.periodDay) {
                val prevDay = addDays(dateStr, -1)
                val prevLogged = logsByDate[prevDay]?.periodDay == true
                if (!prevLogged) {
                    val hasNearbyStart = set.any { abs(diffDays(it, dateStr)) <= 3 }
                    if (!hasNearbyStart) set.add(dateStr)
                }
            }
        }
        return set.filter { it.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$")) }.sorted()
    }

    fun calculateCycleStats(profile: UserProfile, logsByDate: Map<String, DailyLog>): CycleStats {
        val starts = getAllPeriodStartDates(profile, logsByDate)
        val today = getTodayStr()
        val fallbackStart = starts.lastOrNull() ?: today

        var avgCycle = profile.defaultCycleLength.toInt().coerceIn(20, 45)
        if (starts.size >= 2) {
            val diffs = mutableListOf<Int>()
            for (i in 1 until starts.size) {
                val d = diffDays(starts[i - 1], starts[i])
                if (d in 18..50) diffs.add(d)
            }
            if (diffs.isNotEmpty()) {
                avgCycle = diffs.average().roundToInt().coerceIn(18, 50)
            }
        }

        val periodDuration = profile.defaultPeriodDuration.toInt().coerceIn(2, 10)
        var anchorStart = fallbackStart
        while (addDays(anchorStart, avgCycle) <= addDays(today, -periodDuration)) {
            anchorStart = addDays(anchorStart, avgCycle)
        }

        val nextPeriodDate = if (diffDays(anchorStart, today) >= avgCycle) {
            addDays(anchorStart, avgCycle)
        } else {
            anchorStart
        }

        val prevCycleStart = if (starts.isNotEmpty()) {
            if (diffDays(anchorStart, today) >= avgCycle) {
                anchorStart
            } else if (starts.size >= 2 && diffDays(starts.last(), today) >= 0) {
                starts.last()
            } else {
                addDays(nextPeriodDate, -avgCycle)
            }
        } else {
            addDays(nextPeriodDate, -avgCycle)
        }

        val daysSincePrevStart = max(0, diffDays(prevCycleStart, today))
        val currentCycleDay = (daysSincePrevStart % avgCycle) + 1
        val daysUntilNextPeriod = diffDays(today, nextPeriodDate)

        val ovulationDate = addDays(nextPeriodDate, -14)
        val fertileStart = addDays(ovulationDate, -4)
        val fertileEnd = addDays(ovulationDate, 1)

        val isTodayLoggedPeriod = logsByDate[today]?.periodDay == true
        val inPredictedPeriodWindow = diffDays(prevCycleStart, today) in 0 until periodDuration

        val phase: CyclePhase
        val phaseBadge: String
        val phaseTitle: String
        val phaseSub: String

        when {
            isTodayLoggedPeriod || inPredictedPeriodWindow -> {
                phase = CyclePhase.PERIOD
                phaseBadge = "🩸 ඔසප් කාලය (Period Days)"
                phaseTitle = "ඔසප් දින කාලය"
                phaseSub = "විවේකය සහ උණුසුම් ජලය පානය කිරීම වැදගත් වේ."
            }
            daysUntilNextPeriod in 0..5 -> {
                phase = CyclePhase.PMS
                phaseBadge = "🌷 PMS අවධිය (Pre-Period)"
                phaseTitle = "ඔසප් වීමට තව දින $daysUntilNextPeriod"
                phaseSub = "අනුමාන දිනය: ${formatReadableSinhalaDate(nextPeriodDate)}"
            }
            diffDays(fertileStart, today) >= 0 && diffDays(today, fertileEnd) >= 0 -> {
                phase = CyclePhase.OVULATION
                phaseBadge = "🥚 සරු කාලය / Ovulation"
                phaseTitle = if (today == ovulationDate) "අද ඩිම්බ මෝචන දිනයයි" else "සරු කාලය (Fertile Window)"
                phaseSub = "අනුමාන Ovulation දිනය: ${formatReadableSinhalaDate(ovulationDate)}"
            }
            else -> {
                phase = CyclePhase.NORMAL
                phaseBadge = "🌿 සාමාන්‍ය අවධිය (Normal)"
                phaseTitle = if (daysUntilNextPeriod > 0) {
                    "මීළඟ ඔසප් දිනයට තව දින $daysUntilNextPeriod"
                } else {
                    "චක්‍රය යාවත්කාලීන වෙමින්"
                }
                phaseSub = "අනුමාන දිනය: ${formatReadableSinhalaDate(nextPeriodDate)}"
            }
        }

        val predictedStartsSet = starts.toMutableSet()
        var cursor = addDays(nextPeriodDate, -avgCycle * 6)
        val horizonEnd = addDays(today, 365 * 3)
        while (cursor <= horizonEnd) {
            predictedStartsSet.add(cursor)
            cursor = addDays(cursor, avgCycle)
        }
        val predictedStarts = predictedStartsSet.sorted()

        val predictedDays = mutableSetOf<String>()
        predictedStarts.forEach { startStr ->
            for (d in 0 until periodDuration) {
                predictedDays.add(addDays(startStr, d))
            }
        }

        return CycleStats(
            avgCycle = avgCycle,
            periodDuration = periodDuration,
            currentCycleDay = currentCycleDay,
            daysUntilNextPeriod = daysUntilNextPeriod,
            nextPeriodDate = nextPeriodDate,
            ovulationDate = ovulationDate,
            fertileStart = fertileStart,
            fertileEnd = fertileEnd,
            phase = phase,
            phaseBadge = phaseBadge,
            phaseTitle = phaseTitle,
            phaseSub = phaseSub,
            predictedStarts = predictedStarts,
            predictedPeriodDays = predictedDays
        )
    }

    fun buildDailySummaryText(
        profile: UserProfile,
        logsByDate: Map<String, DailyLog>,
        dateStr: String
    ): String {
        val stats = calculateCycleStats(profile, logsByDate)
        val log = logsByDate[dateStr] ?: DailyLog(date = dateStr)
        val waterMl = log.waterMl.toInt()
        val goalMl = profile.dailyWaterGoal.toInt().coerceAtLeast(500)
        val waterPct = ((waterMl * 100f) / goalMl).roundToInt().coerceIn(0, 100)

        val genSymText = if (log.genSymptoms.isNotEmpty()) {
            "${log.genSymptoms.joinToString(", ")} (Severity ${log.genSeverity}/5)"
        } else {
            "නැත"
        }

        val lines = mutableListOf(
            "🌸 *LunaCare Daily Health & Care Report*",
            "👤 නම: ${profile.userName.ifBlank { "User" }} | 📅 දිනය: $dateStr",
            "🔄 චක්‍ර දිනය: Day ${stats.currentCycleDay} | ${stats.phaseBadge}",
            "🗓️ මීළඟ Period (අනුමාන): ${stats.nextPeriodDate} (තව දින ${max(0, stats.daysUntilNextPeriod)})",
            "💧 Water Intake: ${waterMl}ml / ${goalMl}ml ($waterPct%)",
            "🩸 Period Status: ${if (log.periodDay) "ඔව්" else "නැත"} | Flow: ${log.flow.ifBlank { "නැත (None)" }}",
            "😣 Pain Level: ${log.pain}/5",
            "🩺 General Symptoms: $genSymText",
            "😊 Moods: ${if (log.moods.isNotEmpty()) log.moods.joinToString(", ") else "සඳහන් කර නැත"}",
            "🍫 Cravings: ${if (log.cravings.isNotEmpty()) log.cravings.joinToString(", ") else "නැත"}"
        )
        if (log.note.isNotBlank()) {
            lines.add("📝 Note: ${log.note}")
        }
        lines.add("— LunaCare Care Update")
        return lines.joinToString("\n")
    }

    fun buildMonthlyReport(
        profile: UserProfile,
        logsByDate: Map<String, DailyLog>,
        yearMonth: String
    ): MonthlyReportSummary {
        val monthDates = logsByDate.keys.filter { it.startsWith(yearMonth) }.sorted()
        var periodDaysCount = 0
        var totalWaterMl = 0
        var waterLoggedDays = 0
        var maxPain = 0
        val symptomCounts = mutableMapOf<String, Int>()
        val moodCounts = mutableMapOf<String, Int>()

        monthDates.forEach { d ->
            val log = logsByDate[d] ?: return@forEach
            if (log.periodDay) periodDaysCount++
            val w = log.waterMl.toInt()
            if (w > 0) {
                totalWaterMl += w
                waterLoggedDays++
            }
            if (log.pain > maxPain) maxPain = log.pain.toInt()
            (log.genSymptoms + log.symptoms).forEach { s ->
                symptomCounts[s] = (symptomCounts[s] ?: 0) + 1
            }
            log.moods.forEach { m ->
                moodCounts[m] = (moodCounts[m] ?: 0) + 1
            }
        }

        val avgWater = if (waterLoggedDays > 0) (totalWaterMl.toDouble() / waterLoggedDays).roundToInt() else 0
        val topSymptoms = symptomCounts.entries
            .sortedByDescending { it.value }
            .take(3)
            .joinToString(", ") { "${it.key} (${it.value})" }
            .ifBlank { "සටහන් නැත" }
        val topMoods = moodCounts.entries
            .sortedByDescending { it.value }
            .take(3)
            .joinToString(", ") { "${it.key} (${it.value})" }
            .ifBlank { "සටහන් නැත" }

        val waText = listOf(
            "📊 *LunaCare Monthly Report ($yearMonth)*",
            "👤 ${profile.userName.ifBlank { "User" }}",
            "🩸 Period Days: $periodDaysCount | 😣 Peak Pain: $maxPain/5",
            "💧 Avg Water: ${avgWater}ml/day ($waterLoggedDays logged days)",
            "🩺 Top Symptoms: $topSymptoms",
            "😊 Top Moods: $topMoods"
        ).joinToString("\n")

        return MonthlyReportSummary(
            yearMonth = yearMonth,
            loggedDaysCount = monthDates.size,
            periodDaysCount = periodDaysCount,
            maxPain = maxPain,
            avgWaterMl = avgWater,
            waterLoggedDays = waterLoggedDays,
            topSymptoms = topSymptoms,
            topMoods = topMoods,
            whatsappText = waText
        )
    }

    fun hasRedFlagCondition(promptText: String, log: DailyLog?): Boolean {
        val severePain = (log?.pain ?: 0L) >= 4L || (log?.genSeverity ?: 0L) >= 4L
        val redWords = listOf("fever", "උණ", "chest pain", "breath", "හුස්ම", "faint", "කරකැවිල්ල", "severe", "තදබල", "bleeding", "ලේ")
        val lower = promptText.lowercase(Locale.getDefault())
        return severePain || redWords.any { lower.contains(it) }
    }
}
