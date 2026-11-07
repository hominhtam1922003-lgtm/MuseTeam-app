# Đội Muse — app quản lý đội AI Muse

App Android đọc **bảng trạng thái** do chính các Muse tự báo cáo: mỗi Muse
ghi 1 file `status/<id>.json` (và avatar ở `avatars/`) lên repo bảng riêng tư
`hominhtam1922003-lgtm/muse-board`. App chỉ ĐỌC bảng đó qua GitHub API —
không đăng nhập chéo vào tài khoản nào.

Hiển thị, theo đúng mockup v5 đã duyệt:

- **Tab Đội Muse**: avatar thật + tên thật của từng Muse, chấm trạng thái
  sống (đang chạy / chờ / kẹt / nghỉ / xong) + việc đang làm; Muse nào quá
  24 giờ không cập nhật thì hiện "mất tín hiệu". Ba Muse kỳ vọng
  (tong-98, nhanh-97, nhanh-96) chưa có báo cáo thì hiện "chưa nối báo cáo".
- **Tab Tài khoản**: gom theo tài khoản — quota tuần còn bao nhiêu %, ngày
  reset, token bổ sung còn bao nhiêu.
- **Chi tiết**: thẻ Mức sử dụng 2 tầng (quota tuần của gói + token bổ sung),
  đếm ngược tới mốc reset, độ tươi của số liệu, nút mở nhanh muse.ai.
- **Cài đặt**: tên repo bảng + token GitHub fine-grained chỉ-đọc, dán 1 lần.
  Mất mạng vẫn xem được số đã lưu kèm nhãn thời điểm lưu.

Kỹ thuật: Kotlin + Views/XML, package `com.example.museteam`, minSdk 26 /
target 35, chỉ phụ thuộc appcompat (JSON dùng org.json, mạng dùng
HttpURLConnection). Khoá ký cố định riêng của app — xem BUILD-NOTES.md.
