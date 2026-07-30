package com.tourbooking.booking.backend.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tourbooking.booking.backend.model.entity.Tour;

@Repository
public interface TourRepository extends JpaRepository<Tour, Long> {

    Page<Tour> findByTourNameContaining(String tourName, Pageable pageable);

    List<Tour> findByTourNameContainingIgnoreCase(String keyword);

    List<Tour> findByCategoryId(Long categoryId);

    @Query("SELECT DISTINCT t FROM Tour t LEFT JOIN t.schedules ts LEFT JOIN t.city tc WHERE " +
            "(:keyword IS NULL OR LOWER(t.tourName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(t.startLocation) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(t.endLocation) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(tc.cityName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:minPrice IS NULL OR t.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR t.price <= :maxPrice) AND " +
            "(:minRating IS NULL OR t.rating >= :minRating) AND " +
            "(:startDate IS NULL OR ts.startDate >= :startDate)")
    List<Tour> searchToursWithFilters(@Param("keyword") String keyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minRating") Double minRating,
            @Param("startDate") LocalDate startDate);

    @Query("SELECT DISTINCT t FROM Tour t LEFT JOIN t.city tc WHERE " +
            "(:keyword IS NULL OR LOWER(t.tourName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keywordPattern, '%')) " +
            "OR LOWER(t.startLocation) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(tc.cityName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:minPrice IS NULL OR t.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR t.price <= :maxPrice) AND " +
            "(:minRating IS NULL OR t.rating >= :minRating) AND " +
            "(:startDate IS NULL OR EXISTS (SELECT 1 FROM TourSchedule tsSub WHERE tsSub.tour = t AND tsSub.startDate >= :startDate)) AND " +
            "(:categoryId IS NULL OR t.category.id = :categoryId) AND " +
            "(:transportType IS NULL OR LOWER(t.transportType) = LOWER(:transportType)) AND " +
            "(:hideSuspended = false OR NOT (" +
            "  EXISTS (SELECT 1 FROM TourSchedule tsFut WHERE tsFut.tour = t AND tsFut.startDate >= CURRENT_DATE) AND " +
            "  NOT EXISTS (SELECT 1 FROM TourSchedule tsFut2 WHERE tsFut2.tour = t AND tsFut2.startDate >= CURRENT_DATE AND tsFut2.status <> com.tourbooking.booking.backend.model.entity.enums.TourStatus.SUSPENDED)" +
            "))")

    Page<Tour> browseTours(@Param("keyword") String keyword,
            @Param("keywordPattern") String keywordPattern,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minRating") Double minRating,
            @Param("startDate") LocalDate startDate,
            @Param("categoryId") Long categoryId,
            @Param("transportType") String transportType,
            @Param("hideSuspended") boolean hideSuspended,
            Pageable pageable);

    // Popularity sort (booking count)
    @Query(value = """
            SELECT t.*
            FROM dbo.Tours t
            LEFT JOIN dbo.Cities tc ON t.CityID = tc.CityID
            LEFT JOIN (
                SELECT s.TourID, COUNT(b.BookingID) as BookingCount
                FROM dbo.TourSchedules s
                JOIN dbo.Bookings b ON b.ScheduleID = s.ScheduleID
                GROUP BY s.TourID
            ) bc ON bc.TourID = t.TourID
            WHERE
                (:keyword IS NULL OR LOWER(t.TourName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.StartLocation) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(tc.CityName) LIKE LOWER(CONCAT('%', :keyword, '%')))
                AND (:minPrice IS NULL OR t.Price >= :minPrice)
                AND (:maxPrice IS NULL OR t.Price <= :maxPrice)
                AND (:minRating IS NULL OR t.Rating >= :minRating)
                AND (:categoryId IS NULL OR t.CategoryID = :categoryId)
                AND (:transportType IS NULL OR LOWER(t.TransportType) = LOWER(:transportType))
                AND (:startDate IS NULL OR EXISTS (
                    SELECT 1 FROM dbo.TourSchedules ts
                    WHERE ts.TourID = t.TourID AND ts.StartDate >= :startDate
                ))
                AND (:hideSuspended = false OR NOT (
                    EXISTS (SELECT 1 FROM dbo.TourSchedules ts1 WHERE ts1.TourID = t.TourID AND ts1.StartDate >= CAST(GETDATE() AS DATE))
                    AND NOT EXISTS (SELECT 1 FROM dbo.TourSchedules ts2 WHERE ts2.TourID = t.TourID AND ts2.StartDate >= CAST(GETDATE() AS DATE) AND ts2.Status <> 'SUSPENDED')
                ))
            ORDER BY ISNULL(bc.BookingCount, 0) DESC, t.TourID ASC
            OFFSET :#{#pageable.offset} ROWS FETCH NEXT :#{#pageable.pageSize} ROWS ONLY
            """, countQuery = """
            SELECT COUNT(*)
            FROM dbo.Tours t
            LEFT JOIN dbo.Cities tc ON t.CityID = tc.CityID
            WHERE
                (:keyword IS NULL OR LOWER(t.TourName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.StartLocation) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(tc.CityName) LIKE LOWER(CONCAT('%', :keyword, '%')))
                AND (:minPrice IS NULL OR t.Price >= :minPrice)
                AND (:maxPrice IS NULL OR t.Price <= :maxPrice)
                AND (:minRating IS NULL OR t.Rating >= :minRating)
                AND (:categoryId IS NULL OR t.CategoryID = :categoryId)
                AND (:transportType IS NULL OR LOWER(t.TransportType) = LOWER(:transportType))
                AND (:startDate IS NULL OR EXISTS (
                    SELECT 1 FROM dbo.TourSchedules ts
                    WHERE ts.TourID = t.TourID AND ts.StartDate >= :startDate
                ))
                AND (:hideSuspended = 0 OR NOT (
                    EXISTS (SELECT 1 FROM dbo.TourSchedules ts1 WHERE ts1.TourID = t.TourID AND ts1.StartDate >= CAST(GETDATE() AS DATE))
                    AND NOT EXISTS (SELECT 1 FROM dbo.TourSchedules ts2 WHERE ts2.TourID = t.TourID AND ts2.StartDate >= CAST(GETDATE() AS DATE) AND ts2.Status <> 'SUSPENDED')
                ))
            """, nativeQuery = true)
    Page<Tour> browseToursByPopularity(@Param("keyword") String keyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minRating") Double minRating,
            @Param("startDate") LocalDate startDate,
            @Param("categoryId") Long categoryId,
            @Param("transportType") String transportType,
            @Param("hideSuspended") int hideSuspended,
            Pageable pageable);

    // Distance sort
    @Query(value = """
            SELECT t.*
            FROM dbo.Tours t
            LEFT JOIN dbo.Cities tc ON t.CityID = tc.CityID
            WHERE
                t.Latitude IS NOT NULL AND t.Longitude IS NOT NULL
                AND (:keyword IS NULL OR LOWER(t.TourName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.StartLocation) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(tc.CityName) LIKE LOWER(CONCAT('%', :keyword, '%')))
                AND (:minPrice IS NULL OR t.Price >= :minPrice)
                AND (:maxPrice IS NULL OR t.Price <= :maxPrice)
                AND (:minRating IS NULL OR t.Rating >= :minRating)
                AND (:categoryId IS NULL OR t.CategoryID = :categoryId)
                AND (:transportType IS NULL OR LOWER(t.TransportType) = LOWER(:transportType))
                AND (:startDate IS NULL OR EXISTS (
                    SELECT 1 FROM dbo.TourSchedules ts
                    WHERE ts.TourID = t.TourID AND ts.StartDate >= :startDate
                ))
                AND (:hideSuspended = 0 OR NOT (
                    EXISTS (SELECT 1 FROM dbo.TourSchedules ts1 WHERE ts1.TourID = t.TourID AND ts1.StartDate >= CAST(GETDATE() AS DATE))
                    AND NOT EXISTS (SELECT 1 FROM dbo.TourSchedules ts2 WHERE ts2.TourID = t.TourID AND ts2.StartDate >= CAST(GETDATE() AS DATE) AND ts2.Status <> 'SUSPENDED')
                ))
            ORDER BY
                (6371.0 * ACOS(
                    COS(RADIANS(:lat)) * COS(RADIANS(CAST(t.Latitude AS FLOAT)))
                    * COS(RADIANS(CAST(t.Longitude AS FLOAT)) - RADIANS(:lng))
                    + SIN(RADIANS(:lat)) * SIN(RADIANS(CAST(t.Latitude AS FLOAT)))
                )) ASC,
                t.TourID ASC
            OFFSET :#{#pageable.offset} ROWS FETCH NEXT :#{#pageable.pageSize} ROWS ONLY
            """, countQuery = """
            SELECT COUNT(*)
            FROM dbo.Tours t
            LEFT JOIN dbo.Cities tc ON t.CityID = tc.CityID
            WHERE
                t.Latitude IS NOT NULL AND t.Longitude IS NOT NULL
                AND (:keyword IS NULL OR LOWER(t.TourName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.StartLocation) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(tc.CityName) LIKE LOWER(CONCAT('%', :keyword, '%')))
                AND (:minPrice IS NULL OR t.Price >= :minPrice)
                AND (:maxPrice IS NULL OR t.Price <= :maxPrice)
                AND (:minRating IS NULL OR t.Rating >= :minRating)
                AND (:categoryId IS NULL OR t.CategoryID = :categoryId)
                AND (:transportType IS NULL OR LOWER(t.TransportType) = LOWER(:transportType))
                AND (:startDate IS NULL OR EXISTS (
                    SELECT 1 FROM dbo.TourSchedules ts
                    WHERE ts.TourID = t.TourID AND ts.StartDate >= :startDate
                ))
                AND (:hideSuspended = 0 OR NOT (
                    EXISTS (SELECT 1 FROM dbo.TourSchedules ts1 WHERE ts1.TourID = t.TourID AND ts1.StartDate >= CAST(GETDATE() AS DATE))
                    AND NOT EXISTS (SELECT 1 FROM dbo.TourSchedules ts2 WHERE ts2.TourID = t.TourID AND ts2.StartDate >= CAST(GETDATE() AS DATE) AND ts2.Status <> 'SUSPENDED')
                ))
            """, nativeQuery = true)
    Page<Tour> browseToursByDistance(@Param("keyword") String keyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minRating") Double minRating,
            @Param("startDate") LocalDate startDate,
            @Param("categoryId") Long categoryId,
            @Param("transportType") String transportType,
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("hideSuspended") int hideSuspended,
            Pageable pageable);

    boolean existsByTourNameAndStartLocationIgnoreCase(String tourName, String startLocation);

    Optional<Tour> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    List<Tour> findBySourceOrderByTourName(String source);

    long countBySourceAndStartLocationIgnoreCase(String source, String startLocation);

    @EntityGraph(attributePaths = { "images", "category", "city" })
    @Query("SELECT t FROM Tour t WHERE t.id = :id")
    Optional<Tour> findByIdWithDetails(@Param("id") Long id);

    @EntityGraph(attributePaths = { "images", "city", "category" })
    @Query("SELECT DISTINCT t FROM Tour t")
    List<Tour> findAllWithBasicDetails();
}
