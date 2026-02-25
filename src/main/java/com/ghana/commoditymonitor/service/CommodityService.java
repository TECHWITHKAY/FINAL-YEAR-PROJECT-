package com.ghana.commoditymonitor.service;

import com.ghana.commoditymonitor.dto.request.CommodityRequestDto;
import com.ghana.commoditymonitor.dto.response.CommodityResponseDto;
import com.ghana.commoditymonitor.entity.Commodity;
import com.ghana.commoditymonitor.exception.DuplicateResourceException;
import com.ghana.commoditymonitor.exception.ResourceNotFoundException;
import com.ghana.commoditymonitor.repository.CommodityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for Commodity-related business logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommodityService {

    private final CommodityRepository commodityRepository;

    @Transactional
    public CommodityResponseDto createCommodity(CommodityRequestDto request) {
        log.info("Creating new commodity: {}", request.name());
        if (commodityRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Commodity already exists with name: " + request.name());
        }

        Commodity commodity = Commodity.builder()
                .name(request.name())
                .category(request.category())
                .unit(request.unit())
                .build();

        Commodity savedCommodity = commodityRepository.save(commodity);
        return mapToResponse(savedCommodity);
    }

    public CommodityResponseDto getCommodityById(Long id) {
        log.debug("Fetching commodity with id: {}", id);
        return commodityRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Commodity", "id", id));
    }

    public List<CommodityResponseDto> getAllCommodities() {
        log.debug("Fetching all commodities");
        return commodityRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CommodityResponseDto> getCommoditiesByCategory(String category) {
        log.debug("Fetching commodities in category: {}", category);
        return commodityRepository.findByCategory(category).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommodityResponseDto updateCommodity(Long id, CommodityRequestDto request) {
        log.info("Updating commodity with id: {}", id);
        Commodity commodity = commodityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commodity", "id", id));

        if (!commodity.getName().equals(request.name()) && commodityRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Commodity already exists with name: " + request.name());
        }

        commodity.setName(request.name());
        commodity.setCategory(request.category());
        commodity.setUnit(request.unit());

        Commodity updatedCommodity = commodityRepository.save(commodity);
        return mapToResponse(updatedCommodity);
    }

    @Transactional
    public void deleteCommodity(Long id) {
        log.info("Deleting commodity with id: {}", id);
        if (!commodityRepository.existsById(id)) {
            throw new ResourceNotFoundException("Commodity", "id", id);
        }
        commodityRepository.deleteById(id);
    }

    private CommodityResponseDto mapToResponse(Commodity commodity) {
        return CommodityResponseDto.builder()
                .id(commodity.getId())
                .name(commodity.getName())
                .category(commodity.getCategory())
                .unit(commodity.getUnit())
                .createdAt(commodity.getCreatedAt())
                .build();
    }
}
