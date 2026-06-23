IF DB_ID(N'TourBookingDB') IS NULL
BEGIN
    EXEC('CREATE DATABASE TourBookingDB');
END
GO

USE TourBookingDB;
GO

IF OBJECT_ID(N'dbo.Categories', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Categories (
        CategoryID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        CategoryName NVARCHAR(100) NOT NULL UNIQUE,
        Description NVARCHAR(255) NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME()
    );
END
GO

IF OBJECT_ID(N'dbo.Cities', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Cities (
        CityID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        CityName NVARCHAR(100) NOT NULL UNIQUE,
        CenterLatitude DECIMAL(9,6) NOT NULL,
        CenterLongitude DECIMAL(9,6) NOT NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME()
    );
END
GO

IF OBJECT_ID(N'dbo.Users', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Users (
        UserID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        FullName NVARCHAR(100) NULL,
        Email NVARCHAR(100) NOT NULL UNIQUE,
        PasswordHash NVARCHAR(255) NULL,
        Role NVARCHAR(20) NULL DEFAULT 'CUSTOMER',
        AvatarURL NVARCHAR(255) NULL,
        PhoneNumber NVARCHAR(20) NULL,
        Address NVARCHAR(255) NULL,
        Gender NVARCHAR(20) NULL,
        DateOfBirth DATE NULL,
        Bio NVARCHAR(MAX) NULL,
        ExperienceYears INT NULL,
        IsActive BIT NOT NULL DEFAULT 1,
        CurrentSessionID NVARCHAR(64) NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME()
    );
END
GO

IF OBJECT_ID(N'dbo.Tokens', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Tokens (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        token NVARCHAR(500) NOT NULL,
        email NVARCHAR(150) NULL,
        expiryDate DATETIME2 NULL,
        used BIT NOT NULL DEFAULT 0,
        type NVARCHAR(50) NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME()
    );
END
GO

IF OBJECT_ID(N'dbo.Tours', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Tours (
        TourID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        TourName NVARCHAR(200) NULL,
        Description NVARCHAR(MAX) NULL,
        Itinerary NVARCHAR(MAX) NULL,
        Price DECIMAL(10,2) NULL,
        Duration INT NULL,
        StartLocation NVARCHAR(100) NULL,
        EndLocation NVARCHAR(100) NULL,
        Latitude DECIMAL(9,6) NULL,
        Longitude DECIMAL(9,6) NULL,
        TransportType NVARCHAR(50) NULL,
        ChildPolicy NVARCHAR(MAX) NULL,
        SuitableAges NVARCHAR(200) NULL,
        WhyChooseUs NVARCHAR(MAX) NULL,
        BestTime NVARCHAR(200) NULL,
        Inclusions NVARCHAR(MAX) NULL,
        Exclusions NVARCHAR(MAX) NULL,
        CategoryID BIGINT NULL,
        CityID BIGINT NULL,
        Rating FLOAT NOT NULL DEFAULT 0.0,
        Source NVARCHAR(50) NULL DEFAULT 'LOCAL',
        ExternalId NVARCHAR(100) NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_Tours_Categories FOREIGN KEY (CategoryID) REFERENCES dbo.Categories(CategoryID),
        CONSTRAINT FK_Tours_Cities FOREIGN KEY (CityID) REFERENCES dbo.Cities(CityID)
    );
END
GO

IF OBJECT_ID(N'dbo.TourImages', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.TourImages (
        ImageID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        TourID BIGINT NULL,
        ImageURL NVARCHAR(500) NOT NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_TourImages_Tours FOREIGN KEY (TourID) REFERENCES dbo.Tours(TourID)
    );
END
GO

IF OBJECT_ID(N'dbo.TourHighlights', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.TourHighlights (
        HighlightID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        TourID BIGINT NULL,
        Highlight NVARCHAR(255) NOT NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_TourHighlights_Tours FOREIGN KEY (TourID) REFERENCES dbo.Tours(TourID)
    );
END
GO

IF OBJECT_ID(N'dbo.TourFaqs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.TourFaqs (
        FaqID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        TourID BIGINT NULL,
        Question NVARCHAR(500) NOT NULL,
        Answer NVARCHAR(MAX) NOT NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_TourFaqs_Tours FOREIGN KEY (TourID) REFERENCES dbo.Tours(TourID)
    );
END
GO

IF OBJECT_ID(N'dbo.TourSchedules', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.TourSchedules (
        ScheduleID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        TourID BIGINT NOT NULL,
        GuideID BIGINT NULL,
        StartDate DATE NULL,
        EndDate DATE NULL,
        AvailableSlots INT NULL,
        MaxSlots INT NULL,
        Status NVARCHAR(50) NULL DEFAULT 'OPEN',
        CurrentProgress NVARCHAR(MAX) NULL,
        ReportContent NVARCHAR(MAX) NULL,
        ReportSubmittedAt DATETIME2 NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_TourSchedules_Tours FOREIGN KEY (TourID) REFERENCES dbo.Tours(TourID),
        CONSTRAINT FK_TourSchedules_Guide FOREIGN KEY (GuideID) REFERENCES dbo.Users(UserID)
    );
END
GO

IF OBJECT_ID(N'dbo.TourActivityImages', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.TourActivityImages (
        ActivityImageID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        ScheduleID BIGINT NOT NULL,
        ImageURL NVARCHAR(500) NULL,
        Caption NVARCHAR(255) NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_TourActivityImages_Schedules FOREIGN KEY (ScheduleID) REFERENCES dbo.TourSchedules(ScheduleID)
    );
END
GO

IF OBJECT_ID(N'dbo.Bookings', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Bookings (
        BookingID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        UserID BIGINT NOT NULL,
        ScheduleID BIGINT NOT NULL,
        BookingDate DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        NumberOfPeople INT NULL,
        TotalPrice DECIMAL(12,2) NULL,
        Status NVARCHAR(50) NULL DEFAULT 'PENDING',
        DiscountAmount DECIMAL(18,2) NULL,
        DiscountCode NVARCHAR(50) NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_Bookings_Users FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID),
        CONSTRAINT FK_Bookings_Schedules FOREIGN KEY (ScheduleID) REFERENCES dbo.TourSchedules(ScheduleID)
    );
END
GO

IF OBJECT_ID(N'dbo.Passengers', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Passengers (
    [PassengerID] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [BookingID] BIGINT NOT NULL,
    [FullName] NVARCHAR(200) NOT NULL,
    [DateOfBirth] DATE NULL,
    [IdNumber] VARCHAR(50) NULL,
    [PassengerType] VARCHAR(20) NOT NULL,
    [CreatedAt] DATETIME2 NOT NULL DEFAULT GETDATE(),
    [UpdatedAt] DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT [FK_Passengers_Bookings] FOREIGN KEY ([BookingID]) REFERENCES [dbo].[Bookings] ([BookingID]) ON DELETE CASCADE
    );
END
GO

IF OBJECT_ID(N'dbo.Payments', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Payments (
        PaymentID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        BookingID BIGINT NOT NULL,
        Amount DECIMAL(10,2) NULL,
        PaymentMethod NVARCHAR(50) NULL,
        TransactionCode NVARCHAR(100) NULL,
        PaymentDate DATETIME2 NULL,
        Status NVARCHAR(50) NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_Payments_Bookings FOREIGN KEY (BookingID) REFERENCES dbo.Bookings(BookingID)
    );
END
GO

IF OBJECT_ID(N'dbo.PaymentLogs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PaymentLogs (
        LogID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        PaymentID BIGINT NOT NULL,
        LogMessage NVARCHAR(MAX) NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_PaymentLogs_Payments FOREIGN KEY (PaymentID) REFERENCES dbo.Payments(PaymentID)
    );
END
GO

IF OBJECT_ID(N'dbo.Reviews', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Reviews (
        ReviewID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        UserID BIGINT NOT NULL,
        TourID BIGINT NOT NULL,
        Rating INT NULL,
        Comment NVARCHAR(MAX) NULL,
        ReviewDate DATETIME2 NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT UQ_Reviews_User_Tour UNIQUE (UserID, TourID),
        CONSTRAINT FK_Reviews_Users FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID),
        CONSTRAINT FK_Reviews_Tours FOREIGN KEY (TourID) REFERENCES dbo.Tours(TourID)
    );
END
GO

IF OBJECT_ID(N'dbo.Wishlist', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Wishlist (
        WishlistID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        UserID BIGINT NOT NULL,
        TourID BIGINT NOT NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT UQ_Wishlist_User_Tour UNIQUE (UserID, TourID),
        CONSTRAINT FK_Wishlist_Users FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID),
        CONSTRAINT FK_Wishlist_Tours FOREIGN KEY (TourID) REFERENCES dbo.Tours(TourID)
    );
END
GO

IF OBJECT_ID(N'dbo.Newsletters', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Newsletters (
        SubscriberID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        Email NVARCHAR(100) NOT NULL UNIQUE,
        SubscribedAt DATETIME2 NULL DEFAULT SYSDATETIME(),
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME()
    );
END
GO

IF OBJECT_ID(N'dbo.ChatSessions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ChatSessions (
        SessionID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        UserID BIGINT NULL,
        GuestId NVARCHAR(50) NULL,
        Status NVARCHAR(30) NULL,
        LastMessageAt DATETIME2 NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_ChatSessions_Users FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID)
    );
END
GO

IF OBJECT_ID(N'dbo.ChatMessages', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ChatMessages (
        MessageID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        UserID BIGINT NULL,
        SenderType NVARCHAR(20) NULL,
        Message NVARCHAR(MAX) NULL,
        GuestId NVARCHAR(50) NULL,
        SentAt DATETIME2 NULL DEFAULT SYSDATETIME(),
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_ChatMessages_Users FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID)
    );
END
GO

IF OBJECT_ID(N'dbo.ChatEscalations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ChatEscalations (
        EscalationID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        CustomerID BIGINT NULL,
        GuestID NVARCHAR(50) NULL,
        RequestNote NVARCHAR(MAX) NULL,
        MeetingPreference NVARCHAR(255) NULL,
        Status NVARCHAR(20) NULL,
        AssignedStaffID BIGINT NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_ChatEscalations_Customers FOREIGN KEY (CustomerID) REFERENCES dbo.Users(UserID),
        CONSTRAINT FK_ChatEscalations_Staff FOREIGN KEY (AssignedStaffID) REFERENCES dbo.Users(UserID)
    );
END
GO

IF OBJECT_ID(N'dbo.Discounts', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Discounts (
        DiscountID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        Code NVARCHAR(50) NOT NULL UNIQUE,
        DiscountType NVARCHAR(20) NOT NULL,
        Value DECIMAL(10,2) NOT NULL,
        StartDate DATETIME2 NULL,
        EndDate DATETIME2 NULL,
        UsageLimit INT NULL,
        CurrentUsage INT NOT NULL DEFAULT 0,
        IsActive BIT NOT NULL DEFAULT 1,
        MinimumBookingAmount DECIMAL(10,2) NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME()
    );
END
GO

IF OBJECT_ID(N'dbo.Documents', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Documents (
        DocumentID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        UserID BIGINT NOT NULL,
        FileUrl NVARCHAR(500) NULL,
        Type NVARCHAR(100) NULL,
        UploadedAt DATETIME2 NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_Documents_Users FOREIGN KEY (UserID) REFERENCES dbo.Users(UserID)
    );
END
GO

IF OBJECT_ID(N'dbo.RefundRequests', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.RefundRequests (
        RefundID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        BookingID BIGINT NOT NULL,
        Amount DECIMAL(10,2) NULL,
        Reason NVARCHAR(MAX) NULL,
        Status NVARCHAR(50) NULL DEFAULT 'PENDING',
        StaffNote NVARCHAR(MAX) NULL,
        ProcessedAt DATETIME2 NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_RefundRequests_Bookings FOREIGN KEY (BookingID) REFERENCES dbo.Bookings(BookingID)
    );
END
GO

IF OBJECT_ID(N'dbo.PrivacyPolicies', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PrivacyPolicies (
        PolicyID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        Title NVARCHAR(255) NOT NULL,
        Content NVARCHAR(MAX) NOT NULL,
        IsActive BIT NOT NULL DEFAULT 1,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME()
    );
END
GO

IF OBJECT_ID(N'dbo.TourProgressLogs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.TourProgressLogs (
        LogID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        ScheduleID BIGINT NOT NULL,
        Content NVARCHAR(MAX) NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_TourProgressLogs_Schedules FOREIGN KEY (ScheduleID) REFERENCES dbo.TourSchedules(ScheduleID)
    );
END
GO

IF OBJECT_ID(N'dbo.Invoice', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Invoice (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        booking_id BIGINT NULL,
        invoiceNumber NVARCHAR(255) NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_Invoice_Bookings FOREIGN KEY (booking_id) REFERENCES dbo.Bookings(BookingID)
    );
END
GO

IF OBJECT_ID(N'dbo.LoyaltyPoint', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.LoyaltyPoint (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        user_UserID BIGINT NOT NULL UNIQUE,
        points INT NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_LoyaltyPoint_Users FOREIGN KEY (user_UserID) REFERENCES dbo.Users(UserID)
    );
END
GO

IF COL_LENGTH('dbo.Tours', 'TransportType') IS NULL
BEGIN
    ALTER TABLE dbo.Tours ADD TransportType NVARCHAR(50) NULL;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'idx_tours_transport_type'
      AND object_id = OBJECT_ID('dbo.Tours')
)
BEGIN
    CREATE INDEX idx_tours_transport_type ON dbo.Tours(TransportType);
END
GO

IF OBJECT_ID('dbo.TourBookingStats', 'V') IS NULL
BEGIN
    EXEC('
        CREATE VIEW dbo.TourBookingStats AS
        SELECT
            t.TourID AS TourID,
            COUNT(b.BookingID) AS BookingCount
        FROM dbo.Tours t
        LEFT JOIN dbo.TourSchedules ts ON ts.TourID = t.TourID
        LEFT JOIN dbo.Bookings b ON b.ScheduleID = ts.ScheduleID
        GROUP BY t.TourID
    ');
END
GO

-- Bổ sung cột cho các Edge Cases mới:
-- 1. Thêm AssignedStaffID vào ChatSessions để lưu nhân viên đang chat
IF COL_LENGTH('dbo.ChatSessions', 'AssignedStaffID') IS NULL
BEGIN
    ALTER TABLE dbo.ChatSessions ADD AssignedStaffID BIGINT NULL;
    ALTER TABLE dbo.ChatSessions ADD CONSTRAINT FK_ChatSessions_Staff FOREIGN KEY (AssignedStaffID) REFERENCES dbo.Users(UserID);
END
GO

-- 2. Thêm OriginalBookingStatus vào RefundRequests để phục hồi trạng thái gốc khi từ chối hoàn tiền
IF COL_LENGTH('dbo.RefundRequests', 'OriginalBookingStatus') IS NULL
BEGIN
    ALTER TABLE dbo.RefundRequests ADD OriginalBookingStatus NVARCHAR(50) NULL;
END
GO

-- 3. Passenger & Booking: hỗ trợ ADULT / CHILD / INFANT (Customer Booking Flow)
-- IdNumber cho phép NULL (trẻ em & em bé chưa bắt buộc có CCCD/Passport riêng)
IF COL_LENGTH('dbo.Passengers', 'IdNumber') IS NOT NULL
BEGIN
    ALTER TABLE dbo.Passengers ALTER COLUMN IdNumber VARCHAR(50) NULL;
END
GO

-- OccupiedSlots: số chỗ thực tế trừ khỏi TourSchedule (ADULT + CHILD; INFANT không chiếm chỗ)
IF COL_LENGTH('dbo.Bookings', 'OccupiedSlots') IS NULL
BEGIN
    ALTER TABLE dbo.Bookings ADD OccupiedSlots INT NULL;
END
GO

-- Backfill: booking cũ coi toàn bộ NumberOfPeople là chỗ đã chiếm
UPDATE dbo.Bookings
SET OccupiedSlots = NumberOfPeople
WHERE OccupiedSlots IS NULL AND NumberOfPeople IS NOT NULL;
GO

-- Ràng buộc PassengerType chỉ nhận 3 giá trị chuẩn ngành lữ hành
IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE name = 'CK_Passengers_PassengerType'
      AND parent_object_id = OBJECT_ID('dbo.Passengers')
)
BEGIN
    ALTER TABLE dbo.Passengers
    ADD CONSTRAINT CK_Passengers_PassengerType
        CHECK (PassengerType IN ('ADULT', 'CHILD', 'INFANT'));
END
GO

IF OBJECT_ID(N'dbo.DiscountPolicies', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.DiscountPolicies (
        PolicyID BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        PassengerType VARCHAR(50) NOT NULL UNIQUE,
        Rate DECIMAL(5,2) NOT NULL,
        IsActive BIT NOT NULL DEFAULT 1,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME()
    );
    
    INSERT INTO dbo.DiscountPolicies (PassengerType, Rate, IsActive) VALUES ('CHILD', 0.75, 1);
    INSERT INTO dbo.DiscountPolicies (PassengerType, Rate, IsActive) VALUES ('INFANT', 0.10, 1);
END
GO
-- ============================================================
-- REVIEW MODULE REFACTOR: 1 User + 1 Tour = 1 Review => 1 Booking = 1 Review
-- ============================================================

-- Step 1: DROP CONSTRAINT UQ_Reviews_User_Tour (old uniqueness guard)
IF EXISTS (
    SELECT 1 FROM sys.key_constraints 
    WHERE name = 'UQ_Reviews_User_Tour' AND parent_object_id = OBJECT_ID('dbo.Reviews')
)
BEGIN
    ALTER TABLE dbo.Reviews DROP CONSTRAINT UQ_Reviews_User_Tour;
END
GO

-- Step 2: DROP CONSTRAINT FK_Reviews_Tours (old Tour foreign key)
IF EXISTS (
    SELECT 1 FROM sys.foreign_keys 
    WHERE name = 'FK_Reviews_Tours' AND parent_object_id = OBJECT_ID('dbo.Reviews')
)
BEGIN
    ALTER TABLE dbo.Reviews DROP CONSTRAINT FK_Reviews_Tours;
END
GO

-- Step 3: ADD COLUMN BookingID BIGINT NULL
IF COL_LENGTH('dbo.Reviews', 'BookingID') IS NULL
BEGIN
    ALTER TABLE dbo.Reviews ADD BookingID BIGINT NULL;
END
GO

-- Step 4: ADD CONSTRAINT FK_Reviews_Bookings -> Bookings(BookingID)
IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys 
    WHERE name = 'FK_Reviews_Bookings' AND parent_object_id = OBJECT_ID('dbo.Reviews')
)
BEGIN
    ALTER TABLE dbo.Reviews 
    ADD CONSTRAINT FK_Reviews_Bookings FOREIGN KEY (BookingID) REFERENCES dbo.Bookings(BookingID);
END
GO

-- Step 5: Tạo Filtered Unique Index thay vì UNIQUE Constraint thông thường
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'UQ_Reviews_Booking' AND object_id = OBJECT_ID('dbo.Reviews')
)
BEGIN
    -- Chỉ ép UNIQUE đối với những dòng có dữ liệu BookingID (khác NULL)
    CREATE UNIQUE NONCLUSTERED INDEX UQ_Reviews_Booking 
    ON dbo.Reviews(BookingID) 
    WHERE BookingID IS NOT NULL;
END
GO
-- 1. Kiểm tra và xóa ràng buộc khóa ngoại cũ nếu nó còn sót lại
IF EXISTS (
    SELECT 1 FROM sys.foreign_keys 
    WHERE name = 'FK_Reviews_Tours' AND parent_object_id = OBJECT_ID('dbo.Reviews')
)
BEGIN
    ALTER TABLE dbo.Reviews DROP CONSTRAINT FK_Reviews_Tours;
END
GO

-- 2. Xóa bỏ hẳn cột TourID ra khỏi bảng Reviews
IF COL_LENGTH('dbo.Reviews', 'TourID') IS NOT NULL
BEGIN
    ALTER TABLE dbo.Reviews DROP COLUMN TourID;
END
GO

-- ============================================================
-- TOUR SCHEDULE REFACTOR: Guaranteed Departure Model
-- Add BookingDeadline, DepartureTime, ReturnTime columns
-- ============================================================

-- 1. BookingDeadline: the cut-off datetime after which new bookings are rejected.
--    NULL means "no explicit deadline" — the backend will default to the departure datetime.
IF COL_LENGTH('dbo.TourSchedules', 'BookingDeadline') IS NULL
BEGIN
    ALTER TABLE dbo.TourSchedules ADD BookingDeadline DATETIME2 NULL;
END
GO

-- 2. DepartureTime: the clock time on StartDate when the tour departs (e.g. 07:00).
--    Stored as TIME so it can be combined with StartDate in application code.
IF COL_LENGTH('dbo.TourSchedules', 'DepartureTime') IS NULL
BEGIN
    ALTER TABLE dbo.TourSchedules ADD DepartureTime TIME NULL;
END
GO

-- 3. ReturnTime: the clock time on EndDate when the tour returns (e.g. 18:00).
IF COL_LENGTH('dbo.TourSchedules', 'ReturnTime') IS NULL
BEGIN
    ALTER TABLE dbo.TourSchedules ADD ReturnTime TIME NULL;
END
GO

-- 4. Backfill: for existing rows where BookingDeadline is NULL,
--    set it to midnight of StartDate (safe fallback — guests could already book up to the departure day).
UPDATE dbo.TourSchedules
SET BookingDeadline = CAST(CAST(StartDate AS DATETIME2) AS DATETIME2)
WHERE BookingDeadline IS NULL AND StartDate IS NOT NULL;
GO

-- 5. Migrate legacy Status values to the new ScheduleStatus vocabulary.
--    ACTIVE  -> OPEN      (ACTIVE was an old informal status, now normalised)
--    FULL    -> SOLD_OUT  (FULL is replaced by SOLD_OUT)
UPDATE dbo.TourSchedules SET Status = 'OPEN'      WHERE Status = 'ACTIVE';
UPDATE dbo.TourSchedules SET Status = 'SOLD_OUT'  WHERE Status = 'FULL';
GO

-- 6. Add CHECK constraint to lock valid Status values.
--    Only add if it does not already exist.
IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE name = 'CK_TourSchedules_Status'
      AND parent_object_id = OBJECT_ID('dbo.TourSchedules')
)
BEGIN
    ALTER TABLE dbo.TourSchedules
    ADD CONSTRAINT CK_TourSchedules_Status
        CHECK (Status IN ('OPEN','BOOKING_CLOSED','SOLD_OUT','IN_PROGRESS','COMPLETED','CANCELLED','PENDING_GUIDE','CANCELLED_BY_OPERATOR'));
END
GO

-- ============================================================
-- MODULE 1: Tour Schedule Operational Readiness & Alerting Workflow
-- Add PENDING_GUIDE and CANCELLED_BY_OPERATOR to the status CHECK constraint
-- ============================================================

-- Drop the old CHECK constraint (if it exists with the old 6-value list)
IF EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE name = 'CK_TourSchedules_Status'
      AND parent_object_id = OBJECT_ID('dbo.TourSchedules')
)
BEGIN
    ALTER TABLE dbo.TourSchedules DROP CONSTRAINT CK_TourSchedules_Status;
END
GO

-- Recreate with all 8 values including the two new operational states
ALTER TABLE dbo.TourSchedules
ADD CONSTRAINT CK_TourSchedules_Status
    CHECK (Status IN (
        'OPEN',
        'BOOKING_CLOSED',
        'SOLD_OUT',
        'PENDING_GUIDE',
        'IN_PROGRESS',
        'COMPLETED',
        'CANCELLED',
        'CANCELLED_BY_OPERATOR'
    ));
GO

-- ============================================================
-- OperationalAlerts Table
-- Prevents duplicate alert notifications per schedule per window.
-- The unique index on (ScheduleID, AlertWindow) is the idempotency key.
-- ============================================================

IF OBJECT_ID(N'dbo.OperationalAlerts', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.OperationalAlerts (
        id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        ScheduleID    BIGINT NOT NULL,
        AlertWindow   NVARCHAR(10) NOT NULL,  -- '24H', '12H', '6H', '2H'
        CreatedAt     DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_OperationalAlerts_Schedules
            FOREIGN KEY (ScheduleID) REFERENCES dbo.TourSchedules(ScheduleID)
    );
END
GO

-- Unique index: each (schedule + window) pair can only ever have one row.
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UQ_OperationalAlerts_Schedule_Window'
      AND object_id = OBJECT_ID('dbo.OperationalAlerts')
)
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX UQ_OperationalAlerts_Schedule_Window
    ON dbo.OperationalAlerts (ScheduleID, AlertWindow);
END
GO

ALTER TABLE dbo.TourSchedules
ADD CONSTRAINT CK_TourSchedules_Status
CHECK (
    Status IN (
        'OPEN',
        'BOOKING_CLOSED',
        'SOLD_OUT',
        'PENDING_GUIDE',
        'IN_PROGRESS',
        'COMPLETED',
        'CANCELLED',
        'CANCELLED_BY_OPERATOR'
    )
);
GO