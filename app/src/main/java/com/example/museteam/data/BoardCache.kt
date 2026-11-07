package com.example.museteam.data

import java.io.File
import java.util.Properties

/** Ảnh chụp bảng gần nhất đọc tốt — để mất mạng vẫn xem được số đã lưu. */
data class BoardSnapshot(
    val reports: List<MuseReport>,
    val fetchedAtMillis: Long,
    val skippedFiles: Int
)

/**
 * Cache bảng xuống đĩa (THUẦN java.io — kiểm thử được trên JVM):
 *   <dir>/status/<id>.json   nguyên văn từng báo cáo
 *   <dir>/avatars/<tên phẳng> bytes avatar
 *   <dir>/meta.properties    fetchedAt + skipped
 */
class BoardCache(private val dir: File) {

    private val statusDir: File get() = File(dir, "status")
    private val avatarDir: File get() = File(dir, "avatars")
    private val metaFile: File get() = File(dir, "meta.properties")

    fun save(
        rawJsonById: Map<String, String>,
        fetchedAtMillis: Long,
        skippedFiles: Int,
        avatarBytesByPath: Map<String, ByteArray>
    ) {
        dir.mkdirs()
        statusDir.mkdirs()
        avatarDir.mkdirs()
        // Xoá báo cáo đã biến mất khỏi bảng để cache không hiện "ma".
        statusDir.listFiles()?.forEach { f ->
            if (f.name.removeSuffix(".json") !in rawJsonById.keys) f.delete()
        }
        for ((id, raw) in rawJsonById) {
            File(statusDir, "$id.json").writeText(raw)
        }
        for ((path, bytes) in avatarBytesByPath) {
            val f = avatarFile(path)
            f.parentFile?.mkdirs()
            f.writeBytes(bytes)
        }
        val props = Properties()
        props.setProperty("fetchedAt", fetchedAtMillis.toString())
        props.setProperty("skipped", skippedFiles.toString())
        metaFile.outputStream().use { props.store(it, "museteam board cache") }
    }

    fun load(): BoardSnapshot? {
        if (!metaFile.exists()) return null
        val props = Properties()
        try {
            metaFile.inputStream().use { props.load(it) }
        } catch (e: Exception) {
            return null
        }
        val fetchedAt = props.getProperty("fetchedAt")?.toLongOrNull() ?: return null
        val skippedMeta = props.getProperty("skipped")?.toIntOrNull() ?: 0
        val texts = statusDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".json") }
            ?.map { runCatching { it.readText() }.getOrNull() }
            ?.filterNotNull()
            ?: emptyList()
        val batch = ReportParser.parseMany(texts)
        return BoardSnapshot(batch.reports, fetchedAt, skippedMeta + batch.skipped)
    }

    /** Đường dẫn cache của 1 avatar: làm phẳng "avatars/tong-98.webp" -> "avatars_tong-98.webp". */
    fun avatarFile(avatarPath: String): File {
        val flat = avatarPath.replace('/', '_')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(avatarDir, flat)
    }

    /** File avatar đã cache của 1 báo cáo — null nếu chưa có/không khai avatar. */
    fun avatarFileFor(report: MuseReport): File? {
        if (report.avatarPath.isBlank()) return null
        val f = avatarFile(report.avatarPath)
        return if (f.exists() && f.length() > 0) f else null
    }
}
