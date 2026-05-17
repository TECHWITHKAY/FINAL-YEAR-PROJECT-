package com.ghana.commoditymonitor.repository;

import com.ghana.commoditymonitor.entity.Market;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Market entity.
 */
@Repository
public interface MarketRepository extends JpaRepository<Market, Long> {

    List<Market> findByCityId(Long cityId);

    List<Market> findByCityName(String cityName);
    
    boolean existsByName(String name);
    
    Optional<Market> findByNameIgnoreCase(String name);
    
    @org.springframework.data.jpa.repository.Query("SELECT m.id FROM Market m")
    List<Long> findAllIds();
}
