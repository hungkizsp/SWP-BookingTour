USE [master]
GO
/****** Object:  Database [TourBookingDB]    Script Date: 24/06/2026 10:43:17 SA ******/
CREATE DATABASE [TourBookingDB]
 CONTAINMENT = NONE
 ON  PRIMARY 
( NAME = N'TourBookingDB', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL16.SQLEXPRESS\MSSQL\DATA\TourBookingDB.mdf' , SIZE = 73728KB , MAXSIZE = UNLIMITED, FILEGROWTH = 65536KB )
 LOG ON 
( NAME = N'TourBookingDB_log', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL16.SQLEXPRESS\MSSQL\DATA\TourBookingDB_log.ldf' , SIZE = 8192KB , MAXSIZE = 2048GB , FILEGROWTH = 65536KB )
 WITH CATALOG_COLLATION = DATABASE_DEFAULT, LEDGER = OFF
GO
ALTER DATABASE [TourBookingDB] SET COMPATIBILITY_LEVEL = 160
GO
IF (1 = FULLTEXTSERVICEPROPERTY('IsFullTextInstalled'))
begin
EXEC [TourBookingDB].[dbo].[sp_fulltext_database] @action = 'enable'
end
GO
ALTER DATABASE [TourBookingDB] SET ANSI_NULL_DEFAULT OFF 
GO
ALTER DATABASE [TourBookingDB] SET ANSI_NULLS OFF 
GO
ALTER DATABASE [TourBookingDB] SET ANSI_PADDING OFF 
GO
ALTER DATABASE [TourBookingDB] SET ANSI_WARNINGS OFF 
GO
ALTER DATABASE [TourBookingDB] SET ARITHABORT OFF 
GO
ALTER DATABASE [TourBookingDB] SET AUTO_CLOSE ON 
GO
ALTER DATABASE [TourBookingDB] SET AUTO_SHRINK OFF 
GO
ALTER DATABASE [TourBookingDB] SET AUTO_UPDATE_STATISTICS ON 
GO
ALTER DATABASE [TourBookingDB] SET CURSOR_CLOSE_ON_COMMIT OFF 
GO
ALTER DATABASE [TourBookingDB] SET CURSOR_DEFAULT  GLOBAL 
GO
ALTER DATABASE [TourBookingDB] SET CONCAT_NULL_YIELDS_NULL OFF 
GO
ALTER DATABASE [TourBookingDB] SET NUMERIC_ROUNDABORT OFF 
GO
ALTER DATABASE [TourBookingDB] SET QUOTED_IDENTIFIER OFF 
GO
ALTER DATABASE [TourBookingDB] SET RECURSIVE_TRIGGERS OFF 
GO
ALTER DATABASE [TourBookingDB] SET  ENABLE_BROKER 
GO
ALTER DATABASE [TourBookingDB] SET AUTO_UPDATE_STATISTICS_ASYNC OFF 
GO
ALTER DATABASE [TourBookingDB] SET DATE_CORRELATION_OPTIMIZATION OFF 
GO
ALTER DATABASE [TourBookingDB] SET TRUSTWORTHY OFF 
GO
ALTER DATABASE [TourBookingDB] SET ALLOW_SNAPSHOT_ISOLATION OFF 
GO
ALTER DATABASE [TourBookingDB] SET PARAMETERIZATION SIMPLE 
GO
ALTER DATABASE [TourBookingDB] SET READ_COMMITTED_SNAPSHOT OFF 
GO
ALTER DATABASE [TourBookingDB] SET HONOR_BROKER_PRIORITY OFF 
GO
ALTER DATABASE [TourBookingDB] SET RECOVERY SIMPLE 
GO
ALTER DATABASE [TourBookingDB] SET  MULTI_USER 
GO
ALTER DATABASE [TourBookingDB] SET PAGE_VERIFY CHECKSUM  
GO
ALTER DATABASE [TourBookingDB] SET DB_CHAINING OFF 
GO
ALTER DATABASE [TourBookingDB] SET FILESTREAM( NON_TRANSACTED_ACCESS = OFF ) 
GO
ALTER DATABASE [TourBookingDB] SET TARGET_RECOVERY_TIME = 60 SECONDS 
GO
ALTER DATABASE [TourBookingDB] SET DELAYED_DURABILITY = DISABLED 
GO
ALTER DATABASE [TourBookingDB] SET ACCELERATED_DATABASE_RECOVERY = OFF  
GO
ALTER DATABASE [TourBookingDB] SET QUERY_STORE = ON
GO
ALTER DATABASE [TourBookingDB] SET QUERY_STORE (OPERATION_MODE = READ_WRITE, CLEANUP_POLICY = (STALE_QUERY_THRESHOLD_DAYS = 30), DATA_FLUSH_INTERVAL_SECONDS = 900, INTERVAL_LENGTH_MINUTES = 60, MAX_STORAGE_SIZE_MB = 1000, QUERY_CAPTURE_MODE = AUTO, SIZE_BASED_CLEANUP_MODE = AUTO, MAX_PLANS_PER_QUERY = 200, WAIT_STATS_CAPTURE_MODE = ON)
GO
USE [TourBookingDB]
GO
/****** Object:  Table [dbo].[TourSchedules]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[TourSchedules](
	[ScheduleID] [bigint] IDENTITY(1,1) NOT NULL,
	[TourID] [bigint] NOT NULL,
	[GuideID] [bigint] NULL,
	[StartDate] [date] NULL,
	[EndDate] [date] NULL,
	[AvailableSlots] [int] NULL,
	[MaxSlots] [int] NULL,
	[Status] [nvarchar](50) NULL,
	[CurrentProgress] [nvarchar](max) NULL,
	[ReportContent] [nvarchar](max) NULL,
	[ReportSubmittedAt] [datetime2](7) NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
	[BookingDeadline] [datetime2](7) NULL,
	[DepartureTime] [time](7) NULL,
	[ReturnTime] [time](7) NULL,
PRIMARY KEY CLUSTERED 
(
	[ScheduleID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Bookings]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Bookings](
	[BookingID] [bigint] IDENTITY(1,1) NOT NULL,
	[UserID] [bigint] NOT NULL,
	[ScheduleID] [bigint] NOT NULL,
	[BookingDate] [datetime2](7) NOT NULL,
	[NumberOfPeople] [int] NULL,
	[TotalPrice] [decimal](12, 2) NULL,
	[Status] [nvarchar](50) NULL,
	[DiscountAmount] [decimal](18, 2) NULL,
	[DiscountCode] [nvarchar](50) NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
	[OccupiedSlots] [int] NULL,
PRIMARY KEY CLUSTERED 
(
	[BookingID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Tours]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Tours](
	[TourID] [bigint] IDENTITY(1,1) NOT NULL,
	[TourName] [nvarchar](200) NULL,
	[Description] [nvarchar](max) NULL,
	[Itinerary] [nvarchar](max) NULL,
	[Price] [decimal](10, 2) NULL,
	[Duration] [int] NULL,
	[StartLocation] [nvarchar](100) NULL,
	[EndLocation] [nvarchar](100) NULL,
	[Latitude] [decimal](9, 6) NULL,
	[Longitude] [decimal](9, 6) NULL,
	[TransportType] [nvarchar](50) NULL,
	[ChildPolicy] [nvarchar](max) NULL,
	[SuitableAges] [nvarchar](200) NULL,
	[WhyChooseUs] [nvarchar](max) NULL,
	[BestTime] [nvarchar](200) NULL,
	[Inclusions] [nvarchar](max) NULL,
	[Exclusions] [nvarchar](max) NULL,
	[CategoryID] [bigint] NULL,
	[CityID] [bigint] NULL,
	[Rating] [float] NOT NULL,
	[Source] [nvarchar](50) NULL,
	[ExternalId] [nvarchar](100) NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[TourID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  View [dbo].[TourBookingStats]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

        CREATE VIEW [dbo].[TourBookingStats] AS
        SELECT
            t.TourID AS TourID,
            COUNT(b.BookingID) AS BookingCount
        FROM dbo.Tours t
        LEFT JOIN dbo.TourSchedules ts ON ts.TourID = t.TourID
        LEFT JOIN dbo.Bookings b ON b.ScheduleID = ts.ScheduleID
        GROUP BY t.TourID
    
GO
/****** Object:  Table [dbo].[Categories]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Categories](
	[CategoryID] [bigint] IDENTITY(1,1) NOT NULL,
	[CategoryName] [nvarchar](100) NOT NULL,
	[Description] [nvarchar](255) NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[CategoryID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[CategoryName] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[ChatEscalations]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ChatEscalations](
	[EscalationID] [bigint] IDENTITY(1,1) NOT NULL,
	[CustomerID] [bigint] NULL,
	[GuestID] [nvarchar](50) NULL,
	[RequestNote] [nvarchar](max) NULL,
	[MeetingPreference] [nvarchar](255) NULL,
	[Status] [nvarchar](20) NULL,
	[AssignedStaffID] [bigint] NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[EscalationID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[ChatMessages]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ChatMessages](
	[MessageID] [bigint] IDENTITY(1,1) NOT NULL,
	[UserID] [bigint] NULL,
	[SenderType] [nvarchar](20) NULL,
	[Message] [nvarchar](max) NULL,
	[GuestId] [nvarchar](50) NULL,
	[SentAt] [datetime2](7) NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[MessageID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[ChatSessions]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ChatSessions](
	[SessionID] [bigint] IDENTITY(1,1) NOT NULL,
	[UserID] [bigint] NULL,
	[GuestId] [nvarchar](50) NULL,
	[Status] [nvarchar](30) NULL,
	[LastMessageAt] [datetime2](7) NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
	[AssignedStaffID] [bigint] NULL,
PRIMARY KEY CLUSTERED 
(
	[SessionID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Cities]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Cities](
	[CityID] [bigint] IDENTITY(1,1) NOT NULL,
	[CityName] [nvarchar](100) NOT NULL,
	[CenterLatitude] [decimal](9, 6) NOT NULL,
	[CenterLongitude] [decimal](9, 6) NOT NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[CityID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[CityName] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[DiscountPolicies]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[DiscountPolicies](
	[PolicyID] [bigint] IDENTITY(1,1) NOT NULL,
	[PassengerType] [varchar](50) NOT NULL,
	[Rate] [decimal](5, 2) NOT NULL,
	[IsActive] [bit] NOT NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[PolicyID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[PassengerType] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Discounts]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Discounts](
	[DiscountID] [bigint] IDENTITY(1,1) NOT NULL,
	[Code] [nvarchar](50) NOT NULL,
	[DiscountType] [nvarchar](20) NOT NULL,
	[Value] [decimal](10, 2) NOT NULL,
	[StartDate] [datetime2](7) NULL,
	[EndDate] [datetime2](7) NULL,
	[UsageLimit] [int] NULL,
	[CurrentUsage] [int] NOT NULL,
	[IsActive] [bit] NOT NULL,
	[MinimumBookingAmount] [decimal](10, 2) NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[DiscountID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[Code] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Documents]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Documents](
	[DocumentID] [bigint] IDENTITY(1,1) NOT NULL,
	[UserID] [bigint] NOT NULL,
	[FileUrl] [nvarchar](500) NULL,
	[Type] [nvarchar](100) NULL,
	[UploadedAt] [datetime2](7) NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[DocumentID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Invoice]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Invoice](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[booking_id] [bigint] NULL,
	[invoiceNumber] [nvarchar](255) NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[LoyaltyPoint]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[LoyaltyPoint](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[user_UserID] [bigint] NOT NULL,
	[points] [int] NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[user_UserID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Newsletters]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Newsletters](
	[SubscriberID] [bigint] IDENTITY(1,1) NOT NULL,
	[Email] [nvarchar](100) NOT NULL,
	[SubscribedAt] [datetime2](7) NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[SubscriberID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[Email] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[OperationalAlerts]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[OperationalAlerts](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[ScheduleID] [bigint] NOT NULL,
	[AlertWindow] [nvarchar](10) NOT NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Passengers]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Passengers](
	[PassengerID] [bigint] IDENTITY(1,1) NOT NULL,
	[BookingID] [bigint] NOT NULL,
	[FullName] [nvarchar](200) NOT NULL,
	[DateOfBirth] [date] NULL,
	[IdNumber] [varchar](50) NULL,
	[PassengerType] [varchar](20) NOT NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[PassengerID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[PaymentLogs]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[PaymentLogs](
	[LogID] [bigint] IDENTITY(1,1) NOT NULL,
	[PaymentID] [bigint] NOT NULL,
	[LogMessage] [nvarchar](max) NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[LogID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Payments]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Payments](
	[PaymentID] [bigint] IDENTITY(1,1) NOT NULL,
	[BookingID] [bigint] NOT NULL,
	[Amount] [decimal](10, 2) NULL,
	[PaymentMethod] [nvarchar](50) NULL,
	[TransactionCode] [nvarchar](100) NULL,
	[PaymentDate] [datetime2](7) NULL,
	[Status] [nvarchar](50) NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[PaymentID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
 CONSTRAINT [UQ_Payments_BookingID] UNIQUE NONCLUSTERED 
(
	[BookingID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[PrivacyPolicies]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[PrivacyPolicies](
	[PolicyID] [bigint] IDENTITY(1,1) NOT NULL,
	[Title] [nvarchar](255) NOT NULL,
	[Content] [nvarchar](max) NOT NULL,
	[IsActive] [bit] NOT NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[PolicyID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[RefundRequests]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[RefundRequests](
	[RefundID] [bigint] IDENTITY(1,1) NOT NULL,
	[BookingID] [bigint] NOT NULL,
	[Amount] [decimal](10, 2) NULL,
	[Reason] [nvarchar](max) NULL,
	[Status] [nvarchar](50) NULL,
	[StaffNote] [nvarchar](max) NULL,
	[ProcessedAt] [datetime2](7) NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
	[OriginalBookingStatus] [nvarchar](50) NULL,
PRIMARY KEY CLUSTERED 
(
	[RefundID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Reviews]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Reviews](
	[ReviewID] [bigint] IDENTITY(1,1) NOT NULL,
	[UserID] [bigint] NOT NULL,
	[Rating] [int] NULL,
	[Comment] [nvarchar](max) NULL,
	[ReviewDate] [datetime2](7) NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
	[BookingID] [bigint] NULL,
PRIMARY KEY CLUSTERED 
(
	[ReviewID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Tokens]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Tokens](
	[id] [bigint] IDENTITY(1,1) NOT NULL,
	[token] [nvarchar](500) NOT NULL,
	[email] [nvarchar](150) NULL,
	[expiryDate] [datetime2](7) NULL,
	[used] [bit] NOT NULL,
	[type] [nvarchar](50) NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[TourActivityImages]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[TourActivityImages](
	[ActivityImageID] [bigint] IDENTITY(1,1) NOT NULL,
	[ScheduleID] [bigint] NOT NULL,
	[ImageURL] [nvarchar](500) NULL,
	[Caption] [nvarchar](255) NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[ActivityImageID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[TourFaqs]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[TourFaqs](
	[FaqID] [bigint] IDENTITY(1,1) NOT NULL,
	[TourID] [bigint] NULL,
	[Question] [nvarchar](500) NOT NULL,
	[Answer] [nvarchar](max) NOT NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[FaqID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[TourHighlights]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[TourHighlights](
	[HighlightID] [bigint] IDENTITY(1,1) NOT NULL,
	[TourID] [bigint] NULL,
	[Highlight] [nvarchar](255) NOT NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[HighlightID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[TourImages]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[TourImages](
	[ImageID] [bigint] IDENTITY(1,1) NOT NULL,
	[TourID] [bigint] NULL,
	[ImageURL] [nvarchar](500) NOT NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[ImageID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[TourProgressLogs]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[TourProgressLogs](
	[LogID] [bigint] IDENTITY(1,1) NOT NULL,
	[ScheduleID] [bigint] NOT NULL,
	[Content] [nvarchar](max) NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[LogID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Users]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Users](
	[UserID] [bigint] IDENTITY(1,1) NOT NULL,
	[FullName] [nvarchar](100) NULL,
	[Email] [nvarchar](100) NOT NULL,
	[PasswordHash] [nvarchar](255) NULL,
	[Role] [nvarchar](20) NULL,
	[AvatarURL] [nvarchar](255) NULL,
	[PhoneNumber] [nvarchar](20) NULL,
	[Address] [nvarchar](255) NULL,
	[IsActive] [bit] NOT NULL,
	[CurrentSessionID] [nvarchar](64) NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
	[Bio] [nvarchar](max) NULL,
	[DateOfBirth] [date] NULL,
	[ExperienceYears] [int] NULL,
	[Gender] [nvarchar](20) NULL,
PRIMARY KEY CLUSTERED 
(
	[UserID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
UNIQUE NONCLUSTERED 
(
	[Email] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Wishlist]    Script Date: 24/06/2026 10:43:17 SA ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Wishlist](
	[WishlistID] [bigint] IDENTITY(1,1) NOT NULL,
	[UserID] [bigint] NOT NULL,
	[TourID] [bigint] NOT NULL,
	[CreatedAt] [datetime2](7) NOT NULL,
	[UpdatedAt] [datetime2](7) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[WishlistID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
 CONSTRAINT [UQ_Wishlist_User_Tour] UNIQUE NONCLUSTERED 
(
	[UserID] ASC,
	[TourID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Index [UQ_Reviews_Booking]    Script Date: 24/06/2026 10:43:17 SA ******/
CREATE UNIQUE NONCLUSTERED INDEX [UQ_Reviews_Booking] ON [dbo].[Reviews]
(
	[BookingID] ASC
)
WHERE ([BookingID] IS NOT NULL)
WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [idx_tours_transport_type]    Script Date: 24/06/2026 10:43:17 SA ******/
CREATE NONCLUSTERED INDEX [idx_tours_transport_type] ON [dbo].[Tours]
(
	[TransportType] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
ALTER TABLE [dbo].[Bookings] ADD  DEFAULT (sysdatetime()) FOR [BookingDate]
GO
ALTER TABLE [dbo].[Bookings] ADD  DEFAULT ('PENDING') FOR [Status]
GO
ALTER TABLE [dbo].[Bookings] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[Bookings] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[Categories] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[Categories] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[ChatEscalations] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[ChatEscalations] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[ChatMessages] ADD  DEFAULT (sysdatetime()) FOR [SentAt]
GO
ALTER TABLE [dbo].[ChatMessages] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[ChatMessages] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[ChatSessions] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[ChatSessions] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[Cities] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[Cities] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[DiscountPolicies] ADD  DEFAULT ((1)) FOR [IsActive]
GO
ALTER TABLE [dbo].[DiscountPolicies] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[DiscountPolicies] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[Discounts] ADD  DEFAULT ((0)) FOR [CurrentUsage]
GO
ALTER TABLE [dbo].[Discounts] ADD  DEFAULT ((1)) FOR [IsActive]
GO
ALTER TABLE [dbo].[Discounts] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[Discounts] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[Documents] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[Documents] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[Invoice] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[Invoice] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[LoyaltyPoint] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[LoyaltyPoint] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[Newsletters] ADD  DEFAULT (sysdatetime()) FOR [SubscribedAt]
GO
ALTER TABLE [dbo].[Newsletters] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[Newsletters] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[OperationalAlerts] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[Passengers] ADD  DEFAULT (getdate()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[Passengers] ADD  DEFAULT (getdate()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[PaymentLogs] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[PaymentLogs] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[Payments] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[Payments] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[PrivacyPolicies] ADD  DEFAULT ((1)) FOR [IsActive]
GO
ALTER TABLE [dbo].[PrivacyPolicies] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[PrivacyPolicies] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[RefundRequests] ADD  DEFAULT ('PENDING') FOR [Status]
GO
ALTER TABLE [dbo].[RefundRequests] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[RefundRequests] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[Reviews] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[Reviews] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[Tokens] ADD  DEFAULT ((0)) FOR [used]
GO
ALTER TABLE [dbo].[Tokens] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[Tokens] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[TourActivityImages] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[TourActivityImages] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[TourFaqs] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[TourFaqs] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[TourHighlights] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[TourHighlights] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[TourImages] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[TourImages] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[TourProgressLogs] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[TourProgressLogs] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[Tours] ADD  DEFAULT ((0.0)) FOR [Rating]
GO
ALTER TABLE [dbo].[Tours] ADD  DEFAULT ('LOCAL') FOR [Source]
GO
ALTER TABLE [dbo].[Tours] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[Tours] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[TourSchedules] ADD  DEFAULT ('OPEN') FOR [Status]
GO
ALTER TABLE [dbo].[TourSchedules] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[TourSchedules] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[Users] ADD  DEFAULT ('CUSTOMER') FOR [Role]
GO
ALTER TABLE [dbo].[Users] ADD  DEFAULT ((1)) FOR [IsActive]
GO
ALTER TABLE [dbo].[Users] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[Users] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[Wishlist] ADD  DEFAULT (sysdatetime()) FOR [CreatedAt]
GO
ALTER TABLE [dbo].[Wishlist] ADD  DEFAULT (sysdatetime()) FOR [UpdatedAt]
GO
ALTER TABLE [dbo].[Bookings]  WITH CHECK ADD  CONSTRAINT [FK_Bookings_Schedules] FOREIGN KEY([ScheduleID])
REFERENCES [dbo].[TourSchedules] ([ScheduleID])
GO
ALTER TABLE [dbo].[Bookings] CHECK CONSTRAINT [FK_Bookings_Schedules]
GO
ALTER TABLE [dbo].[Bookings]  WITH CHECK ADD  CONSTRAINT [FK_Bookings_Users] FOREIGN KEY([UserID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[Bookings] CHECK CONSTRAINT [FK_Bookings_Users]
GO
ALTER TABLE [dbo].[ChatEscalations]  WITH CHECK ADD  CONSTRAINT [FK_ChatEscalations_Customers] FOREIGN KEY([CustomerID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[ChatEscalations] CHECK CONSTRAINT [FK_ChatEscalations_Customers]
GO
ALTER TABLE [dbo].[ChatEscalations]  WITH CHECK ADD  CONSTRAINT [FK_ChatEscalations_Staff] FOREIGN KEY([AssignedStaffID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[ChatEscalations] CHECK CONSTRAINT [FK_ChatEscalations_Staff]
GO
ALTER TABLE [dbo].[ChatMessages]  WITH CHECK ADD  CONSTRAINT [FK_ChatMessages_Users] FOREIGN KEY([UserID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[ChatMessages] CHECK CONSTRAINT [FK_ChatMessages_Users]
GO
ALTER TABLE [dbo].[ChatSessions]  WITH CHECK ADD  CONSTRAINT [FK_ChatSessions_Staff] FOREIGN KEY([AssignedStaffID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[ChatSessions] CHECK CONSTRAINT [FK_ChatSessions_Staff]
GO
ALTER TABLE [dbo].[ChatSessions]  WITH CHECK ADD  CONSTRAINT [FK_ChatSessions_Users] FOREIGN KEY([UserID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[ChatSessions] CHECK CONSTRAINT [FK_ChatSessions_Users]
GO
ALTER TABLE [dbo].[Documents]  WITH CHECK ADD  CONSTRAINT [FK_Documents_Users] FOREIGN KEY([UserID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[Documents] CHECK CONSTRAINT [FK_Documents_Users]
GO
ALTER TABLE [dbo].[Invoice]  WITH CHECK ADD  CONSTRAINT [FK_Invoice_Bookings] FOREIGN KEY([booking_id])
REFERENCES [dbo].[Bookings] ([BookingID])
GO
ALTER TABLE [dbo].[Invoice] CHECK CONSTRAINT [FK_Invoice_Bookings]
GO
ALTER TABLE [dbo].[LoyaltyPoint]  WITH CHECK ADD  CONSTRAINT [FK_LoyaltyPoint_Users] FOREIGN KEY([user_UserID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[LoyaltyPoint] CHECK CONSTRAINT [FK_LoyaltyPoint_Users]
GO
ALTER TABLE [dbo].[OperationalAlerts]  WITH CHECK ADD  CONSTRAINT [FK_OperationalAlerts_Schedules] FOREIGN KEY([ScheduleID])
REFERENCES [dbo].[TourSchedules] ([ScheduleID])
GO
ALTER TABLE [dbo].[OperationalAlerts] CHECK CONSTRAINT [FK_OperationalAlerts_Schedules]
GO
ALTER TABLE [dbo].[Passengers]  WITH CHECK ADD  CONSTRAINT [FK_Passengers_Bookings] FOREIGN KEY([BookingID])
REFERENCES [dbo].[Bookings] ([BookingID])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[Passengers] CHECK CONSTRAINT [FK_Passengers_Bookings]
GO
ALTER TABLE [dbo].[PaymentLogs]  WITH CHECK ADD  CONSTRAINT [FK_PaymentLogs_Payments] FOREIGN KEY([PaymentID])
REFERENCES [dbo].[Payments] ([PaymentID])
GO
ALTER TABLE [dbo].[PaymentLogs] CHECK CONSTRAINT [FK_PaymentLogs_Payments]
GO
ALTER TABLE [dbo].[Payments]  WITH CHECK ADD  CONSTRAINT [FK_Payments_Bookings] FOREIGN KEY([BookingID])
REFERENCES [dbo].[Bookings] ([BookingID])
GO
ALTER TABLE [dbo].[Payments] CHECK CONSTRAINT [FK_Payments_Bookings]
GO
ALTER TABLE [dbo].[RefundRequests]  WITH CHECK ADD  CONSTRAINT [FK_RefundRequests_Bookings] FOREIGN KEY([BookingID])
REFERENCES [dbo].[Bookings] ([BookingID])
GO
ALTER TABLE [dbo].[RefundRequests] CHECK CONSTRAINT [FK_RefundRequests_Bookings]
GO
ALTER TABLE [dbo].[Reviews]  WITH CHECK ADD  CONSTRAINT [FK_Reviews_Bookings] FOREIGN KEY([BookingID])
REFERENCES [dbo].[Bookings] ([BookingID])
GO
ALTER TABLE [dbo].[Reviews] CHECK CONSTRAINT [FK_Reviews_Bookings]
GO
ALTER TABLE [dbo].[Reviews]  WITH CHECK ADD  CONSTRAINT [FK_Reviews_Users] FOREIGN KEY([UserID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[Reviews] CHECK CONSTRAINT [FK_Reviews_Users]
GO
ALTER TABLE [dbo].[TourActivityImages]  WITH CHECK ADD  CONSTRAINT [FK_TourActivityImages_Schedules] FOREIGN KEY([ScheduleID])
REFERENCES [dbo].[TourSchedules] ([ScheduleID])
GO
ALTER TABLE [dbo].[TourActivityImages] CHECK CONSTRAINT [FK_TourActivityImages_Schedules]
GO
ALTER TABLE [dbo].[TourFaqs]  WITH CHECK ADD  CONSTRAINT [FK_TourFaqs_Tours] FOREIGN KEY([TourID])
REFERENCES [dbo].[Tours] ([TourID])
GO
ALTER TABLE [dbo].[TourFaqs] CHECK CONSTRAINT [FK_TourFaqs_Tours]
GO
ALTER TABLE [dbo].[TourHighlights]  WITH CHECK ADD  CONSTRAINT [FK_TourHighlights_Tours] FOREIGN KEY([TourID])
REFERENCES [dbo].[Tours] ([TourID])
GO
ALTER TABLE [dbo].[TourHighlights] CHECK CONSTRAINT [FK_TourHighlights_Tours]
GO
ALTER TABLE [dbo].[TourImages]  WITH CHECK ADD  CONSTRAINT [FK_TourImages_Tours] FOREIGN KEY([TourID])
REFERENCES [dbo].[Tours] ([TourID])
GO
ALTER TABLE [dbo].[TourImages] CHECK CONSTRAINT [FK_TourImages_Tours]
GO
ALTER TABLE [dbo].[TourProgressLogs]  WITH CHECK ADD  CONSTRAINT [FK_TourProgressLogs_Schedules] FOREIGN KEY([ScheduleID])
REFERENCES [dbo].[TourSchedules] ([ScheduleID])
GO
ALTER TABLE [dbo].[TourProgressLogs] CHECK CONSTRAINT [FK_TourProgressLogs_Schedules]
GO
ALTER TABLE [dbo].[Tours]  WITH CHECK ADD  CONSTRAINT [FK_Tours_Categories] FOREIGN KEY([CategoryID])
REFERENCES [dbo].[Categories] ([CategoryID])
GO
ALTER TABLE [dbo].[Tours] CHECK CONSTRAINT [FK_Tours_Categories]
GO
ALTER TABLE [dbo].[Tours]  WITH CHECK ADD  CONSTRAINT [FK_Tours_Cities] FOREIGN KEY([CityID])
REFERENCES [dbo].[Cities] ([CityID])
GO
ALTER TABLE [dbo].[Tours] CHECK CONSTRAINT [FK_Tours_Cities]
GO
ALTER TABLE [dbo].[TourSchedules]  WITH CHECK ADD  CONSTRAINT [FK_TourSchedules_Guide] FOREIGN KEY([GuideID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[TourSchedules] CHECK CONSTRAINT [FK_TourSchedules_Guide]
GO
ALTER TABLE [dbo].[TourSchedules]  WITH CHECK ADD  CONSTRAINT [FK_TourSchedules_Tours] FOREIGN KEY([TourID])
REFERENCES [dbo].[Tours] ([TourID])
GO
ALTER TABLE [dbo].[TourSchedules] CHECK CONSTRAINT [FK_TourSchedules_Tours]
GO
ALTER TABLE [dbo].[Wishlist]  WITH CHECK ADD  CONSTRAINT [FK_Wishlist_Tours] FOREIGN KEY([TourID])
REFERENCES [dbo].[Tours] ([TourID])
GO
ALTER TABLE [dbo].[Wishlist] CHECK CONSTRAINT [FK_Wishlist_Tours]
GO
ALTER TABLE [dbo].[Wishlist]  WITH CHECK ADD  CONSTRAINT [FK_Wishlist_Users] FOREIGN KEY([UserID])
REFERENCES [dbo].[Users] ([UserID])
GO
ALTER TABLE [dbo].[Wishlist] CHECK CONSTRAINT [FK_Wishlist_Users]
GO
ALTER TABLE [dbo].[Passengers]  WITH CHECK ADD  CONSTRAINT [CK_Passengers_PassengerType] CHECK  (([PassengerType]='INFANT' OR [PassengerType]='CHILD' OR [PassengerType]='ADULT'))
GO
ALTER TABLE [dbo].[Passengers] CHECK CONSTRAINT [CK_Passengers_PassengerType]
GO
ALTER TABLE [dbo].[TourSchedules]  WITH CHECK ADD  CONSTRAINT [CK_TourSchedules_Status] CHECK  (([Status]='CANCELLED_BY_OPERATOR' OR [Status]='CANCELLED' OR [Status]='COMPLETED' OR [Status]='IN_PROGRESS' OR [Status]='PENDING_GUIDE' OR [Status]='SOLD_OUT' OR [Status]='BOOKING_CLOSED' OR [Status]='OPEN'))
GO
ALTER TABLE [dbo].[TourSchedules] CHECK CONSTRAINT [CK_TourSchedules_Status]
GO
USE [master]
GO
ALTER DATABASE [TourBookingDB] SET  READ_WRITE 
GO
ALTER TABLE Bookings ADD CancellationReason NVARCHAR(500) NULL;
GO

-- ====== TOUR CHAT GROUP ======
CREATE TABLE [dbo].[TourChatGroups] (
    [Id] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [ScheduleID] BIGINT NOT NULL,
    [IsActive] BIT NOT NULL DEFAULT 1,
    [CreatedAt] DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT [FK_TourChatGroups_TourSchedules] FOREIGN KEY ([ScheduleID]) REFERENCES [dbo].[TourSchedules]([ScheduleID])
);
GO

CREATE TABLE [dbo].[TourChatGroupMembers] (
    [Id] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [GroupID] BIGINT NOT NULL,
    [UserID] BIGINT NOT NULL,
    [JoinedAt] DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT [FK_TCGM_Group] FOREIGN KEY ([GroupID]) REFERENCES [dbo].[TourChatGroups]([Id]),
    CONSTRAINT [FK_TCGM_User] FOREIGN KEY ([UserID]) REFERENCES [dbo].[Users]([UserID]),
    CONSTRAINT [UQ_TCGM_GroupUser] UNIQUE ([GroupID], [UserID])
);
GO

CREATE TABLE [dbo].[TourChatGroupMessages] (
    [Id] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [GroupID] BIGINT NOT NULL,
    [UserID] BIGINT NOT NULL,
    [Content] NVARCHAR(MAX) NOT NULL,
    [SentAt] DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT [FK_TCGMsg_Group] FOREIGN KEY ([GroupID]) REFERENCES [dbo].[TourChatGroups]([Id]),
    CONSTRAINT [FK_TCGMsg_User] FOREIGN KEY ([UserID]) REFERENCES [dbo].[Users]([UserID])
);
GO

CREATE INDEX [idx_tcg_schedule] ON [dbo].[TourChatGroups]([ScheduleID]);
CREATE INDEX [idx_tcgmember_user] ON [dbo].[TourChatGroupMembers]([UserID]);
CREATE INDEX [idx_tcgm_group_sent] ON [dbo].[TourChatGroupMessages]([GroupID], [SentAt] DESC);
GO

-- ====== VOUCHERS ======
IF NOT EXISTS (SELECT * FROM sys.columns WHERE Name = N'MaxDiscountAmount' AND Object_ID = Object_ID(N'dbo.Discounts'))
BEGIN
    ALTER TABLE [dbo].[Discounts] ADD [MaxDiscountAmount] DECIMAL(18,2);
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE Name = N'ApplicableTourID' AND Object_ID = Object_ID(N'dbo.Discounts'))
BEGIN
    ALTER TABLE [dbo].[Discounts] ADD [ApplicableTourID] BIGINT;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE Name = N'DiscountID' AND Object_ID = Object_ID(N'dbo.Bookings'))
BEGIN
    ALTER TABLE [dbo].[Bookings] ADD [DiscountID] BIGINT REFERENCES [dbo].[Discounts]([DiscountID]);
END
GO

-- ====== LOYALTY POINTS ======
IF NOT EXISTS (SELECT * FROM sys.columns WHERE Name = N'LoyaltyPointsUsed' AND Object_ID = Object_ID(N'dbo.Bookings'))
BEGIN
    ALTER TABLE [dbo].[Bookings] ADD [LoyaltyPointsUsed] INT DEFAULT 0;
END
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE Name = N'LoyaltyDiscountAmount' AND Object_ID = Object_ID(N'dbo.Bookings'))
BEGIN
    ALTER TABLE [dbo].[Bookings] ADD [LoyaltyDiscountAmount] DECIMAL(10,2) DEFAULT 0;
END
GO

CREATE TABLE [dbo].[loyalty_transaction] (
    [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [user_id] BIGINT NOT NULL,
    [points] INT NOT NULL,
    [transaction_type] NVARCHAR(10) NOT NULL,
    [booking_id] BIGINT NULL,
    [description] NVARCHAR(255) NULL,
    [created_at] DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT [FK_LoyaltyTx_User] FOREIGN KEY ([user_id]) REFERENCES [dbo].[Users]([UserID]),
    CONSTRAINT [FK_LoyaltyTx_Booking] FOREIGN KEY ([booking_id]) REFERENCES [dbo].[Bookings]([BookingID])
);
GO

-- ====================================================================
-- Feature 3: Tour Itinerary Day
-- ====================================================================
CREATE TABLE [dbo].[tour_itinerary_day] (
    [id] BIGINT IDENTITY(1,1) PRIMARY KEY,
    [tour_id] BIGINT NOT NULL,
    [day_number] INT NOT NULL,
    [title] NVARCHAR(255) NOT NULL,
    [description] NVARCHAR(MAX) NULL,
    [accommodation] NVARCHAR(255) NULL,
    [meals] NVARCHAR(100) NULL,
    [transportation] NVARCHAR(100) NULL,
    [highlights] NVARCHAR(MAX) NULL,
    [image_url] NVARCHAR(500) NULL,
    [created_at] DATETIME2 DEFAULT GETDATE(),
    [updated_at] DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT [FK_TourItinerary_Tour] FOREIGN KEY ([tour_id]) REFERENCES [dbo].[Tours]([TourID]),
    CONSTRAINT [UQ_TourItinerary_TourDay] UNIQUE ([tour_id], [day_number])
);
GO