package com.ghana.commoditymonitor.service;

import com.ghana.commoditymonitor.dto.request.PriceRecordRequestDto;
import com.ghana.commoditymonitor.dto.response.PriceRecordResponseDto;
import com.ghana.commoditymonitor.entity.Commodity;
import com.ghana.commoditymonitor.entity.Market;
import com.ghana.commoditymonitor.entity.PriceRecord;
import com.ghana.commoditymonitor.exception.ResourceNotFoundException;
import com.ghana.commoditymonitor.repository.CommodityRepository;
import com.ghana.commoditymonitor.repository.MarketRepository;
import com.ghana.commoditymonitor.repository.PriceRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for PriceRecord-related business logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriceRecordService {

    private final PriceRecordRepository priceRecordRepository;
    private final CommodityRepository commodityRepository;
    private final MarketRepository marketRepository;

    @Transactional
    public PriceRecordResponseDto createPriceRecord(PriceRecordRequestDto request) {
        log.info("Creating new price record for commodity: {} in market: {}", request.commodityId(), request.marketId());

        Commodity commodity = commodityRepository.findById(request.commodityId())
                .orElseThrow(() -> new ResourceNotFoundException("Commodity", "id", request.commodityId()));

        Market market = marketRepository.findById(request.marketId())
                .orElseThrow(() -> new ResourceNotFoundException("Market", "id", request.marketId()));

        PriceRecord priceRecord = PriceRecord.builder()
                .commodity(commodity)
                .market(market)
                .price(request.price())
                .recordedDate(request.recordedDate())
                .source(request.source())
                .build();

        PriceRecord savedRecord = priceRecordRepository.save(priceRecord);
        return mapToResponse(savedRecord);
    }

    public PriceRecordResponseDto getPriceRecordById(Long id) {
        log.debug("Fetching price record with id: {}", id);
        return priceRecordRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("PriceRecord", "id", id));
    }

    public Page<PriceRecordResponseDto> getAllPriceRecords(Pageable pageable) {
        log.debug("Fetching all price records with pagination");
        return priceRecordRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    public List<PriceRecordResponseDto> getPriceRecordsByCommodity(Long commodityId) {
        log.debug("Fetching price records for commodity id: {}", commodityId);
        return priceRecordRepository.findByCommodityId(commodityId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<PriceRecordResponseDto> getPriceRecordsByMarket(Long marketId) {
        log.debug("Fetching price records for market id: {}", marketId);
        return priceRecordRepository.findByMarketId(marketId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PriceRecordResponseDto updatePriceRecord(Long id, PriceRecordRequestDto request) {
        log.info("Updating price record with id: {}", id);
        PriceRecord priceRecord = priceRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PriceRecord", "id", id));

        Commodity commodity = commodityRepository.findById(request.commodityId())
                .orElseThrow(() -> new ResourceNotFoundException("Commodity", "id", request.commodityId()));

        Market market = marketRepository.findById(request.marketId())
                .orElseThrow(() -> new ResourceNotFoundException("Market", "id", request.marketId()));

        priceRecord.setCommodity(commodity);
        priceRecord.setMarket(market);
        priceRecord.setPrice(request.price());
        priceRecord.setRecordedDate(request.recordedDate());
        priceRecord.setSource(request.source());

        PriceRecord updatedRecord = priceRecordRepository.save(priceRecord);
        return mapToResponse(updatedRecord);
    }

    @Transactional
    public void deletePriceRecord(Long id) {
        log.info("Deleting price record with id: {}", id);
        if (!priceRecordRepository.existsById(id)) {
            throw new ResourceNotFoundException("PriceRecord", "id", id);
        }
        priceRecordRepository.deleteById(id);
    }

    private PriceRecordResponseDto mapToResponse(PriceRecord record) {
        return PriceRecordResponseDto.builder()
                .id(record.getId())
                .commodityId(record.getCommodity().getId())
                .commodityName(record.getCommodity().getName())
                .marketId(record.getMarket().getId())
                .marketName(record.getMarket().getName())
                .cityName(record.getMarket().getCity().getName())
                .price(record.getPrice())
                .recordedDate(record.getRecordedDate())
                .source(record.getSource())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
