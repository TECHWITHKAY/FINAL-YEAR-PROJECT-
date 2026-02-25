package com.ghana.commoditymonitor.service;

import com.ghana.commoditymonitor.dto.request.CityRequestDto;
import com.ghana.commoditymonitor.dto.response.CityResponseDto;
import com.ghana.commoditymonitor.entity.City;
import com.ghana.commoditymonitor.exception.DuplicateResourceException;
import com.ghana.commoditymonitor.exception.ResourceNotFoundException;
import com.ghana.commoditymonitor.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for City-related business logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CityService {

    private final CityRepository cityRepository;

    @Transactional
    public CityResponseDto createCity(CityRequestDto request) {
        log.info("Creating new city: {}", request.name());
        if (cityRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("City already exists with name: " + request.name());
        }

        City city = City.builder()
                .name(request.name())
                .region(request.region())
                .build();

        City savedCity = cityRepository.save(city);
        return mapToResponse(savedCity);
    }

    public CityResponseDto getCityById(Long id) {
        log.debug("Fetching city with id: {}", id);
        return cityRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("City", "id", id));
    }

    public List<CityResponseDto> getAllCities() {
        log.debug("Fetching all cities");
        return cityRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CityResponseDto> getCitiesByRegion(String region) {
        log.debug("Fetching cities in region: {}", region);
        return cityRepository.findByRegion(region).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CityResponseDto updateCity(Long id, CityRequestDto request) {
        log.info("Updating city with id: {}", id);
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("City", "id", id));

        if (!city.getName().equals(request.name()) && cityRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("City already exists with name: " + request.name());
        }

        city.setName(request.name());
        city.setRegion(request.region());

        City updatedCity = cityRepository.save(city);
        return mapToResponse(updatedCity);
    }

    @Transactional
    public void deleteCity(Long id) {
        log.info("Deleting city with id: {}", id);
        if (!cityRepository.existsById(id)) {
            throw new ResourceNotFoundException("City", "id", id);
        }
        cityRepository.deleteById(id);
    }

    private CityResponseDto mapToResponse(City city) {
        return CityResponseDto.builder()
                .id(city.getId())
                .name(city.getName())
                .region(city.getRegion())
                .createdAt(city.getCreatedAt())
                .build();
    }
}
