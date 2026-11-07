package com.example.museteam.data

/**
 * Logic THUẦN cho vòng avatar dự phòng (khi chưa có/không tải được avatar thật):
 * chữ viết tắt + cặp màu gradient theo tên — cùng bộ màu với mockup v5.
 */
object AvatarLogic {

    /** Các cặp màu (start, end) — hồng/tím · xanh dương/chàm · cam/đỏ ·
     *  xanh lá/xanh dương · tím nhạt/hồng đậm · xám. */
    val PALETTE: List<Pair<Int, Int>> = listOf(
        (0xFFFF5DA2.toInt() to 0xFFA855F7.toInt()),
        (0xFF38BDF8.toInt() to 0xFF6366F1.toInt()),
        (0xFFF59E0B.toInt() to 0xFFEF4444.toInt()),
        (0xFF34D399.toInt() to 0xFF0EA5E9.toInt()),
        (0xFFA78BFA.toInt() to 0xFFEC4899.toInt()),
        (0xFF94A3B8.toInt() to 0xFF475569.toInt())
    )

    /**
     * Tên kết thúc bằng số -> lấy số ("Nhánh 97" -> "97", "Tổng 98" -> "98");
     * còn lại lấy chữ cái đầu ("Lưu Diệc Phi" -> "L").
     */
    fun initialsFor(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "?"
        val trailing = Regex("""(\d+)\s*$""").find(trimmed)
        if (trailing != null) return trailing.groupValues[1]
        val first = trimmed.firstOrNull { it.isLetterOrDigit() } ?: return "?"
        return first.uppercaseChar().toString()
    }

    /** Chỉ số màu ổn định theo tên (cùng tên -> cùng màu mọi lần mở app). */
    fun paletteIndexFor(name: String): Int {
        val h = name.trim().lowercase().hashCode()
        return (h and Int.MAX_VALUE) % PALETTE.size
    }
}
