package com.example.museteam.data

import android.content.Context

/**
 * Cấu hình app: repo bảng + token GitHub chỉ-đọc.
 * Lưu ở SharedPreferences RIÊNG của app ("museteam_settings") trên máy này.
 */
class Settings(context: Context) {

    private val prefs =
        context.getSharedPreferences("museteam_settings", Context.MODE_PRIVATE)

    var repo: String
        get() = prefs.getString(KEY_REPO, DEFAULT_REPO)?.takeIf { it.isNotBlank() }
            ?: DEFAULT_REPO
        set(value) {
            prefs.edit().putString(KEY_REPO, value.trim()).apply()
        }

    var token: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_TOKEN, value.trim()).apply()
        }

    companion object {
        const val DEFAULT_REPO = "hominhtam1922003-lgtm/muse-board"
        private const val KEY_REPO = "repo"
        private const val KEY_TOKEN = "token"
    }
}
