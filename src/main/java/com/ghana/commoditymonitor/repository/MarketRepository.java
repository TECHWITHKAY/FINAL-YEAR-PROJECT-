package com.ghana.commoditymonitor.repository;

import com.ghana.commoditymonitor.entity.Market;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Market entity.
 */
@Repository
public interface MarketRepository extends JpaRepository<Market, Long> {

    List<Market> findByCityId(Long cityId);

    List<Market> findByCityName(String cityName);
}
