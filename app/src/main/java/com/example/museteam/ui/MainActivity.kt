package com.example.museteam.ui

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.museteam.R
import com.example.museteam.data.ActivityInfo
import com.example.museteam.data.ActivityState
import com.example.museteam.data.BoardStore
import com.example.museteam.data.ExpectedTeam
import com.example.museteam.data.Freshness
import com.example.museteam.data.MemberRow
import com.example.museteam.data.MuseReport

/**
 * Màn chính — 2 tab đúng mockup v5: Đội Muse | Tài khoản.
 * Mở app: hiện ngay cache đã lưu (nếu có) rồi tự tải bảng mới ở nền.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvSummary: TextView
    private lateinit var tvBanner: TextView
    private lateinit var listContainer: LinearLayout
    private lateinit var tabTeam: TextView
    private lateinit var tabAccounts: TextView

    private var currentTab = TAB_TEAM
    private var refreshing = false
    private var initialLoadDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTitle = findViewById(R.id.tvTitle)
        tvSummary = findViewById(R.id.tvSummary)
        tvBanner = findViewById(R.id.tvBanner)
        listContainer = findViewById(R.id.listContainer)
        tabTeam = findViewById(R.id.tabTeam)
        tabAccounts = findViewById(R.id.tabAccounts)

        findViewById<TextView>(R.id.btnRefresh).setOnClickListener { refresh() }
        findViewById<TextView>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        tabTeam.setOnClickListener { currentTab = TAB_TEAM; render() }
        tabAccounts.setOnClickListener { currentTab = TAB_ACCOUNTS; render() }

        if (BoardStore.snapshot == null) BoardStore.loadCache(this)
        render()
        refresh()
        initialLoadDone = true
    }

    override fun onResume() {
        super.onResume()
        // Quay về từ Cài đặt sau khi đổi repo/token -> tự tải lại.
        if (initialLoadDone && BoardStore.needsReload) refresh()
        else if (initialLoadDone) render()
    }

    private fun refresh() {
        if (refreshing) return
        refreshing = true
        render()
        BoardStore.refresh(this) {
            runOnUiThread {
                refreshing = false
                render()
            }
        }
    }

    // ------------------------------------------------------------------ render

    private fun render() {
        styleTabs()
        renderBanner()
        val reports = BoardStore.snapshot?.reports ?: emptyList()
        if (currentTab == TAB_TEAM) renderTeam(reports) else renderAccounts(reports)
    }

    private fun styleTabs() {
        val on = getColor(R.color.accent_pink)
        val off = getColor(R.color.text_faint)
        tabTeam.setTextColor(if (currentTab == TAB_TEAM) on else off)
        tabAccounts.setTextColor(if (currentTab == TAB_ACCOUNTS) on else off)
        tabTeam.typeface = if (currentTab == TAB_TEAM) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        tabAccounts.typeface = if (currentTab == TAB_ACCOUNTS) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        tvTitle.text = if (currentTab == TAB_TEAM) "Đội Muse" else "Tài khoản"
    }

    private fun renderBanner() {
        val snap = BoardStore.snapshot
        val err = BoardStore.lastError
        when {
            refreshing -> showBanner("Đang tải số mới…", R.color.text_faint)
            err != null -> {
                val cacheNote = if (snap != null) {
                    " Đang xem số đã lưu lúc ${Freshness.formatTime(snap.fetchedAtMillis)}."
                } else ""
                showBanner(err + cacheNote, R.color.error_red)
            }
            BoardStore.fromCache && snap != null -> showBanner(
                "Đang xem số đã lưu lúc ${Freshness.formatTime(snap.fetchedAtMillis)}.",
                R.color.text_faint
            )
            else -> tvBanner.visibility = View.GONE
        }
    }

    private fun showBanner(text: String, colorRes: Int) {
        tvBanner.visibility = View.VISIBLE
        tvBanner.text = text
        tvBanner.setTextColor(getColor(colorRes))
    }

    private fun renderTeam(reports: List<MuseReport>) {
        val now = System.currentTimeMillis()
        val rows = ExpectedTeam.mergeRows(reports)
        val live = reports.filter { !Freshness.isStale(now, it.updatedAtMillis) }
        val working = live.count { it.activity?.state == ActivityState.WORKING }
        val waiting = live.count { it.activity?.state == ActivityState.WAITING }
        val lost = rows.count { it.report == null } +
            reports.count { Freshness.isStale(now, it.updatedAtMillis) }
        tvSummary.text = "$working đang chạy · $waiting chờ · $lost mất tín hiệu"

        listContainer.removeAllViews()
        for (row in rows) listContainer.addView(memberRowView(row, now))
        if (reports.isEmpty() && BoardStore.snapshot == null) {
            listContainer.addView(hintView("Chưa tải được bảng lần nào — kiểm tra mạng, hoặc dán token trong Cài đặt (nút ⚙)."))
        }
    }

    private fun memberRowView(row: MemberRow, now: Long): View {
        val v = layoutInflater.inflate(R.layout.item_member, listContainer, false)
        val avatar = v.findViewById<AvatarView>(R.id.avatarView)
        val tvName = v.findViewById<TextView>(R.id.tvName)
        val tvSub = v.findViewById<TextView>(R.id.tvSub)
        val tvOpen = v.findViewById<TextView>(R.id.tvOpen)
        val report = row.report

        tvName.text = row.displayName
        avatar.setData(row.displayName, report?.let { BoardStore.cache(this).avatarFileFor(it) })

        when {
            report == null -> {
                tvName.setTextColor(getColor(R.color.text_faint))
                tvSub.text = "Chưa nối báo cáo"
            }
            Freshness.isStale(now, report.updatedAtMillis) -> {
                tvSub.text = "○ Mất tín hiệu · số của ${Freshness.ageText(now, report.updatedAtMillis)}"
                tvSub.setTextColor(getColor(R.color.text_faint))
            }
            report.activity != null -> {
                tvSub.text = stateLine(report.activity)
            }
            else -> {
                tvSub.text = "Cập nhật ${Freshness.ageText(now, report.updatedAtMillis)}"
            }
        }

        tvOpen.setOnClickListener { UiUtil.openMuse(this) }
        v.setOnClickListener {
            startActivity(
                Intent(this, DetailActivity::class.java)
                    .putExtra(DetailActivity.EXTRA_ID, row.id)
            )
        }
        return v
    }

    /** "● Đang chạy · <việc>" — chấm + nhãn tô màu trạng thái, việc để màu thường. */
    private fun stateLine(act: ActivityInfo): CharSequence {
        val head = "● ${act.state.label}"
        val tail = if (act.task.isNotBlank()) " · ${act.task}" else ""
        val sb = SpannableStringBuilder(head + tail)
        sb.setSpan(
            ForegroundColorSpan(getColor(UiUtil.stateColorRes(act.state))),
            0, head.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return sb
    }

    private fun renderAccounts(reports: List<MuseReport>) {
        val groups = ExpectedTeam.groupAccounts(reports)
        val exhausted = reports.count {
            it.weekly != null && Freshness.remainingPct(it.weekly.usedPct) == 0
        }
        tvSummary.text = "${groups.size} tài khoản · $exhausted hết quota tuần"

        listContainer.removeAllViews()
        if (groups.isEmpty()) {
            listContainer.addView(hintView("Chưa có báo cáo nào trên bảng — khi các Muse gửi báo cáo đầu tiên, tài khoản của chúng sẽ hiện ở đây."))
            return
        }
        for (group in groups) {
            for (member in group.members) {
                listContainer.addView(accountRowView(group.label, member))
            }
        }
    }

    private fun accountRowView(groupLabel: String, member: MuseReport): View {
        val v = layoutInflater.inflate(R.layout.item_account, listContainer, false)
        val tvTitleRow = v.findViewById<TextView>(R.id.tvTitle)
        val tvSub = v.findViewById<TextView>(R.id.tvSub)
        val tvOpen = v.findViewById<TextView>(R.id.tvOpen)
        // Dòng chính của hàng: email đăng nhập của member (thiếu email mới
        // rơi về nhãn nhóm / "Tài khoản") — xem ExpectedTeam.accountTitle.
        tvTitleRow.text = ExpectedTeam.accountTitle(groupLabel, member)
        tvSub.text = ExpectedTeam.accountLine(member)
        tvOpen.setOnClickListener { UiUtil.openMuse(this) }
        v.setOnClickListener {
            startActivity(
                Intent(this, DetailActivity::class.java)
                    .putExtra(DetailActivity.EXTRA_ID, member.id)
            )
        }
        return v
    }

    private fun hintView(text: String): View {
        val tv = TextView(this)
        tv.text = text
        tv.setTextColor(getColor(R.color.text_faint))
        tv.textSize = 12.5f
        tv.setPadding(2, 18, 2, 6)
        tv.setLineSpacing(0f, 1.4f)
        return tv
    }

    companion object {
        private const val TAB_TEAM = 0
        private const val TAB_ACCOUNTS = 1
    }
}
