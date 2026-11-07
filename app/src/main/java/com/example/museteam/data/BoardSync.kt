package com.example.museteam.data

import org.json.JSONArray
import java.io.IOException

/** Lỗi HTTP từ GitHub — mang mã trạng thái để dịch thành câu báo dễ hiểu. */
class HttpStatusException(val code: Int) : Exception("HTTP $code")

/**
 * Cổng tải bảng — tách interface để kiểm thử được bằng fetcher giả.
 * Bản thật là GitHubFetcher (HttpURLConnection).
 */
interface BoardFetcher {
    /** Liệt kê 1 thư mục trên repo: trả nguyên văn JSON của GitHub Contents API. */
    fun listDir(path: String): String

    /** Tải 1 file dạng raw (bytes gốc). */
    fun fetchRaw(path: String): ByteArray
}

sealed class SyncResult {
    data class Success(
        val reports: List<MuseReport>,
        /** Số file bị bỏ qua (tải hỏng hoặc JSON hỏng). */
        val skippedFiles: Int,
        /** Bytes avatar theo ĐƯỜNG DẪN avatar trong báo cáo — tải hỏng thì vắng mặt. */
        val avatarBytes: Map<String, ByteArray>
    ) : SyncResult()

    data class Failure(val message: String) : SyncResult()
}

/**
 * Đồng bộ bảng trạng thái: liệt kê status/ -> tải từng .json -> parse ->
 * tải avatar. Lỗi từng file KHÔNG làm sập cả lượt: file đó bị bỏ qua và
 * đếm vào skippedFiles (giao diện vẫn hiện các Muse còn lại).
 */
object BoardSync {

    fun httpErrorMessage(code: Int): String = when (code) {
        401 -> "Token sai hoặc đã hết hạn (HTTP 401) — vào Cài đặt dán lại token."
        403 -> "GitHub từ chối truy cập (HTTP 403) — token thiếu quyền đọc repo này hoặc đã hết hạn."
        404 -> "Không thấy repo hoặc thư mục status/ (HTTP 404) — kiểm tra tên repo trong Cài đặt."
        else -> "GitHub trả lỗi HTTP $code — thử tải lại sau."
    }

    /** Lấy tên các file .json từ JSON liệt kê thư mục của GitHub. Hỏng -> null. */
    fun parseListingNames(listingText: String): List<String>? {
        val arr = try {
            JSONArray(listingText)
        } catch (e: Exception) {
            return null
        }
        val names = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            if (item.optString("type") != "file") continue
            val name = item.optString("name")
            if (name.endsWith(".json")) names += name
        }
        return names
    }

    fun sync(fetcher: BoardFetcher): SyncResult {
        val listingText = try {
            fetcher.listDir("status")
        } catch (e: HttpStatusException) {
            return SyncResult.Failure(httpErrorMessage(e.code))
        } catch (e: IOException) {
            return SyncResult.Failure(
                "Không tải được bảng — kiểm tra mạng rồi thử lại. (${e.message ?: "lỗi kết nối"})"
            )
        } catch (e: Exception) {
            return SyncResult.Failure("Lỗi đọc bảng: ${e.message ?: "không rõ nguyên nhân"}")
        }

        val names = parseListingNames(listingText)
            ?: return SyncResult.Failure(
                "Danh sách file trên bảng không đọc được (định dạng lạ) — thử tải lại."
            )

        val texts = mutableListOf<String>()
        var skipped = 0
        for (name in names) {
            try {
                texts += String(fetcher.fetchRaw("status/$name"), Charsets.UTF_8)
            } catch (e: Exception) {
                skipped++
            }
        }
        val batch = ReportParser.parseMany(texts)
        skipped += batch.skipped

        // Avatar: tải lẻ từng cái, hỏng cái nào bỏ cái đó (vòng chữ thay thế).
        val avatars = mutableMapOf<String, ByteArray>()
        for (r in batch.reports) {
            val path = r.avatarPath
            if (path.isBlank() || avatars.containsKey(path)) continue
            try {
                avatars[path] = fetcher.fetchRaw(path)
            } catch (e: Exception) {
                // bỏ qua — UI tự rơi về vòng chữ
            }
        }
        return SyncResult.Success(batch.reports, skipped, avatars)
    }
}
