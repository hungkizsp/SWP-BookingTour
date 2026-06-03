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
}
