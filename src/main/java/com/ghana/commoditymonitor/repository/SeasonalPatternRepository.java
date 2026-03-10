package com.ghana.commoditymonitor.repository;

import com.ghana.commoditymonitor.entity.SeasonalPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeasonalPatternRepository extends JpaRepository<SeasonalPattern, Long> {

    List<SeasonalPattern> findByCommodityIdOrderByMonthOfYear(Long commodityId);

    List<SeasonalPattern> findByCommodityId(Long commodityId);

    void deleteAllByCommodityId(Long commodityId);
}
