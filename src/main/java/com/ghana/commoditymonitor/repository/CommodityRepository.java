package com.ghana.commoditymonitor.repository;

import com.ghana.commoditymonitor.entity.Commodity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Commodity entity.
 */
@Repository
public interface CommodityRepository extends JpaRepository<Commodity, Long> {

    List<Commodity> findByCategory(String category);

    Optional<Commodity> findByName(String name);

    boolean existsByName(String name);
}
