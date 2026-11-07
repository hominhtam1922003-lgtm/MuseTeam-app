package com.example.museteam.data

import org.json.JSONObject

/**
 * Parse 1 file báo cáo status/<id>.json -> MuseReport.
 * Nguyên tắc phòng thủ: file hỏng/thiếu id -> null (bỏ qua, KHÔNG crash);
 * thiếu khối con (weekly/extra/activity) -> khối đó null, phần còn lại vẫn dùng.
 * Dùng org.json — có sẵn trong Android và trong json.jar khi test JVM.
 */
object ReportParser {

    data class ParseBatch(val reports: List<MuseReport>, val skipped: Int)

    fun parse(text: String): MuseReport? {
        val obj = try {
            JSONObject(text)
        } catch (e: Exception) {
            return null
        }
        val id = obj.optString("id").trim()
        if (id.isEmpty()) return null

        val weeklyObj = obj.optJSONObject("weekly")
        val weekly = if (weeklyObj != null && weeklyObj.has("used_pct")) {
            WeeklyQuota(
                usedPct = weeklyObj.optInt("used_pct", 0).coerceIn(0, 100),
                resetAtMillis = Freshness.parseIsoToMillis(weeklyObj.optString("reset_at"))
            )
        } else null

        val extraObj = obj.optJSONObject("extra")
        val extra = if (extraObj != null && (extraObj.has("used_pct") || extraObj.has("remaining_text"))) {
            ExtraQuota(
                usedPct = extraObj.optInt("used_pct", 0).coerceIn(0, 100),
                remainingText = extraObj.optString("remaining_text").trim(),
                expires = extraObj.optString("expires").trim()
            )
        } else null

        val actObj = obj.optJSONObject("activity")
        val activity = actObj?.let {
            ActivityInfo(
                state = ActivityState.fromKey(it.optString("state")),
                task = it.optString("task").trim(),
                sinceMillis = Freshness.parseIsoToMillis(it.optString("since"))
            )
        }

        return MuseReport(
            id = id,
            name = obj.optString("name").trim().ifBlank { id },
            role = obj.optString("role").trim(),
            team = obj.optString("team").trim(),
            accountLabel = obj.optString("account_label").trim(),
            accountEmail = obj.optString("account_email").trim(),
            avatarPath = obj.optString("avatar").trim(),
            plan = obj.optString("plan").trim(),
            weekly = weekly,
            extra = extra,
            activity = activity,
            updatedAtMillis = Freshness.parseIsoToMillis(obj.optString("updated_at")),
            rawJson = text
        )
    }

    /** Parse nhiều file: file hỏng bị đếm vào skipped, file tốt vẫn ra. */
    fun parseMany(texts: List<String>): ParseBatch {
        val out = mutableListOf<MuseReport>()
        var skipped = 0
        for (t in texts) {
            val r = parse(t)
            if (r != null) out += r else skipped++
        }
        return ParseBatch(out, skipped)
    }
}
