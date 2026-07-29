package com.tourbooking.booking.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Tự động kiểm tra và tạo các bảng DB cần thiết khi ứng dụng khởi động.
 * Đảm bảo mọi người pull code về đều có đủ schema mà không cần chạy script tay.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initialize() {
        log.info("=== DatabaseInitializer: Checking required tables... ===");
        initTourProgressLogs();
        initTourActivityImages();
        initTourScheduleColumns();
        initTourGroupMessages();
        initUserNotifications();
        initSecurityTables();
        log.info("=== DatabaseInitializer: Done. ===");
    }

    // =========================================================
    // Bảng TourProgressLogs (UC28 - Guide Update Progress)
    // =========================================================
    private void initTourProgressLogs() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'TourProgressLogs'",
                Integer.class);

            if (count != null && count > 0) {
                log.info("  [OK] Table TourProgressLogs already exists.");
                return;
            }

            jdbcTemplate.execute("""
                CREATE TABLE TourProgressLogs (
                    LogID       BIGINT IDENTITY(1,1) PRIMARY KEY,
                    ScheduleID  BIGINT NOT NULL,
                    Content     NVARCHAR(MAX) NULL,
                    CreatedAt   DATETIME NOT NULL DEFAULT GETDATE(),
                    UpdatedAt   DATETIME NOT NULL DEFAULT GETDATE(),
                    CONSTRAINT FK_ProgressLog_Schedule
                        FOREIGN KEY (ScheduleID) REFERENCES TourSchedules(ScheduleID)
                )
            """);
            log.info("  [CREATED] Table TourProgressLogs.");
        } catch (Exception e) {
            log.error("  [ERROR] Failed to init TourProgressLogs: {}", e.getMessage());
        }
    }

    // =========================================================
    // Bảng TourActivityImages (UC29 - Guide Upload Photos)
    // =========================================================
    private void initTourActivityImages() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'TourActivityImages'",
                Integer.class);

            if (count != null && count > 0) {
                log.info("  [OK] Table TourActivityImages already exists.");
                return;
            }

            jdbcTemplate.execute("""
                CREATE TABLE TourActivityImages (
                    ActivityImageID BIGINT IDENTITY(1,1) PRIMARY KEY,
                    ScheduleID      BIGINT NOT NULL,
                    ImageURL        NVARCHAR(500) NULL,
                    Caption         NVARCHAR(255) NULL,
                    CreatedAt       DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
                    UpdatedAt       DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
                    CONSTRAINT FK_ActivityImage_Schedule
                        FOREIGN KEY (ScheduleID) REFERENCES TourSchedules(ScheduleID)
                )
            """);
            log.info("  [CREATED] Table TourActivityImages.");
        } catch (Exception e) {
            log.error("  [ERROR] Failed to init TourActivityImages: {}", e.getMessage());
        }
    }

    // =========================================================
    // Bảng TourGroupMessages (Group chat theo TourSchedule)
    // =========================================================
    private void initTourGroupMessages() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'TourGroupMessages'",
                Integer.class);

            if (count != null && count > 0) {
                log.info("  [OK] Table TourGroupMessages already exists.");
                return;
            }

            jdbcTemplate.execute("""
                CREATE TABLE TourGroupMessages (
                    MessageID   BIGINT IDENTITY(1,1) PRIMARY KEY,
                    ScheduleID  BIGINT NOT NULL,
                    SenderID    BIGINT NOT NULL,
                    SenderRole  NVARCHAR(20) NULL,
                    Message     NVARCHAR(MAX) NULL,
                    SentAt      DATETIME2 NULL,
                    CreatedAt   DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
                    UpdatedAt   DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
                    CONSTRAINT FK_GroupMessage_Schedule
                        FOREIGN KEY (ScheduleID) REFERENCES TourSchedules(ScheduleID),
                    CONSTRAINT FK_GroupMessage_Sender
                        FOREIGN KEY (SenderID) REFERENCES Users(UserID)
                )
            """);
            log.info("  [CREATED] Table TourGroupMessages.");
        } catch (Exception e) {
            log.error("  [ERROR] Failed to init TourGroupMessages: {}", e.getMessage());
        }
    }

    // =========================================================
    // Thêm các cột cần thiết vào TourSchedules (nếu chưa có)
    // =========================================================
    private void initTourScheduleColumns() {
        addColumnIfMissing("TourSchedules", "GuideID",
            "ALTER TABLE TourSchedules ADD GuideID BIGINT NULL");

        addColumnIfMissing("TourSchedules", "CurrentProgress",
            "ALTER TABLE TourSchedules ADD CurrentProgress NVARCHAR(MAX) NULL");

        addColumnIfMissing("TourSchedules", "ReportContent",
            "ALTER TABLE TourSchedules ADD ReportContent NVARCHAR(MAX) NULL");

        addColumnIfMissing("TourSchedules", "ReportSubmittedAt",
            "ALTER TABLE TourSchedules ADD ReportSubmittedAt DATETIME2 NULL");
    }

    // =========================================================
    // Bảng UserNotifications (chuông thông báo cho người dùng)
    // =========================================================
    private void initUserNotifications() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'UserNotifications'",
                Integer.class);

            if (count != null && count > 0) {
                log.info("  [OK] Table UserNotifications already exists.");
                return;
            }

            jdbcTemplate.execute("""
                CREATE TABLE UserNotifications (
                    NotificationID  BIGINT IDENTITY(1,1) PRIMARY KEY,
                    UserID          BIGINT NOT NULL,
                    Title           NVARCHAR(200) NULL,
                    Message         NVARCHAR(MAX) NULL,
                    Type            NVARCHAR(50) NULL,
                    Link            NVARCHAR(500) NULL,
                    IsRead          BIT NOT NULL DEFAULT 0,
                    CreatedAt       DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
                    UpdatedAt       DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
                    CONSTRAINT FK_Notification_User
                        FOREIGN KEY (UserID) REFERENCES Users(UserID)
                )
            """);
            log.info("  [CREATED] Table UserNotifications.");
        } catch (Exception e) {
            log.error("  [ERROR] Failed to init UserNotifications: {}", e.getMessage());
        }
    }

    // =========================================================
    // Helper: thêm cột nếu chưa tồn tại
    // =========================================================
    private void addColumnIfMissing(String tableName, String columnName, String alterSql) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, tableName, columnName);

            if (count != null && count > 0) {
                log.info("  [OK] Column {}.{} already exists.", tableName, columnName);
                return;
            }

            jdbcTemplate.execute(alterSql);
            log.info("  [CREATED] Column {}.{}.", tableName, columnName);
        } catch (Exception e) {
            log.error("  [ERROR] Failed to add column {}.{}: {}", tableName, columnName, e.getMessage());
        }
    }

    // =========================================================
    // Bảng security_logs và blocked_ips (Security Module)
    // =========================================================
    private void initSecurityTables() {
        // --- security_logs ---
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'security_logs'",
                Integer.class);
            if (cnt != null && cnt > 0) {
                log.info("  [OK] Table security_logs already exists.");
            } else {
                jdbcTemplate.execute("""
                    CREATE TABLE security_logs (
                        id               BIGINT IDENTITY(1,1) PRIMARY KEY,
                        ip_address       NVARCHAR(64)  NOT NULL,
                        user_id          BIGINT        NULL,
                        user_email       NVARCHAR(255) NULL,
                        endpoint         NVARCHAR(255) NULL,
                        method           NVARCHAR(10)  NULL,
                        status_code      INT           NULL,
                        response_time_ms BIGINT        NULL,
                        status           NVARCHAR(20)  NULL,
                        created_at       DATETIME2     NOT NULL DEFAULT SYSDATETIME()
                    )
                """);
                jdbcTemplate.execute("CREATE INDEX idx_security_logs_ip ON security_logs(ip_address)");
                jdbcTemplate.execute("CREATE INDEX idx_security_logs_created ON security_logs(created_at)");
                log.info("  [CREATED] Table security_logs.");
            }
        } catch (Exception e) {
            log.error("  [ERROR] Failed to init security_logs: {}", e.getMessage());
        }

        // --- Migrate: add user_email column if missing (for existing DBs) ---
        try {
            Integer colCnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'security_logs' AND COLUMN_NAME = 'user_email'",
                Integer.class);
            if (colCnt == null || colCnt == 0) {
                jdbcTemplate.execute("ALTER TABLE security_logs ADD user_email NVARCHAR(255) NULL");
                log.info("  [MIGRATED] Added user_email column to security_logs.");
            }
        } catch (Exception e) {
            log.warn("  [WARN] Could not add user_email column to security_logs: {}", e.getMessage());
        }

        // --- blocked_ips ---
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'blocked_ips'",
                Integer.class);
            if (cnt != null && cnt > 0) {
                log.info("  [OK] Table blocked_ips already exists.");
            } else {
                jdbcTemplate.execute("""
                    CREATE TABLE blocked_ips (
                        id            BIGINT IDENTITY(1,1) PRIMARY KEY,
                        ip_address    NVARCHAR(64)  NOT NULL,
                        reason        NVARCHAR(255) NULL,
                        blocked_until DATETIME2     NOT NULL,
                        created_at    DATETIME2     NOT NULL DEFAULT SYSDATETIME()
                    )
                """);
                jdbcTemplate.execute("CREATE INDEX idx_blocked_ips_address ON blocked_ips(ip_address)");
                jdbcTemplate.execute("CREATE INDEX idx_blocked_ips_until ON blocked_ips(blocked_until)");
                log.info("  [CREATED] Table blocked_ips.");
            }
        } catch (Exception e) {
            log.error("  [ERROR] Failed to init blocked_ips: {}", e.getMessage());
        }
    }
}
