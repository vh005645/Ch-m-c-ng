# Chấm công ca-kíp (v3)

Build bằng GitHub Actions:
1. Tạo repo mới, giải nén zip và đẩy TOÀN BỘ nội dung (kể cả thư mục `.github`) lên nhánh `main`.
2. Vào tab Actions > "Build APK" > chờ chạy xong > tải artifact `ChamCong-APK` (file app-debug.apk).
3. Cài APK, mở app, chọn kíp ở tab "Cài đặt ca-kíp", rồi cấp các quyền ở mục "Quyền cần cấp".
4. Bấm "Thử popup sau 10 giây" và khóa màn hình để kiểm tra (popup có chữ THỬ NGHIỆM nhấp nháy).

## Thay đổi v3
- Chữ "Nhiệt Khí Thải" nằm dưới, chính giữa logo.
- Bỏ dòng "Nhắc nhở kế tiếp" ở tab Cài đặt ca-kíp. Ngày đã chọn trước là nghỉ phép / nghỉ tự do thì không báo.

## Thay đổi v2
- Header: logo (res/drawable-nodpi/logo_ht.png) + chữ "Nhiệt Khí Thải".
- Dòng ngày tháng năm + dòng "Kíp ... - trạng thái" (Ca 1 đầu, Ca 2 cuối, Tour xịn...).
- Ngày không làm việc hiện mờ chữ "tour".
- Thêm trạng thái "Nghỉ phép" (vẫn tính công, ô đỏ + chấm xanh góc dưới).
- Popup lần 1 có 3 nút: Chấm công / Nghỉ phép / Nghỉ tự do.
- 3 lần nhắc mỗi ca: trước giờ vào 5' (chấm công), sau giờ vào 5' (ký sổ), trước giờ về 5' (chấm công ra về).
- Chỉ nhắc ngày đi làm; ngày tour, ngày đã chọn nghỉ phép / nghỉ tự do không nhắc.
- Thanh tab nền đen, 2 tab ngăn cách rõ ràng.
