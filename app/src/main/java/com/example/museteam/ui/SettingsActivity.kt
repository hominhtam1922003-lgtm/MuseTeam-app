package com.example.museteam.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.museteam.R
import com.example.museteam.data.BoardStore
import com.example.museteam.data.Settings

/** Cài đặt: repo bảng + token GitHub chỉ-đọc (dán 1 lần). */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val settings = Settings(this)
        val etRepo = findViewById<EditText>(R.id.etRepo)
        val etToken = findViewById<EditText>(R.id.etToken)
        val tvMsg = findViewById<TextView>(R.id.tvSettingsMsg)

        etRepo.setText(settings.repo)
        etToken.setText(settings.token)

        findViewById<TextView>(R.id.tvBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val repo = etRepo.text.toString().trim()
            if (!repo.contains("/") || repo.startsWith("/") || repo.endsWith("/")) {
                tvMsg.setTextColor(getColor(R.color.error_red))
                tvMsg.text = "Repo phải có dạng owner/name — ví dụ: ${Settings.DEFAULT_REPO}"
                return@setOnClickListener
            }
            settings.repo = repo
            settings.token = etToken.text.toString()
            BoardStore.needsReload = true
            finish()
        }
    }
}
