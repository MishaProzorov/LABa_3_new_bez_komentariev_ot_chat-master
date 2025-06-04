package com.example.SunriseSunset.repository;

import com.example.SunriseSunset.model.SunriseSunsetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SunriseSunsetRepository extends JpaRepository<SunriseSunsetEntity, Integer> {

    @Query("SELECT s FROM SunriseSunsetEntity s JOIN s.locations l WHERE l.id = :locationId")
    List<SunriseSunsetEntity> findByLocationId(@Param("locationId") Integer locationId);

    @Query("SELECT s FROM SunriseSunsetEntity s JOIN s.locations l WHERE s.date = :date AND l.name = :locationName")
    List<SunriseSunsetEntity> findByDateAndLocationName(@Param("date") LocalDate date, @Param("locationName") String locationName);
}