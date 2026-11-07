# Muse Team ("Đội Muse") Android — BUILD NOTES

App Android quản lý đội AI Muse của user, v1.0.0. Viết bởi Bạch Lộc
(Lập trình viên Trưởng) ngày 07/11/2026. Căn cứ: mockup v5 đã được user
duyệt (`~/workspace/your_files/Mockup-app-quan-ly-doi-Muse.html`) và cơ chế
"bảng trạng thái — Muse tự báo cáo" do user chốt 19:34 07/11/2026.

## 1. Cơ chế hoạt động (app CHỈ ĐỌC)

- Mỗi Muse tự ghi 1 file `status/<id>.json` + avatar ở `avatars/` lên repo
  bảng riêng tư `hominhtam1922003-lgtm/muse-board` (hợp đồng file xem README
  của repo bảng / `~/workspace/muse-board-seed/`).
- App đọc bảng qua **GitHub Contents API** (nhánh `main`): liệt kê `status/`
  -> tải raw từng `.json` -> tải avatar theo field `avatar`. Token
  fine-grained chỉ-đọc do user dán 1 lần ở màn Cài đặt, lưu ở
  SharedPreferences riêng `museteam_settings`; token chỉ đi ở header
  Authorization tới api.github.com, không gắn URL, không log.
- **Cache**: toàn bộ lần đọc tốt gần nhất (JSON từng báo cáo + bytes avatar +
  mốc fetchedAt) lưu ở `filesDir/board-cache/`. Mở app hiện cache ngay rồi
  tải mới ở nền; mất mạng/lỗi thì vẫn xem cache kèm nhãn "Đang xem số đã
  lưu lúc HH:mm · dd/MM" + câu lỗi cụ thể.
- **Độ tươi**: tuổi số liệu tính từ `updated_at` của từng báo cáo; quá 24 giờ
  -> hàng hiện "○ Mất tín hiệu · số của X trước", màn chi tiết gắn cảnh báo
  số cũ. Đây là logic thuần có test (Freshness).

## 2. Màn hình (khớp mockup v5)

- **Đội Muse**: hàng = AvatarView tròn 40dp (ảnh thật đã cache; không
  có/hỏng -> vòng gradient + chữ viết tắt theo tên: "Nhánh 97" -> "97") +
  tên thật + dòng trạng thái (chấm màu + nhãn + việc đang làm, cắt …) +
  nút ↗ mở nhanh. Đầu màn: dòng tổng "x đang chạy · y chờ · z mất tín
  hiệu". Chạm hàng -> Chi tiết. Ba id kỳ vọng (tong-98 "Lưu Diệc Phi",
  nhanh-97 "Nhánh 97", nhanh-96 "Nhánh 96") chưa có báo cáo -> hàng xám
  "Chưa nối báo cáo"; báo cáo ngoài kỳ vọng xếp sau theo tên.
- **Tài khoản**: gom theo account (ưu tiên `account_email`, thiếu thì
  `account_label`); mỗi hàng 1 Muse: "<nhãn TK> · <tên Muse>" + dòng
  "Tuần: còn X% · reset dd/MM · Token phụ còn …" (hết thì "Tuần: hết") +
  nút ↗. Đầu màn: "N tài khoản · M hết quota tuần".
- **Chi tiết**: header avatar 48dp + tên + role · tài khoản; khối hoạt động
  (chấm + "Đang làm việc — <task>" + "Từ HH:mm · dd/MM"); thẻ Mức sử dụng
  nền `#14172E` bo 16dp với 2 thanh gradient xanh dương->chàm: quota tuần
  ("Đã dùng X%" — đỏ khi còn 0%; "Giới hạn hàng tuần sẽ đặt lại vào
  <d> tháng <m> · còn lại Y% · còn Z nữa reset") và token bổ sung
  ("Đã dùng x%"; "Còn <remaining_text> · không bao giờ hết hạn" khi
  `expires=never`); dòng "Số liệu Muse này tự báo cáo · cập nhật …";
  nút gradient hồng->tím "↗ Mở nhanh Muse này" = ACTION_VIEW
  https://muse.ai (KHÔNG hứa đổi đúng tài khoản — giới hạn đã nói với user).
- **Cài đặt**: ô repo (mặc định `hominhtam1922003-lgtm/muse-board`, kiểm
  dạng owner/name) + ô token (kiểu password) + nút "Lưu và tải lại" (lưu
  xong quay về màn chính tự tải lại).
- Làm mới: tự tải khi mở app + nút ⟳ trên thanh đầu. Nền tối theo mockup:
  bg `#0B0E1A`, thẻ `#14172E`, viền `#23264A`, chữ `#E8EAF6`/`#9AA3C7`,
  nhấn hồng `#FF5DA2` -> tím `#A855F7`; trạng thái: chạy `#34D399`, chờ
  `#FBBF24`, kẹt `#F87171`, nghỉ `#8A8AA0`, xong `#38BDF8`.

## 3. Kỹ thuật

- Kotlin + Views/XML, package `com.example.museteam`, minSdk 26 /
  targetSdk 35, versionCode 1 / versionName "1.0.0", tên hiển thị
  "Đội Muse". Theme `Theme.AppCompat.NoActionBar` (KHÔNG dùng Material —
  app chỉ phụ thuộc **appcompat 1.7.0**; JSON = org.json có sẵn; mạng =
  HttpURLConnection; không thư viện ngoài nào khác).
- AGP 8.7.3 / Kotlin 2.0.21 / Gradle 8.9 (cùng khuôn KaraokeMaker Android).
  `app/build.gradle.kts` đọc biến môi trường ký: `KEYSTORE_FILE` /
  `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` — CÙNG TÊN secrets với
  KaraokeMaker để workflow dùng chung khuôn (xem WORKFLOW.txt). Không có
  env (build tay) -> lùi về debug signing. Minify TẮT.
- Phân tầng `data/`:
  - `Models.kt` — data class + `ExpectedTeam` (gộp đội hình kỳ vọng, gom
    tài khoản, dòng tóm tắt tài khoản) — THUẦN.
  - `Freshness.kt` — parse ISO-8601, % còn lại, tuổi số liệu, đếm ngược
    reset, định dạng ngày — THUẦN (java.time, API 26+ có sẵn).
  - `ReportParser.kt` — org.json -> model, phòng thủ từng khối — THUẦN.
  - `AvatarLogic.kt` — chữ viết tắt + bảng màu vòng avatar — THUẦN.
  - `BoardSync.kt` — đồng bộ qua interface `BoardFetcher` (liệt kê -> tải
    -> parse -> avatar), lỗi từng file không sập cả lượt — THUẦN.
  - `GitHubFetcher.kt` — BoardFetcher thật bằng HttpURLConnection.
  - `BoardCache.kt` — cache đĩa THUẦN java.io (status/ + avatars/ +
    meta.properties; lưu lại sẽ xoá báo cáo đã biến mất khỏi bảng).
  - `Settings.kt` / `BoardStore.kt` — SharedPreferences + kho trạng thái
    RAM, tải nền bằng 1 executor duy nhất.
- `ui/`: `MainActivity` (2 tab tự vẽ bằng TextView — không TabLayout vì
  không dùng Material), `DetailActivity`, `SettingsActivity`,
  `AvatarView` (View tự vẽ: cắt tròn ảnh thật hoặc gradient + chữ),
  `UiUtil` (màu trạng thái + mở muse.ai).

## 4. Khoá ký (MỚI, riêng cho app này)

- Keystore: `~/workspace/museteam-android-signing/museteam.jks`, alias
  `museteam`; mật khẩu trong `signing-info.txt` cùng thư mục (KHÔNG chép
  vào file này/repo).
- **SHA-256 của cert:
  10:6E:A9:F4:A9:1B:96:40:02:3C:76:4F:33:C2:E4:01:EE:AD:A9:73:E7:C0:93:DC:25:18:A5:76:9E:94:D9:FD**
- Secrets CI cần nạp (repo MuseTeam-app): `KEYSTORE_B64` (base64 của
  museteam.jks), `KEYSTORE_PASSWORD`, `KEY_ALIAS` = `museteam`,
  `KEY_PASSWORD` — giá trị lấy từ signing-info.txt.
- Workflow THẬT phải tạo ở `.github/workflows/build-android.yml` qua web
  editor; nội dung chuẩn nằm ở **WORKFLOW.txt** (gốc cây) — gồm
  `permissions: contents: write` ở cấp cao nhất (bài học 403 của
  KaraokeMaker), input `release_tag`, APK ra tên `MuseTeam.apk`.

## 5. Kiểm chứng ĐÃ CHẠY (07/11/2026)

Harness riêng `~/workspace/museteam-verify/` (khuôn karaoke-android-verify;
VM chỉ có SDK 34 nên manifest bản scratch được chèn thuộc tính `package`
khi link — cây thật giữ nguyên dạng namespace):

1. **aapt2 compile + link toàn bộ res** (5 layout + drawable + values, kèm
   res của appcompat 1.7.0 và các AAR phụ thuộc): LINK OK, R.java đủ id.
   - Lỗi đã gặp + sửa trong cây thật: theme khai `colorSecondary` (attr
     của Material, app không dùng Material) -> aapt2 báo attr not found ->
     đã bỏ khỏi `themes.xml`.
2. **kotlinc 2.0.21 biên dịch TOÀN CÂY (14 file Kotlin: 9 data + 5 ui) +
   R.java: 0 lỗi** — VERIFY OK.
3. **Test logic thuần trên JVM** (`~/workspace/museteam-verify/test/TeamTest.kt`,
   chạy bằng `run-tests.sh`): **64/64 ca ĐẠT, 0 trượt**, gồm:
   - Parser (12 ca): mẫu THẬT của Tổng 98 từ muse-board-seed (đối chiếu
     từng field + epoch millis của reset/since/updated), JSON tối thiểu,
     rác -> null, thiếu id -> null, weekly thiếu used_pct -> null từng
     khối, state lạ -> IDLE, used_pct 130 -> kẹp 100, parseMany bỏ file hỏng.
   - Freshness (25 ca): parse ISO dạng +07:00 và Z cùng mốc, % còn lại ở
     biên (100/130/-10/59), chữ tuổi (vừa xong/phút/giờ/ngày/null), mốc
     stale đúng 24h (23h59m chưa, 24h00 chưa, 24h+1s stale, null stale),
     đếm ngược reset (30 phút/5 giờ/2 ngày/đã qua/null), định dạng
     "08/11" và "8 tháng 11" theo giờ Asia/Bangkok.
   - Gộp đội hình + tài khoản (6 ca): thứ tự kỳ vọng + hàng chưa nối, báo
     cáo lạ xếp cuối, cùng email -> 1 nhóm, khác email -> 2 nhóm, dòng tài
     khoản khớp nguyên văn kỳ vọng cho 2 mẫu thật.
   - Đồng bộ bằng fetcher giả (10 ca) — chính là lưới kịch bản lỗi:
     * token sai/401 -> Failure có câu hướng dẫn dán lại token, không crash;
     * mất mạng (IOException) -> Failure "Không tải được bảng…";
     * repo rỗng (listing []) -> Success 0 báo cáo (không coi là lỗi);
     * listing hỏng định dạng -> Failure rõ;
     * 1 file JSON hỏng giữa các file tốt -> bỏ qua đúng file đó
       (skipped=1), file tốt vẫn hiện;
     * file .md / thư mục con trong listing -> không tải;
     * avatar tải hỏng -> vẫn Success, avatarBytes trống (UI rơi về vòng chữ);
     * httpErrorMessage cho 403/404/500 đúng nội dung.
   - Cache đĩa (6 ca): load rỗng -> null, round-trip giữ nguyên báo cáo +
     fetchedAt + skipped, tên file avatar làm phẳng, bytes avatar đọc lại
     đúng, Muse không khai avatar -> null, lưu lại thiếu báo cáo -> file
     "ma" bị xoá.
   - AvatarLogic (5 ca): "Nhánh 97"->"97", "Tổng 98"->"98",
     "Lưu Diệc Phi"->"L", rỗng->"?", chỉ số màu ổn định.
4. **Chưa kiểm được ở VM** (nói thẳng): chạy thật trên máy (mở app, tải
   bảng bằng token thật, hiển thị avatar webp) — chờ bản CI đầu + verdict
   máy thật của user như mọi app trước. Gradle không chạy được trong VM
   này (daemon bị chặn socket) nên cổng biên dịch là harness kotlinc ở
   trên, đúng thông lệ các app trước.

## 6. Danh sách file (41 file)

```
.gitignore · README.md · BUILD-NOTES.md · WORKFLOW.txt
build.gradle · settings.gradle · gradle.properties
gradle/wrapper/gradle-wrapper.properties
app/build.gradle.kts · app/proguard-rules.pro
app/src/main/AndroidManifest.xml
app/src/main/java/com/example/museteam/data/
  Models.kt · Freshness.kt · ReportParser.kt · AvatarLogic.kt
  BoardSync.kt · GitHubFetcher.kt · BoardCache.kt · Settings.kt · BoardStore.kt
app/src/main/java/com/example/museteam/ui/
  MainActivity.kt · DetailActivity.kt · SettingsActivity.kt
  AvatarView.kt · UiUtil.kt
app/src/main/res/layout/
  activity_main.xml · activity_detail.xml · activity_settings.xml
  item_member.xml · item_account.xml
app/src/main/res/values/ colors.xml · strings.xml · themes.xml
app/src/main/res/drawable/
  bg_card.xml · bg_btn_gradient.xml · bg_btn_round.xml · bg_input.xml
  progress_blue.xml · ic_launcher_background.xml · ic_launcher_foreground.xml
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
```

## 7. Việc còn lại (Tổng 98 / user)

1. Tạo repo `MuseTeam-app` + nạp 4 secrets + dán workflow từ WORKFLOW.txt
   qua web editor -> build CI -> giao APK + checklist (Tổng 98 làm từ live
   chat; bản này KHÔNG tự push).
2. User mở app lần đầu -> Cài đặt -> dán token fine-grained chỉ-đọc repo
   `muse-board` -> app tự tải bảng (báo cáo thật của Tổng 98 đã có sẵn
   trên bảng).
3. Nhánh 97/96 dán lệnh thường trực báo cáo (HUONG-DAN-BAO-CAO.md ở
   muse-board-seed) để hết "chưa nối báo cáo".
