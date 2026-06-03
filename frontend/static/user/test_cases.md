# Kịch Bản Kiểm Thử (Test Cases) - Customer Use Cases (UC12 - UC26)

Tài liệu này mô tả chi tiết các kịch bản kiểm thử (Test Cases) thủ công dành cho chức năng của khách hàng (Customer), bao gồm việc đặt tour, thanh toán, quản lý lịch sử và cập nhật thông tin cá nhân.

---

## 1. Đặt Tour & Thanh Toán (UC12 - UC18)

### TC_UC12_01: Đặt Tour thành công với đầy đủ thông tin
- **Pre-condition (Tiền điều kiện):** Người dùng đang ở trang chi tiết tour và chọn "Đặt ngay".
- **Steps (Các bước):**
  1. Nhập Họ và Tên.
  2. Nhập Email hợp lệ.
  3. Nhập Số điện thoại.
  4. Chọn Ngày khởi hành.
  5. Nhập số lượng khách (Ví dụ: 2).
  6. Chọn phương thức thanh toán.
  7. Bấm "Xác Nhận Đặt Tour".
- **Expected Result (Kết quả mong đợi):** Chuyển hướng đến cổng thanh toán, sau đó thông báo đặt tour thành công và chuyển về trang Lịch sử đặt tour.

### TC_UC13_01: Kiểm tra chỗ trống (Check Available Slots)
- **Pre-condition:** Đang ở form đặt tour.
- **Steps:** Chọn một lịch trình (Ngày khởi hành) trong dropdown.
- **Expected Result:** Hiển thị thông báo số lượng chỗ trống còn lại. Nếu chọn ngày **đã hết chỗ**, hệ thống báo lỗi không cho phép đặt hoặc vô hiệu hóa nút đặt.

### TC_UC14_01: Tính tổng tiền tự động (Calculate Total Price)
- **Pre-condition:** Giá vé cơ bản của tour là 3.500.000 VNĐ.
- **Steps:** Đổi số lượng khách từ 1 thành 3.
- **Expected Result:** Giao diện hiển thị tổng tiền là `10.500.000 VNĐ` ngay lập tức mà không cần tải lại trang.

### TC_UC15_01: Áp dụng mã giảm giá hợp lệ
- **Pre-condition:** Mã giảm giá `SUMMER2026` (giảm 500k) đang hoạt động.
- **Steps:** Nhập `SUMMER2026` vào ô voucher và bấm "Áp dụng".
- **Expected Result:** Dòng "Giảm giá" xuất hiện với số tiền `-500.000 VNĐ`. Tổng tiền thanh toán giảm đi tương ứng. Thông báo "Áp dụng thành công" màu xanh.

### TC_UC15_02: Áp dụng mã giảm giá sai
- **Steps:** Nhập `SAIMARCL` vào ô voucher và bấm "Áp dụng".
- **Expected Result:** Báo lỗi chữ đỏ: "Mã không hợp lệ hoặc đã hết hạn!". Tổng tiền không thay đổi.

### TC_UC16_01 & TC_UC18_01: Chọn phương thức thanh toán và Trả góp
- **Steps:**
  1. Tích chọn "Thanh toán VNPay".
  2. Tích chọn "Thanh toán trả góp 0%".
- **Expected Result:** Chỉ một phương thức hoặc (Radio button) được chọn một lúc. Bấm Xác nhận sẽ xử lý luồng tương ứng.

---

## 2. Lịch Sử & Quản Lý Chuyến Đi (UC19 - UC23)

### TC_UC19_01: Xem danh sách lịch sử đặt tour
- **Pre-condition:** Người dùng đã đăng nhập và đã từng đặt 2 tour.
- **Steps:** Truy cập mục "Lịch sử đặt tour".
- **Expected Result:** Hiển thị danh sách 2 tour với đầy đủ trạng thái (Đang chờ, Đã hoàn thành), giá tiền, ngày đi.

### TC_UC20_01: Hủy Booking thành công
- **Pre-condition:** Đơn hàng trạng thái "Đang chờ khởi hành" và đủ điều kiện thời gian hủy (trước 7 ngày).
- **Steps:**
  1. Bấm nút "Hủy Đặt Tour" trên đơn hàng.
  2. Xác nhận thông báo trên Modal.
- **Expected Result:** Cập nhật trạng thái thành "Đã hủy". Nút Hủy và Nút Thanh Toán biến mất.

### TC_UC21_01: Yêu cầu hoàn tiền (Request Refund)
- **Pre-condition:** Người dùng đang hủy đơn hàng có giá trị hoàn tiền (Đã thanh toán trước).
- **Steps:**
  1. Trên Modal Hủy Tour, điền số tài khoản ngân hàng.
  2. Bấm "Xác nhận Hủy".
- **Expected Result:** Yêu cầu hoàn tiền được ghi nhận vào hệ thống (Status Pending Refund) và sẽ được Staff xử lý.

### TC_UC22_01: Tải hóa đơn PDF
- **Pre-condition:** Đơn đặt tour đã thanh toán.
- **Steps:** Bấm nút "Tải Hóa Đơn PDF".
- **Expected Result:** Hệ thống tải xuống 1 file PDF chứa thông tin mã số thuế, mã đơn hàng, ngày thanh toán hợp lệ.

### TC_UC23_01: Viết đánh giá sau chuyến đi
- **Pre-condition:** Trạng thái đơn là "Đã hoàn thành".
- **Steps:**
  1. Bấm nút "Đánh giá chuyến đi".
  2. Chọn 5 sao và nhập "Tour rất tuyệt".
  3. Bấm Gửi.
- **Expected Result:** Nút Đánh giá biến mất hoặc chuyển thành "Xem đánh giá của bạn". Record lưu vào database thành công.

---

## 3. Hồ Sơ Người Dùng (UC24 - UC26)

### TC_UC24_01: Upload tài liệu định danh (CMND/Passport) hợp lệ
- **Pre-condition:** File có định dạng `.jpg` hoặc `.pdf` và dưới 5MB.
- **Steps:** Click "Bấm để chọn file", tải file định dạng hợp lệ lên.
- **Expected Result:** Thanh progress bar tải file hoàn tất, hiển thị dòng xanh "Đã tải lên: passport.jpg".

### TC_UC24_02: Upload tài liệu vượt quá kích thước
- **Pre-condition:** File dung lượng 10MB.
- **Steps:** Chọn file và tải lên.
- **Expected Result:** Hiển thị thông báo "File vuợt quá 5MB. Vui lòng chọn file khác." và upload bị hủy.

### TC_UC25_01: Cập nhật thông tin cá nhân
- **Steps:**
  1. Sửa "Số điện thoại" đang trống thành `0987654321`.
  2. Bấm "Lưu Thay Đổi".
- **Expected Result:** Thông báo "Cập nhật thành công". Load lại trang số điện thoại vẫn là `0987654321`.

### TC_UC26_01: Kiểm tra hiển thị điểm thưởng (Loyalty Points)
- **Pre-condition:** Tích lũy đủ điểm sau khi đi 2 chuyến (ví dụ 1,250 điểm ~ Hạng Vàng).
- **Steps:** Truy cập "Tài khoản của tôi".
- **Expected Result:** Hiển thị nổi bật Widget: "Hạng Vàng (Gold Member)" - "1,250 Điểm tích lũy". Điểm này khớp với database.
