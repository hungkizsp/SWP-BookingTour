package com.tourbooking.booking.backend.repository;

import com.tourbooking.booking.backend.model.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {
    Optional<City> findByCityNameIgnoreCase(String cityName);

    List<City> findAllByOrderByCityName();
}
