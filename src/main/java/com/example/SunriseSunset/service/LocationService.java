package com.example.SunriseSunset.service;

import com.example.SunriseSunset.dto.LocationDTO;
import com.example.SunriseSunset.model.LocationEntity;
import com.example.SunriseSunset.model.SunriseSunsetEntity;
import com.example.SunriseSunset.repository.LocationRepository;
import com.example.SunriseSunset.repository.SunriseSunsetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private static final Logger logger = LoggerFactory.getLogger(LocationService.class);

    private final LocationRepository locationRepository;
    private final SunriseSunsetRepository sunriseSunsetRepository;
    private final Map<String, Object> entityCache;

    @Autowired
    public LocationService(LocationRepository locationRepository,
                           SunriseSunsetRepository sunriseSunsetRepository,
                           Map<String, Object> entityCache) {
        this.locationRepository = locationRepository;
        this.sunriseSunsetRepository = sunriseSunsetRepository;
        this.entityCache = entityCache;
    }

    public LocationDTO createLocation(LocationDTO dto) {
        LocationEntity entity = new LocationEntity();
        entity.name = dto.getName();
        entity.country = dto.getCountry();
        if (dto.getSunriseSunsetIds() != null && !dto.getSunriseSunsetIds().isEmpty()) {
            List<SunriseSunsetEntity> sunriseSunsets = sunriseSunsetRepository.findAllById(dto.getSunriseSunsetIds());
            entity.sunriseSunsets = sunriseSunsets;
        }
        LocationEntity savedEntity = locationRepository.save(entity);
        LocationDTO savedDto = convertToDTO(savedEntity);
        logger.info("Caching Location with ID {} after creation", savedEntity.id);
        entityCache.put("Location_" + savedEntity.id, savedDto);
        logger.debug("Invalidating Location_All cache after creation of Location ID {}", savedEntity.id);
        entityCache.remove("Location_All");
        return savedDto;
    }

    public LocationDTO getLocationById(Integer id) {
        String cacheKey = "Location_" + id;
        if (entityCache.containsKey(cacheKey)) {
            logger.debug("Cache hit for Location ID {}", id);
            return (LocationDTO) entityCache.get(cacheKey);
        }
        logger.debug("Cache miss for Location ID {}, querying database", id);
        Optional<LocationEntity> entity = locationRepository.findById(id);
        if (entity.isPresent()) {
            LocationDTO dto = convertToDTO(entity.get());
            logger.info("Caching Location with ID {} after database query", id);
            entityCache.put(cacheKey, dto);
            return dto;
        }
        return null;
    }

    public List<LocationDTO> getAllLocations() {
        String cacheKey = "Location_All";
        if (entityCache.containsKey(cacheKey)) {
            logger.debug("Cache hit for all Locations");
            return (List<LocationDTO>) entityCache.get(cacheKey);
        }
        logger.debug("Cache miss for all Locations, querying database");
        List<LocationEntity> entities = locationRepository.findAll();
        List<LocationDTO> dtos = entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        for (LocationDTO dto : dtos) {
            logger.info("Caching Location with ID {} after database query", dto.getId());
            entityCache.put("Location_" + dto.getId(), dto);
        }
        logger.info("Caching all Locations under Location_All");
        entityCache.put(cacheKey, dtos);
        return dtos;
    }

    public LocationDTO updateLocation(Integer id, LocationDTO dto) {
        Optional<LocationEntity> existing = locationRepository.findById(id);
        if (existing.isPresent()) {
            LocationEntity entity = existing.get();
            entity.name = dto.getName();
            entity.country = dto.getCountry();

            if (dto.getSunriseSunsetIds() != null && !dto.getSunriseSunsetIds().isEmpty()) {
                List<SunriseSunsetEntity> sunriseSunsets = sunriseSunsetRepository.findAllById(dto.getSunriseSunsetIds());
                entity.sunriseSunsets = sunriseSunsets;
            } else {
                entity.sunriseSunsets.clear();
            }

            LocationEntity updatedEntity = locationRepository.save(entity);
            LocationDTO updatedDto = convertToDTO(updatedEntity);
            logger.info("Updating cache for Location with ID {}", id);
            entityCache.put("Location_" + id, updatedDto);
            logger.debug("Invalidating Location_All cache after update of Location ID {}", id);
            entityCache.remove("Location_All");
            return updatedDto;
        }
        return null;
    }

    public void deleteLocation(Integer id) {
        locationRepository.deleteById(id);
        logger.info("Removing Location with ID {} from cache", id);
        entityCache.remove("Location_" + id);
        logger.debug("Invalidating Location_All cache after deletion of Location ID {}", id);
        entityCache.remove("Location_All");
    }

    private LocationDTO convertToDTO(LocationEntity entity) {
        List<Integer> sunriseSunsetIds = entity.sunriseSunsets.stream()
                .map(sunriseSunset -> sunriseSunset.id)
                .collect(Collectors.toList());
        return new LocationDTO(entity.id, entity.name, entity.country, sunriseSunsetIds);
    }
}