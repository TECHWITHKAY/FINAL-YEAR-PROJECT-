package com.ghana.commoditymonitor.repository;

import com.ghana.commoditymonitor.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for City entity.
 */
@Repository
public interface CityRepository extends JpaRepository<City, Long> {

    Optional<City> findByName(String name);

    List<City> findByRegion(String region);

    boolean existsByName(String name);
}
