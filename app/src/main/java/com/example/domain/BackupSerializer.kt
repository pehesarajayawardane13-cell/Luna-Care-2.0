package com.example.domain

import com.example.model.DEFAULT_CARE_NOTE_NORMAL
import com.example.model.DEFAULT_CARE_NOTE_OVULATION
import com.example.model.DEFAULT_CARE_NOTE_PERIOD
import com.example.model.DEFAULT_CARE_NOTE_PMS
import com.example.model.DailyLog
import com.example.model.PetalStyle
import com.example.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject

data class ParsedBackup(
    val profile: UserProfile,
    val logs: Map<String, DailyLog>
)

object BackupSerializer {

    fun exportToJson(profile: UserProfile, logsByDate: Map<String, DailyLog>): String {
        val root = JSONObject()
        root.put("isSetupDone", true)
        root.put("themeMode", profile.themeMode)
        root.put("petalSettings", JSONObject().apply {
            put("enabled", profile.petalsEnabled)
            put("style", profile.petalStyle)
            put("size", profile.petalSize)
            put("opacityPct", profile.petalOpacityPct)
        })
        root.put("userName", profile.userName)
        root.put("pin", profile.pin)
        root.put("defaultCycleLength", profile.defaultCycleLength)
        root.put("defaultPeriodDuration", profile.defaultPeriodDuration)
        root.put("dailyWaterGoal", profile.dailyWaterGoal)
        root.put("periodStarts", JSONArray(profile.periodStarts))
        root.put("bfWhatsapp", profile.bfWhatsapp)
        root.put("bfEmail", profile.bfEmail)
        root.put("careNotes", JSONObject().apply {
            put("period", profile.careNotePeriod)
            put("pms", profile.careNotePms)
            put("ovulation", profile.careNoteOvulation)
            put("normal", profile.careNoteNormal)
        })

        val dailyLogsObj = JSONObject()
        val waterLogsObj = JSONObject()
        logsByDate.toSortedMap().forEach { (dateStr, log) ->
            dailyLogsObj.put(dateStr, JSONObject().apply {
                put("isPeriod", log.periodDay)
                put("flow", log.flow)
                put("pain", log.pain)
                put("genSeverity", log.genSeverity)
                put("moods", JSONArray(log.moods))
                put("symptoms", JSONArray(log.symptoms))
                put("genSymptoms", JSONArray(log.genSymptoms))
                put("cravings", JSONArray(log.cravings))
                put("note", log.note)
                put("waterMl", log.waterMl)
            })
            if (log.waterMl > 0) {
                waterLogsObj.put(dateStr, log.waterMl)
            }
        }
        root.put("dailyLogs", dailyLogsObj)
        root.put("waterLogs", waterLogsObj)
        return root.toString(2)
    }

    fun parseFromJson(jsonText: String, uid: String): ParsedBackup {
        val root = JSONObject(jsonText)
        val petalObj = root.optJSONObject("petalSettings")
        val notesObj = root.optJSONObject("careNotes")

        val startsArr = root.optJSONArray("periodStarts")
        val starts = mutableListOf<String>()
        if (startsArr != null) {
            for (i in 0 until startsArr.length()) {
                val item = startsArr.optString(i).trim()
                if (item.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
                    starts.add(item)
                }
            }
        }

        val profile = UserProfile(
            userId = uid,
            userName = root.optString("userName", "User").trim().ifBlank { "User" }.take(80),
            setupDone = true,
            pin = root.optString("pin", "1234").takeIf { it.matches(Regex("^\\d{4}$")) } ?: "1234",
            themeMode = if (root.optString("themeMode") == "dark") "dark" else "light",
            petalsEnabled = petalObj?.optBoolean("enabled", true) ?: true,
            petalStyle = PetalStyle.fromKey(petalObj?.optString("style")).key,
            petalSize = (petalObj?.optLong("size", 22L) ?: 22L).coerceIn(12L, 42L),
            petalOpacityPct = (petalObj?.optLong("opacityPct", 34L) ?: 34L).coerceIn(8L, 80L),
            defaultCycleLength = root.optLong("defaultCycleLength", 28L).coerceIn(20L, 45L),
            defaultPeriodDuration = root.optLong("defaultPeriodDuration", 5L).coerceIn(2L, 10L),
            dailyWaterGoal = root.optLong("dailyWaterGoal", 2000L).coerceIn(500L, 5000L),
            periodStarts = starts.distinct().sorted().takeLast(240),
            bfWhatsapp = root.optString("bfWhatsapp", "").trim().take(40),
            bfEmail = root.optString("bfEmail", "").trim().take(120),
            careNotePeriod = notesObj?.optString("period")?.ifBlank { DEFAULT_CARE_NOTE_PERIOD }
                ?: DEFAULT_CARE_NOTE_PERIOD,
            careNotePms = notesObj?.optString("pms")?.ifBlank { DEFAULT_CARE_NOTE_PMS }
                ?: DEFAULT_CARE_NOTE_PMS,
            careNoteOvulation = notesObj?.optString("ovulation")?.ifBlank { DEFAULT_CARE_NOTE_OVULATION }
                ?: DEFAULT_CARE_NOTE_OVULATION,
            careNoteNormal = notesObj?.optString("normal")?.ifBlank { DEFAULT_CARE_NOTE_NORMAL }
                ?: DEFAULT_CARE_NOTE_NORMAL
        )

        val logsMap = mutableMapOf<String, DailyLog>()
        val dailyLogsObj = root.optJSONObject("dailyLogs")
        val waterLogsObj = root.optJSONObject("waterLogs")

        if (dailyLogsObj != null) {
            val keys = dailyLogsObj.keys()
            while (keys.hasNext()) {
                val dateKey = keys.next()
                if (!dateKey.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) continue
                val item = dailyLogsObj.optJSONObject(dateKey) ?: continue
                val waterFallback = waterLogsObj?.optLong(dateKey, 0L) ?: 0L
                val waterMl = item.optLong("waterMl", waterFallback).coerceIn(0L, 10000L)
                logsMap[dateKey] = DailyLog(
                    id = dateKey,
                    userId = uid,
                    date = dateKey,
                    periodDay = item.optBoolean("isPeriod", item.optBoolean("periodDay", false)),
                    flow = item.optString("flow", "නැත (None)").take(60),
                    pain = item.optLong("pain", 0L).coerceIn(0L, 5L),
                    genSeverity = item.optLong("genSeverity", 0L).coerceIn(0L, 5L),
                    waterMl = waterMl,
                    moods = jsonArrayToList(item.optJSONArray("moods")),
                    symptoms = jsonArrayToList(item.optJSONArray("symptoms")),
                    genSymptoms = jsonArrayToList(item.optJSONArray("genSymptoms")),
                    cravings = jsonArrayToList(item.optJSONArray("cravings")),
                    note = item.optString("note", "").take(1000)
                )
            }
        }

        if (waterLogsObj != null) {
            val wKeys = waterLogsObj.keys()
            while (wKeys.hasNext()) {
                val dateKey = wKeys.next()
                if (!dateKey.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) continue
                if (!logsMap.containsKey(dateKey)) {
                    val ml = waterLogsObj.optLong(dateKey, 0L).coerceIn(0L, 10000L)
                    logsMap[dateKey] = DailyLog(
                        id = dateKey,
                        userId = uid,
                        date = dateKey,
                        waterMl = ml
                    )
                }
            }
        }

        return ParsedBackup(profile = profile, logs = logsMap)
    }

    private fun jsonArrayToList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val s = arr.optString(i).trim()
            if (s.isNotEmpty()) list.add(s)
        }
        return list.distinct().take(20)
    }
}
