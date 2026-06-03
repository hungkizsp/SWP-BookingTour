-- Idempotent migration for Guest UC01–UC11 enhancements
-- Target: SQL Server (TourBookingDB)

-- 1) Tours.TransportType
IF COL_LENGTH('Tours', 'TransportType') IS NULL
BEGIN
    ALTER TABLE Tours ADD TransportType NVARCHAR(50) NULL;
END
GO

-- 2) Cities table (for distance sorting/filtering)
IF OBJECT_ID('dbo.Cities', 'U') IS NULL
BEGIN
    CREATE TABLE Cities (
        CityID BIGINT IDENTITY(1,1) PRIMARY KEY,
        CityName NVARCHAR(100) NOT NULL UNIQUE,
        CenterLatitude DECIMAL(9,6) NOT NULL,
        CenterLongitude DECIMAL(9,6) NOT NULL,
        CreatedAt DATETIME DEFAULT GETDATE(),
        UpdatedAt DATETIME DEFAULT GETDATE()
    );
END
GO

-- 3) Helpful indexes (optional, safe if already exist)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_tours_transport_type' AND object_id = OBJECT_ID('dbo.Tours'))
BEGIN
    CREATE INDEX idx_tours_transport_type ON Tours(TransportType);
END
GO

-- 4) Booking stats view for popularity sorting (optional)
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

