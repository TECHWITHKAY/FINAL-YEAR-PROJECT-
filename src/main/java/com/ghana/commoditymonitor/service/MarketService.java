package com.ghana.commoditymonitor.service;

import com.ghana.commoditymonitor.dto.request.MarketRequestDto;
import com.ghana.commoditymonitor.dto.response.MarketResponseDto;
import com.ghana.commoditymonitor.entity.City;
import com.ghana.commoditymonitor.entity.Market;
import com.ghana.commoditymonitor.exception.ResourceNotFoundException;
import com.ghana.commoditymonitor.repository.CityRepository;
import com.ghana.commoditymonitor.repository.MarketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for Market-related business logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketService {

    private final MarketRepository marketRepository;
    private final CityRepository cityRepository;

    @Transactional
    public MarketResponseDto createMarket(MarketRequestDto request) {
        log.info("Creating new market: {} in city: {}", request.name(), request.cityId());
        
        City city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new ResourceNotFoundException("City", "id", request.cityId()));

        Market market = Market.builder()
                .name(request.name())
                .city(city)
                .build();

        Market savedMarket = marketRepository.save(market);
        return mapToResponse(savedMarket);
    }

    public MarketResponseDto getMarketById(Long id) {
        log.debug("Fetching market with id: {}", id);
        return marketRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Market", "id", id));
    }

    public List<MarketResponseDto> getAllMarkets() {
        log.debug("Fetching all markets");
        return marketRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<MarketResponseDto> getMarketsByCity(Long cityId) {
        log.debug("Fetching markets for city id: {}", cityId);
        return marketRepository.findByCityId(cityId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MarketResponseDto updateMarket(Long id, MarketRequestDto request) {
        log.info("Updating market with id: {}", id);
        Market market = marketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Market", "id", id));

        City city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new ResourceNotFoundException("City", "id", request.cityId()));

        market.setName(request.name());
        market.setCity(city);

        Market updatedMarket = marketRepository.save(market);
        return mapToResponse(updatedMarket);
    }

    @Transactional
    public void deleteMarket(Long id) {
        log.info("Deleting market with id: {}", id);
        if (!marketRepository.existsById(id)) {
            throw new ResourceNotFoundException("Market", "id", id);
        }
        marketRepository.deleteById(id);
    }

    private MarketResponseDto mapToResponse(Market market) {
        return MarketResponseDto.builder()
                .id(market.getId())
                .name(market.getName())
                .cityId(market.getCity().getId())
                .cityName(market.getCity().getName())
                .createdAt(market.getCreatedAt())
                .build();
    }
}
