package com.tourbooking.booking.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.tourbooking.booking.backend.model.entity.TourImage;

import java.util.List;

@Repository
public interface TourImageRepository extends JpaRepository<TourImage, Long> {
    @Query("SELECT ti FROM TourImage ti WHERE ti.tour.id IN :tourIds")
    List<TourImage> findByTourIdIn(@Param("tourIds") List<Long> tourIds);
}
