# BOOKING

## Giới Thiệu

BOOKING là hệ thống đặt tour du lịch được xây dựng theo mô hình tách biệt giữa backend và frontend nhằm bảo đảm khả năng phát triển độc lập, dễ bảo trì và thuận tiện cho việc mở rộng trong tương lai. Dự án mô phỏng các nghiệp vụ quan trọng của một nền tảng dịch vụ du lịch trực tuyến, bao gồm quản lý thông tin tour, hỗ trợ người dùng tra cứu và đặt tour, xác thực tài khoản, xử lý thanh toán, gửi thông báo và tích hợp với một số dịch vụ bên thứ ba.

Về mặt học thuật và thực tiễn, dự án được định hướng như một bài toán tổng hợp trong phát triển ứng dụng web hiện đại. Thông qua việc tổ chức hệ thống theo kiến trúc rõ ràng, dự án giúp thể hiện cách phân lớp trách nhiệm giữa giao diện người dùng, tầng xử lý nghiệp vụ và tầng dữ liệu. Đồng thời, đây cũng là môi trường phù hợp để áp dụng các công nghệ như Spring Boot, JPA, Spring Security, JWT, Mail, PayOS và Google OAuth vào một hệ thống có tính ứng dụng cao.

Phần backend đóng vai trò trung tâm trong việc xử lý logic nghiệp vụ, xác thực, phân quyền và làm việc với cơ sở dữ liệu. Phần frontend được xây dựng bằng HTML, CSS và JavaScript tĩnh, phục vụ hiển thị giao diện người dùng và tương tác với backend thông qua các API. Cách tổ chức này giúp dự án dễ triển khai trên môi trường local, đồng thời thuận lợi khi đóng gói ứng dụng hoàn chỉnh để chạy thử nghiệm hoặc trình bày báo cáo.

## Tổng Quan Hệ Thống

Hệ thống được chia thành 2 phần chính:

- `backend/`: Ứng dụng Spring Boot, quản lý API, nghiệp vụ, bảo mật, truy cập dữ liệu và tích hợp dịch vụ ngoài.
- `frontend/`: Giao diện tĩnh cho người dùng cuối và các trang quản trị, được tổ chức theo dạng tài nguyên web độc lập.

## Cấu Trúc Thư Mục Chính

- `backend/` - mã nguồn backend, cấu hình ứng dụng và các lớp xử lý nghiệp vụ.
- `frontend/static/assets/` - CSS, JavaScript, hình ảnh và các tài nguyên dùng chung.
- `frontend/static/pages/` - các trang HTML của hệ thống.
- `frontend/static/user/` - tài nguyên phục vụ luồng người dùng và các trang liên quan.
- `db/` - thư mục liên quan đến dữ liệu hoặc script hỗ trợ cơ sở dữ liệu.

## Công Nghệ Sử Dụng

- Java 21
- Spring Boot 3
- Spring Data JPA
- Spring Security
- JWT
- SQL Server
- Maven
- HTML, CSS, JavaScript
- PayOS
- Google OAuth
- JavaMail / Spring Mail

## Yêu Cầu Môi Trường

Trước khi chạy dự án, cần cài đặt và cấu hình:

- Java 21
- Maven
- Node.js và npm
- SQL Server
- PowerShell trên Windows để chạy script đồng bộ frontend nếu cần

Ngoài ra, cần đảm bảo các cổng mặc định không bị trùng:

- Backend: `8080`
- Frontend: `3000`

## Cấu Hình Trước Khi Chạy

### 1. Tạo cơ sở dữ liệu

Backend đang kết nối đến SQL Server với tên cơ sở dữ liệu:

`TourBookingDB`

Vì vậy, cần bảo đảm:

- SQL Server đang chạy.
- Tài khoản kết nối cơ sở dữ liệu có quyền phù hợp.
- Database `TourBookingDB` đã được tạo trước khi khởi động backend.

### 2. Cấu hình biến môi trường

Dự án sử dụng file `.env` ở thư mục gốc để nạp các thông tin cấu hình quan trọng. Backend sẽ tự đọc file này khi khởi động nếu file tồn tại.

Các biến môi trường thường cần có:

- `JWT_SECRET`
- `SMTP_USERNAME`
- `SMTP_PASSWORD`
- `PAYOS_CLIENT_ID`
- `PAYOS_API_KEY`
- `PAYOS_CHECKSUM_KEY`
- `GEMINI_API_KEY`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`

File `backend/src/main/resources/application.properties` đã được cấu hình để lấy giá trị từ biến môi trường, vì vậy bạn không nên ghi trực tiếp secret vào mã nguồn.

## Hướng Dẫn Chạy Dự Án

### Bước 1: Clone dự án

```bash
git clone https://github.com/hungkizsp/SWP-BookingTour.git
cd BOOKING
```

### Bước 2: Cài đặt frontend

```bash
cd frontend
npm install
```

Lệnh này sẽ cài các gói cần thiết cho frontend, bao gồm `browser-sync` để chạy giao diện local.

### Bước 3: Chạy frontend ở chế độ phát triển

```bash
cd frontend
npm run dev
```

Frontend sẽ chạy tại:

`http://localhost:3000`

Chế độ này phù hợp khi bạn muốn kiểm tra giao diện, chỉnh sửa HTML/CSS/JS và xem kết quả ngay lập tức.

### Bước 4: Chạy backend

```bash
cd backend
mvn spring-boot:run
```

Backend sẽ chạy tại:

`http://localhost:8080`

Khi khởi động, ứng dụng sẽ:

- Nạp cấu hình từ biến môi trường hoặc file `.env`.
- Kết nối đến SQL Server.
- Khởi tạo các thành phần bảo mật, JPA, Mail và các dịch vụ tích hợp khác.

### Bước 5: Đồng bộ giao diện vào backend khi cần

Khi thay đổi giao diện trong `frontend/static/assets/` hoặc `frontend/static/pages/`, bạn có thể đồng bộ sang backend bằng script:

```bash
cd frontend
./scripts/sync.ps1
```

Script này sao chép các tài nguyên giao diện cần thiết vào `backend/src/main/resources/static` để backend có thể phục vụ trực tiếp sau khi build.

## Build Ứng Dụng

Khi cần đóng gói dự án để chạy như một ứng dụng hoàn chỉnh, thực hiện build backend:

```bash
cd backend
mvn clean package
```

Sau khi build xong, file `.jar` sẽ nằm trong thư mục `target/`. Bạn có thể chạy trực tiếp bằng:

```bash
java -jar target/booking-0.0.1-SNAPSHOT.jar
```

## Luồng Chạy Khuyến Nghị

Để làm việc thuận tiện trong quá trình phát triển, có thể chạy theo trình tự sau:

1. Cài dependency cho frontend bằng `npm install`.
2. Khởi động frontend bằng `npm run dev` để kiểm tra giao diện.
3. Khởi động backend bằng `mvn spring-boot:run`.
4. Kiểm tra đăng nhập, đặt tour, thanh toán và các chức năng nghiệp vụ trên trình duyệt.
5. Khi hoàn tất thay đổi giao diện, đồng bộ tài nguyên và build backend bằng `mvn clean package`.

## Ghi Chú Quan Trọng

- Không đưa secret thật trực tiếp vào Git.
- Nếu thay đổi biến môi trường, cần khởi động lại backend để cấu hình mới có hiệu lực.
- Nếu backend không kết nối được CSDL, hãy kiểm tra lại tên database, tài khoản đăng nhập và trạng thái dịch vụ SQL Server.
- Nếu frontend không hiển thị đúng tài nguyên sau khi build, hãy kiểm tra lại việc đồng bộ thư mục `assets` và `pages`.

## Kết Luận

Dự án BOOKING được xây dựng theo hướng rõ ràng, dễ mở rộng và phù hợp cho cả mục đích thực hành lẫn trình bày báo cáo. Việc tách riêng backend và frontend giúp quá trình phát triển trở nên linh hoạt hơn, đồng thời tạo điều kiện thuận lợi cho kiểm thử, bảo trì và đóng gói hệ thống trong các giai đoạn tiếp theo.
