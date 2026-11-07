package com.example.museteam.data

import java.time.ZoneId

/**
 * Mô hình dữ liệu của "bảng trạng thái" — mỗi Muse tự ghi 1 file
 * status/<id>.json lên repo bảng (hợp đồng xem README của repo muse-board).
 * File này THUẦN Kotlin (không import Android) để kiểm thử được trên JVM.
 */

enum class ActivityState(val key: String, val label: String) {
    WORKING("working", "Đang chạy"),
    WAITING("waiting", "Đang chờ"),
    STUCK("stuck", "Kẹt"),
    IDLE("idle", "Nghỉ"),
    DONE("done", "Xong");

    companion object {
        fun fromKey(key: String?): ActivityState =
            entries.firstOrNull { it.key == key?.trim()?.lowercase() } ?: IDLE
    }
}

data class WeeklyQuota(
    val usedPct: Int,
    val resetAtMillis: Long?
)

data class ExtraQuota(
    val usedPct: Int,
    val remainingText: String,
    val expires: String
)

data class ActivityInfo(
    val state: ActivityState,
    val task: String,
    val sinceMillis: Long?
)

data class MuseReport(
    val id: String,
    val name: String,
    val role: String,
    val team: String,
    val accountLabel: String,
    val accountEmail: String,
    val avatarPath: String,
    val plan: String,
    val weekly: WeeklyQuota?,
    val extra: ExtraQuota?,
    val activity: ActivityInfo?,
    val updatedAtMillis: Long?,
    /** Nguyên văn JSON — để cache xuống đĩa rồi parse lại y hệt. */
    val rawJson: String
)

/** Một hàng trong tab Đội Muse: có báo cáo thật hoặc chỉ là kỳ vọng. */
data class MemberRow(
    val id: String,
    val displayName: String,
    val report: MuseReport?
)

data class ExpectedMember(val id: String, val fallbackName: String)

data class AccountGroup(
    val key: String,
    val label: String,
    val members: List<MuseReport>
)

object ExpectedTeam {

    /** Đội hình kỳ vọng nạp sẵn — id chưa có báo cáo thì hiện "chưa nối báo cáo". */
    val EXPECTED: List<ExpectedMember> = listOf(
        ExpectedMember("tong-98", "Lưu Diệc Phi"),
        ExpectedMember("nhanh-97", "Nhánh 97"),
        ExpectedMember("nhanh-96", "Nhánh 96")
    )

    /**
     * Gộp báo cáo thật vào đội hình kỳ vọng: hàng kỳ vọng giữ đúng thứ tự,
     * báo cáo lạ (ngoài kỳ vọng) xếp sau theo tên.
     */
    fun mergeRows(reports: List<MuseReport>): List<MemberRow> {
        val byId = reports.associateBy { it.id }
        val rows = mutableListOf<MemberRow>()
        val seen = mutableSetOf<String>()
        for (e in EXPECTED) {
            val r = byId[e.id]
            rows += MemberRow(e.id, r?.name?.ifBlank { e.fallbackName } ?: e.fallbackName, r)
            seen += e.id
        }
        reports.filter { it.id !in seen }
            .sortedBy { it.name.lowercase() }
            .forEach { rows += MemberRow(it.id, it.name, it) }
        return rows
    }

    /** Khoá gom tài khoản: ưu tiên email, thiếu email thì dùng nhãn. */
    fun accountKey(r: MuseReport): String =
        r.accountEmail.ifBlank { r.accountLabel }

    /** Gom các Muse chạy chung 1 tài khoản thành 1 nhóm (giữ thứ tự gặp đầu). */
    fun groupAccounts(reports: List<MuseReport>): List<AccountGroup> {
        val groups = LinkedHashMap<String, MutableList<MuseReport>>()
        for (r in reports) groups.getOrPut(accountKey(r)) { mutableListOf() } += r
        return groups.map { (key, members) ->
            AccountGroup(key, members.first().accountLabel, members)
        }
    }

    /** Dòng tóm tắt 1 tài khoản: "Tuần: còn X% · reset dd/MM · Token phụ còn …" */
    fun accountLine(r: MuseReport, zone: ZoneId = ZoneId.systemDefault()): String {
        val parts = mutableListOf<String>()
        val w = r.weekly
        if (w != null) {
            val left = Freshness.remainingPct(w.usedPct)
            parts += if (left == 0) "Tuần: hết" else "Tuần: còn $left%"
            if (w.resetAtMillis != null) parts += "reset ${Freshness.formatDayMonth(w.resetAtMillis, zone)}"
        } else {
            parts += "Tuần: chưa rõ"
        }
        val e = r.extra
        if (e != null && e.remainingText.isNotBlank()) {
            parts += "Token phụ còn ${e.remainingText}"
        }
        return parts.joinToString(" · ")
    }
}
