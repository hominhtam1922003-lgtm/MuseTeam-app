package com.example.museteam.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.museteam.R
import com.example.museteam.data.ActivityState
import com.example.museteam.data.BoardStore
import com.example.museteam.data.ExpectedTeam
import com.example.museteam.data.Freshness

/**
 * Chi tiết 1 Muse: hoạt động sống + thẻ Mức sử dụng 2 tầng quota
 * (tuần của gói / token bổ sung) đúng panel Muse — số do chính Muse đó
 * tự báo cáo lên bảng.
 */
class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        if (BoardStore.snapshot == null) BoardStore.loadCache(this)
        val id = intent.getStringExtra(EXTRA_ID) ?: ""
        val report = BoardStore.reportById(id)
        val fallbackName = ExpectedTeam.EXPECTED.firstOrNull { it.id == id }?.fallbackName ?: id

        val avatar = findViewById<AvatarView>(R.id.avatarDetail)
        val tvName = findViewById<TextView>(R.id.tvDetailName)
        val tvRole = findViewById<TextView>(R.id.tvDetailRole)
        val activityBlock = findViewById<LinearLayout>(R.id.activityBlock)
        val tvActDot = findViewById<TextView>(R.id.tvActDot)
        val tvActivity = findViewById<TextView>(R.id.tvActivity)
        val tvSince = findViewById<TextView>(R.id.tvSince)
        val tvStaleWarn = findViewById<TextView>(R.id.tvStaleWarn)
        val tvNotConnected = findViewById<TextView>(R.id.tvNotConnected)
        val usageCard = findViewById<LinearLayout>(R.id.usageCard)
        val tvFresh = findViewById<TextView>(R.id.tvFresh)
        val btnOpen = findViewById<Button>(R.id.btnOpen)

        findViewById<TextView>(R.id.tvBack).setOnClickListener { finish() }
        btnOpen.setOnClickListener { UiUtil.openMuse(this) }

        val now = System.currentTimeMillis()

        if (report == null) {
            // Hàng kỳ vọng chưa có báo cáo.
            tvName.text = fallbackName
            tvRole.text = "Chưa nối báo cáo"
            avatar.setData(fallbackName, null)
            activityBlock.visibility = View.GONE
            tvSince.visibility = View.GONE
            usageCard.visibility = View.GONE
            tvFresh.visibility = View.GONE
            tvNotConnected.visibility = View.VISIBLE
            tvNotConnected.text =
                "Muse này chưa nối báo cáo — chưa có báo cáo nào của nó trên bảng trạng thái. " +
                    "Khi nó gửi báo cáo đầu tiên (theo lệnh thường trực), avatar, quota và việc đang làm sẽ hiện ở đây."
            return
        }

        tvName.text = report.name
        val roleLine = listOf(report.role, report.accountLabel)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        tvRole.text = roleLine
        avatar.setData(report.name, BoardStore.cache(this).avatarFileFor(report))

        // --- Hoạt động sống ---
        val act = report.activity
        if (act != null) {
            tvActDot.setTextColor(getColor(UiUtil.stateColorRes(act.state)))
            val head = when (act.state) {
                ActivityState.WORKING -> "Đang làm việc"
                ActivityState.WAITING -> "Đang chờ"
                ActivityState.STUCK -> "Đang kẹt"
                ActivityState.IDLE -> "Đang nghỉ"
                ActivityState.DONE -> "Đã xong việc"
            }
            tvActivity.text = if (act.task.isNotBlank()) "$head — ${act.task}" else head
            if (act.sinceMillis != null) {
                tvSince.text = "Từ ${Freshness.formatTime(act.sinceMillis)}"
            } else {
                tvSince.visibility = View.GONE
            }
        } else {
            tvActDot.setTextColor(getColor(R.color.idle_gray))
            tvActivity.text = "Chưa rõ trạng thái — báo cáo không có mục hoạt động."
            tvSince.visibility = View.GONE
        }

        if (Freshness.isStale(now, report.updatedAtMillis)) {
            tvStaleWarn.visibility = View.VISIBLE
            tvStaleWarn.text =
                "⚠ Số liệu đã cũ — hơn 24 giờ chưa cập nhật, có thể không còn đúng."
        }

        // --- Thẻ Mức sử dụng ---
        val tvPlanName = findViewById<TextView>(R.id.tvPlanName)
        val tvWeeklyUsed = findViewById<TextView>(R.id.tvWeeklyUsed)
        val pbWeekly = findViewById<ProgressBar>(R.id.pbWeekly)
        val tvWeeklySub = findViewById<TextView>(R.id.tvWeeklySub)
        val tvExtraUsed = findViewById<TextView>(R.id.tvExtraUsed)
        val pbExtra = findViewById<ProgressBar>(R.id.pbExtra)
        val tvExtraSub = findViewById<TextView>(R.id.tvExtraSub)

        tvPlanName.text = if (report.plan.isNotBlank()) "Gói ${report.plan}" else "Gói"
        val w = report.weekly
        if (w != null) {
            val left = Freshness.remainingPct(w.usedPct)
            tvWeeklyUsed.text = "Đã dùng ${w.usedPct}%"
            tvWeeklyUsed.setTextColor(
                getColor(if (left == 0) R.color.error_red else R.color.text_sub)
            )
            pbWeekly.progress = w.usedPct
            val countdown = Freshness.resetCountdownText(now, w.resetAtMillis)
            tvWeeklySub.text = buildString {
                append("Giới hạn hàng tuần sẽ đặt lại vào ${Freshness.formatDayMonthLong(w.resetAtMillis)}")
                append(" · còn lại $left%")
                if (countdown.isNotEmpty()) append(" · $countdown")
            }
        } else {
            tvWeeklyUsed.text = "—"
            pbWeekly.progress = 0
            tvWeeklySub.text = "Báo cáo chưa có số quota tuần."
        }

        val e = report.extra
        if (e != null) {
            tvExtraUsed.text = "Đã dùng ${e.usedPct}%"
            pbExtra.progress = e.usedPct
            tvExtraSub.text = buildString {
                if (e.remainingText.isNotBlank()) append("Còn ${e.remainingText}")
                when {
                    e.expires.equals("never", ignoreCase = true) -> {
                        if (isNotEmpty()) append(" · ")
                        append("không bao giờ hết hạn")
                    }
                    e.expires.isNotBlank() -> {
                        if (isNotEmpty()) append(" · ")
                        append("hết hạn: ${e.expires}")
                    }
                }
                if (isEmpty()) append("—")
            }
        } else {
            tvExtraUsed.text = "—"
            pbExtra.progress = 0
            tvExtraSub.text = "Báo cáo chưa có số token bổ sung."
        }

        tvFresh.text =
            "Số liệu Muse này tự báo cáo · cập nhật ${Freshness.ageText(now, report.updatedAtMillis)}"
    }

    companion object {
        const val EXTRA_ID = "member_id"
    }
}
