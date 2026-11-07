package com.example.museteam.data

import android.content.Context
import java.io.File
import java.util.concurrent.Executors

/**
 * Kho trạng thái dùng chung cho các màn: giữ snapshot mới nhất trong RAM,
 * tải nền bằng 1 executor duy nhất (tránh hai lượt tải chồng nhau).
 */
object BoardStore {

    @Volatile
    var snapshot: BoardSnapshot? = null
        private set

    /** Snapshot hiện tại đến từ cache đĩa (chưa tải mới được). */
    @Volatile
    var fromCache: Boolean = false
        private set

    @Volatile
    var lastError: String? = null
        private set

    /** Đặt true khi Cài đặt thay đổi -> MainActivity tự tải lại khi quay về. */
    @Volatile
    var needsReload: Boolean = true

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "museteam-board").apply { isDaemon = true }
    }

    fun cacheDir(context: Context): File = File(context.filesDir, "board-cache")

    fun cache(context: Context): BoardCache = BoardCache(cacheDir(context))

    /** Nạp cache đĩa vào RAM (mở app là hiện ngay số đã lưu, rồi mới tải mới). */
    fun loadCache(context: Context): BoardSnapshot? {
        val snap = cache(context).load()
        if (snap != null) {
            snapshot = snap
            fromCache = true
        }
        return snap
    }

    fun refresh(context: Context, onDone: (SyncResult) -> Unit) {
        val appContext = context.applicationContext
        val settings = Settings(appContext)
        val repo = settings.repo
        val token = settings.token
        val cache = cache(appContext)
        executor.execute {
            val result = BoardSync.sync(GitHubFetcher(repo, token))
            if (result is SyncResult.Success) {
                val now = System.currentTimeMillis()
                val rawById = result.reports.associate { it.id to it.rawJson }
                runCatching {
                    cache.save(rawById, now, result.skippedFiles, result.avatarBytes)
                }
                snapshot = BoardSnapshot(result.reports, now, result.skippedFiles)
                fromCache = false
                lastError = null
                needsReload = false
            } else if (result is SyncResult.Failure) {
                lastError = result.message
            }
            onDone(result)
        }
    }

    fun reportById(id: String): MuseReport? =
        snapshot?.reports?.firstOrNull { it.id == id }
}
