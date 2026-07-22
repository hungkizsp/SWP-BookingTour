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
        initUsersColumns();
        initBookingsColumns();
        initToursColumns();
        initTourGroupMessages();
        initUserNotifications();
        initTourItineraryDay();
        initTourChatGroups();
        initLoyaltyTransaction();
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

    private void initUsersColumns() {
        addColumnIfMissing("Users", "EmailVerified",
            "ALTER TABLE Users ADD EmailVerified BIT NOT NULL DEFAULT 0");
    }

    private void initBookingsColumns() {
        addColumnIfMissing("Bookings", "OccupiedSlots",
            "ALTER TABLE Bookings ADD OccupiedSlots INT NULL");
        addColumnIfMissing("Bookings", "CancellationReason",
            "ALTER TABLE Bookings ADD CancellationReason NVARCHAR(500) NULL");
        addColumnIfMissing("Bookings", "DiscountID",
            "ALTER TABLE Bookings ADD DiscountID BIGINT NULL");
        addColumnIfMissing("Bookings", "LoyaltyPointsUsed",
            "ALTER TABLE Bookings ADD LoyaltyPointsUsed INT NULL");
        addColumnIfMissing("Bookings", "LoyaltyDiscountAmount",
            "ALTER TABLE Bookings ADD LoyaltyDiscountAmount DECIMAL(10,2) NULL");
        addColumnIfMissing("Bookings", "DiscountAmount",
            "ALTER TABLE Bookings ADD DiscountAmount DECIMAL(18,2) NULL");
        addColumnIfMissing("Bookings", "DiscountCode",
            "ALTER TABLE Bookings ADD DiscountCode NVARCHAR(50) NULL");
    }

    private void initToursColumns() {
        addColumnIfMissing("Tours", "Latitude",
            "ALTER TABLE Tours ADD Latitude DECIMAL(9,6) NULL");
        addColumnIfMissing("Tours", "Longitude",
            "ALTER TABLE Tours ADD Longitude DECIMAL(9,6) NULL");
        addColumnIfMissing("Tours", "ChildPolicy",
            "ALTER TABLE Tours ADD ChildPolicy NVARCHAR(MAX) NULL");
        addColumnIfMissing("Tours", "SuitableAges",
            "ALTER TABLE Tours ADD SuitableAges NVARCHAR(200) NULL");
        addColumnIfMissing("Tours", "WhyChooseUs",
            "ALTER TABLE Tours ADD WhyChooseUs NVARCHAR(MAX) NULL");
        addColumnIfMissing("Tours", "BestTime",
            "ALTER TABLE Tours ADD BestTime NVARCHAR(200) NULL");
        addColumnIfMissing("Tours", "Inclusions",
            "ALTER TABLE Tours ADD Inclusions NVARCHAR(MAX) NULL");
        addColumnIfMissing("Tours", "Exclusions",
            "ALTER TABLE Tours ADD Exclusions NVARCHAR(MAX) NULL");
        addColumnIfMissing("Tours", "CityID",
            "ALTER TABLE Tours ADD CityID BIGINT NULL");
        addColumnIfMissing("Tours", "Source",
            "ALTER TABLE Tours ADD Source NVARCHAR(50) NULL");
        addColumnIfMissing("Tours", "ExternalId",
            "ALTER TABLE Tours ADD ExternalId NVARCHAR(100) NULL");
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
    // Bảng tour_itinerary_day (lịch trình theo ngày của tour)
    // =========================================================
    private void initTourItineraryDay() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'tour_itinerary_day'",
                Integer.class);

            if (count != null && count > 0) {
                log.info("  [OK] Table tour_itinerary_day already exists.");
                return;
            }

            jdbcTemplate.execute("""
                CREATE TABLE tour_itinerary_day (
                    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
                    tour_id          BIGINT NOT NULL,
                    day_number       INT NOT NULL,
                    title            NVARCHAR(255) NOT NULL,
                    description      NVARCHAR(MAX) NULL,
                    accommodation    NVARCHAR(255) NULL,
                    meals            NVARCHAR(100) NULL,
                    transportation   NVARCHAR(100) NULL,
                    highlights       NVARCHAR(MAX) NULL,
                    image_url        NVARCHAR(500) NULL,
                    CreatedAt        DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
                    UpdatedAt        DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
                    CONSTRAINT UQ_tour_itinerary_day UNIQUE (tour_id, day_number),
                    CONSTRAINT FK_itinerary_day_tour
                        FOREIGN KEY (tour_id) REFERENCES Tours(TourID)
                )
            """);
            log.info("  [CREATED] Table tour_itinerary_day.");
        } catch (Exception e) {
            log.error("  [ERROR] Failed to init tour_itinerary_day: {}", e.getMessage());
        }
    }

    // =========================================================
    // Bảng TourChatGroups / Members / Messages (group chat mới)
    // =========================================================
    private void initTourChatGroups() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'TourChatGroups'",
                Integer.class);

            if (count == null || count == 0) {
                jdbcTemplate.execute("""
                    CREATE TABLE TourChatGroups (
                        Id          BIGINT IDENTITY(1,1) PRIMARY KEY,
                        ScheduleID  BIGINT NOT NULL,
                        IsActive    BIT NOT NULL DEFAULT 1,
                        CreatedAt   DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
                        CONSTRAINT FK_ChatGroup_Schedule
                            FOREIGN KEY (ScheduleID) REFERENCES TourSchedules(ScheduleID)
                    )
                """);
                log.info("  [CREATED] Table TourChatGroups.");
            } else {
                log.info("  [OK] Table TourChatGroups already exists.");
            }
        } catch (Exception e) {
            log.error("  [ERROR] Failed to init TourChatGroups: {}", e.getMessage());
        }

        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'TourChatGroupMembers'",
                Integer.class);

            if (count == null || count == 0) {
                jdbcTemplate.execute("""
                    CREATE TABLE TourChatGroupMembers (
                        Id          BIGINT IDENTITY(1,1) PRIMARY KEY,
                        GroupID     BIGINT NOT NULL,
                        UserID      BIGINT NOT NULL,
                        JoinedAt    DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
                        CONSTRAINT UQ_ChatGroupMember UNIQUE (GroupID, UserID),
                        CONSTRAINT FK_ChatGroupMember_Group
                            FOREIGN KEY (GroupID) REFERENCES TourChatGroups(Id),
                        CONSTRAINT FK_ChatGroupMember_User
                            FOREIGN KEY (UserID) REFERENCES Users(UserID)
                    )
                """);
                log.info("  [CREATED] Table TourChatGroupMembers.");
            } else {
                log.info("  [OK] Table TourChatGroupMembers already exists.");
            }
        } catch (Exception e) {
            log.error("  [ERROR] Failed to init TourChatGroupMembers: {}", e.getMessage());
        }

        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'TourChatGroupMessages'",
                Integer.class);

            if (count == null || count == 0) {
                jdbcTemplate.execute("""
                    CREATE TABLE TourChatGroupMessages (
                        Id          BIGINT IDENTITY(1,1) PRIMARY KEY,
                        GroupID     BIGINT NOT NULL,
                        UserID      BIGINT NOT NULL,
                        Content     NVARCHAR(MAX) NOT NULL,
                        SentAt      DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
                        CONSTRAINT FK_ChatGroupMsg_Group
                            FOREIGN KEY (GroupID) REFERENCES TourChatGroups(Id),
                        CONSTRAINT FK_ChatGroupMsg_User
                            FOREIGN KEY (UserID) REFERENCES Users(UserID)
                    )
                """);
                log.info("  [CREATED] Table TourChatGroupMessages.");
            } else {
                log.info("  [OK] Table TourChatGroupMessages already exists.");
            }
        } catch (Exception e) {
            log.error("  [ERROR] Failed to init TourChatGroupMessages: {}", e.getMessage());
        }
    }

    // =========================================================
    // Bảng loyalty_transaction (lịch sử điểm tích lũy)
    // =========================================================
    private void initLoyaltyTransaction() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'loyalty_transaction'",
                Integer.class);

            if (count != null && count > 0) {
                log.info("  [OK] Table loyalty_transaction already exists.");
                return;
            }

            jdbcTemplate.execute("""
                CREATE TABLE loyalty_transaction (
                    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
                    user_id             BIGINT NOT NULL,
                    points              INT NOT NULL,
                    transaction_type    NVARCHAR(10) NOT NULL,
                    booking_id          BIGINT NULL,
                    description         NVARCHAR(255) NULL,
                    created_at          DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
                    CONSTRAINT FK_loyalty_user
                        FOREIGN KEY (user_id) REFERENCES Users(UserID),
                    CONSTRAINT FK_loyalty_booking
                        FOREIGN KEY (booking_id) REFERENCES Bookings(BookingID)
                )
            """);
            log.info("  [CREATED] Table loyalty_transaction.");
        } catch (Exception e) {
            log.error("  [ERROR] Failed to init loyalty_transaction: {}", e.getMessage());
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
}
