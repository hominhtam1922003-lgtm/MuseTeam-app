package com.example.museteam.data

import java.net.HttpURLConnection
import java.net.URL

/**
 * BoardFetcher thật: đọc repo bảng qua GitHub Contents API (nhánh main).
 * - Liệt kê thư mục: Accept application/vnd.github+json.
 * - Tải file: Accept application/vnd.github.raw+json -> bytes gốc.
 * Token (nếu có) đi ở header Authorization — KHÔNG bao giờ gắn vào URL/log.
 */
class GitHubFetcher(
    private val repo: String,
    private val token: String,
    private val branch: String = "main"
) : BoardFetcher {

    private fun open(path: String, accept: String): HttpURLConnection {
        val url = URL("https://api.github.com/repos/$repo/contents/$path?ref=$branch")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15_000
        conn.readTimeout = 20_000
        conn.setRequestProperty("Accept", accept)
        conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        conn.setRequestProperty("User-Agent", "MuseTeam-App")
        if (token.isNotBlank()) {
            conn.setRequestProperty("Authorization", "Bearer $token")
        }
        return conn
    }

    private fun read(conn: HttpURLConnection): ByteArray {
        val code = conn.responseCode
        if (code !in 200..299) {
            conn.disconnect()
            throw HttpStatusException(code)
        }
        return conn.inputStream.use { it.readBytes() }
    }

    override fun listDir(path: String): String =
        String(read(open(path, "application/vnd.github+json")), Charsets.UTF_8)

    override fun fetchRaw(path: String): ByteArray =
        read(open(path, "application/vnd.github.raw+json"))
}
