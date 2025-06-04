package com.example.SunriseSunset.service;

import com.example.SunriseSunset.dto.SunriseSunsetDTO;
import com.example.SunriseSunset.model.LocationEntity;
import com.example.SunriseSunset.model.SunriseSunsetEntity;
import com.example.SunriseSunset.dto.SunriseSunsetModel;
import com.example.SunriseSunset.repository.LocationRepository;
import com.example.SunriseSunset.repository.SunriseSunsetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SunriseSunsetService {

    private static final Logger logger = LoggerFactory.getLogger(SunriseSunsetService.class);

    private final String SUN_API_URL = "https://api.sunrise-sunset.org/json";
    private final RestTemplate restTemplate;
    private final SunriseSunsetRepository sunriseSunsetRepository;
    private final LocationRepository locationRepository;
    private final Map<String, Object> entityCache;

    @Autowired
    public SunriseSunsetService(RestTemplate restTemplate,
                                SunriseSunsetRepository sunriseSunsetRepository,
                                LocationRepository locationRepository,
                                Map<String, Object> entityCache) {
        this.restTemplate = restTemplate;
        this.sunriseSunsetRepository = sunriseSunsetRepository;
        this.locationRepository = locationRepository;
        this.entityCache = entityCache;
    }

    public SunriseSunsetDTO createSunriseSunset(SunriseSunsetDTO dto) {
        SunriseSunsetModel sunData = getSunriseSunset(dto.getLatitude(), dto.getLongitude(), dto.getDate().toString());
        SunriseSunsetEntity entity = new SunriseSunsetEntity();
        entity.date = dto.getDate();
        entity.latitude = dto.getLatitude();
        entity.longitude = dto.getLongitude();
        entity.sunrise = OffsetDateTime.parse(sunData.getResults().getSunrise());
        entity.sunset = OffsetDateTime.parse(sunData.getResults().getSunset());

        if (dto.getLocationIds() != null && !dto.getLocationIds().isEmpty()) {
            List<LocationEntity> locations = locationRepository.findAllById(dto.getLocationIds());
            entity.locations = locations;
        }

        SunriseSunsetEntity savedEntity = sunriseSunsetRepository.save(entity);
        SunriseSunsetDTO savedDto = convertToDTO(savedEntity);
        logger.info("Caching SunriseSunset with ID {} after creation", savedEntity.id);
        entityCache.put("SunriseSunset_" + savedEntity.id, savedDto);
        logger.debug("Invalidating SunriseSunset_All cache after creation of SunriseSunset ID {}", savedEntity.id);
        entityCache.remove("SunriseSunset_All");
        return savedDto;
    }

    public SunriseSunsetDTO getSunriseSunsetById(Integer id) {
        String cacheKey = "SunriseSunset_" + id;
        if (entityCache.containsKey(cacheKey)) {
            logger.debug("Cache hit for SunriseSunset ID {}", id);
            return (SunriseSunsetDTO) entityCache.get(cacheKey);
        }
        logger.debug("Cache miss for SunriseSunset ID {}, querying database", id);
        Optional<SunriseSunsetEntity> entity = sunriseSunsetRepository.findById(id);
        if (entity.isPresent()) {
            SunriseSunsetDTO dto = convertToDTO(entity.get());
            logger.info("Caching SunriseSunset with ID {} after database query", id);
            entityCache.put(cacheKey, dto);
            return dto;
        }
        return null;
    }

    public List<SunriseSunsetDTO> getAllSunriseSunsets() {
        String cacheKey = "SunriseSunset_All";
        if (entityCache.containsKey(cacheKey)) {
            logger.debug("Cache hit for all SunriseSunsets");
            return (List<SunriseSunsetDTO>) entityCache.get(cacheKey);
        }
        logger.debug("Cache miss for all SunriseSunsets, querying database");
        List<SunriseSunsetEntity> entities = sunriseSunsetRepository.findAll();
        List<SunriseSunsetDTO> dtos = entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        for (SunriseSunsetDTO dto : dtos) {
            logger.info("Caching SunriseSunset with ID {} after database query", dto.getId());
            entityCache.put("SunriseSunset_" + dto.getId(), dto);
        }
        logger.info("Caching all SunriseSunsets under SunriseSunset_All");
        entityCache.put(cacheKey, dtos);
        return dtos;
    }

    public SunriseSunsetDTO updateSunriseSunset(Integer id, SunriseSunsetDTO dto) {
        Optional<SunriseSunsetEntity> existing = sunriseSunsetRepository.findById(id);
        if (existing.isPresent()) {
            SunriseSunsetEntity entity = existing.get();
            entity.date = dto.getDate();
            entity.latitude = dto.getLatitude();
            entity.longitude = dto.getLongitude();
            SunriseSunsetModel sunData = getSunriseSunset(dto.getLatitude(), dto.getLongitude(), dto.getDate().toString());
            entity.sunrise = OffsetDateTime.parse(sunData.getResults().getSunrise());
            entity.sunset = OffsetDateTime.parse(sunData.getResults().getSunset());

            if (dto.getLocationIds() != null && !dto.getLocationIds().isEmpty()) {
                List<LocationEntity> locations = locationRepository.findAllById(dto.getLocationIds());
                entity.locations = locations;
            } else {
                entity.locations.clear();
            }

            SunriseSunsetEntity updatedEntity = sunriseSunsetRepository.save(entity);
            SunriseSunsetDTO updatedDto = convertToDTO(updatedEntity);
            logger.info("Updating cache for SunriseSunset with ID {}", id);
            entityCache.put("SunriseSunset_" + id, updatedDto);
            logger.debug("Invalidating SunriseSunset_All cache after update of SunriseSunset ID {}", id);
            entityCache.remove("SunriseSunset_All");
            return updatedDto;
        }
        return null;
    }

    public void deleteSunriseSunset(Integer id) {
        sunriseSunsetRepository.deleteById(id);
        logger.info("Removing SunriseSunset with ID {} from cache", id);
        entityCache.remove("SunriseSunset_" + id);
        logger.debug("Invalidating SunriseSunset_All cache after deletion of SunriseSunset ID {}", id);
        entityCache.remove("SunriseSunset_All");
    }

    public List<SunriseSunsetDTO> getSunriseSunsetsByLocationId(Integer locationId) {
        String cacheKey = "SunriseSunset_Location_" + locationId;
        if (entityCache.containsKey(cacheKey)) {
            logger.debug("Cache hit for SunriseSunsets by Location ID {}", locationId);
            return (List<SunriseSunsetDTO>) entityCache.get(cacheKey);
        }
        logger.debug("Cache miss for SunriseSunsets by Location ID {}, querying database", locationId);
        List<SunriseSunsetEntity> entities = sunriseSunsetRepository.findByLocationId(locationId);
        List<SunriseSunsetDTO> dtos = entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        for (SunriseSunsetDTO dto : dtos) {
            logger.info("Caching SunriseSunset with ID {} after database query", dto.getId());
            entityCache.put("SunriseSunset_" + dto.getId(), dto);
        }
        logger.info("Caching SunriseSunsets by Location ID {} under key {}", locationId, cacheKey);
        entityCache.put(cacheKey, dtos);
        return dtos;
    }

    public List<SunriseSunsetDTO> getSunriseSunsetsByDateAndLocationName(LocalDate date, String locationName) {
        String cacheKey = "SunriseSunset_Date_" + date + "_Location_" + locationName;
        if (entityCache.containsKey(cacheKey)) {
            logger.debug("Cache hit for SunriseSunsets by Date {} and Location {}", date, locationName);
            return (List<SunriseSunsetDTO>) entityCache.get(cacheKey);
        }
        logger.debug("Cache miss for SunriseSunsets by Date {} and Location {}, querying database", date, locationName);
        List<SunriseSunsetEntity> entities = sunriseSunsetRepository.findByDateAndLocationName(date, locationName);
        List<SunriseSunsetDTO> dtos = entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        for (SunriseSunsetDTO dto : dtos) {
            logger.info("Caching SunriseSunset with ID {} after database query", dto.getId());
            entityCache.put("SunriseSunset_" + dto.getId(), dto);
        }
        logger.info("Caching SunriseSunsets by Date {} and Location {} under key {}", date, locationName, cacheKey);
        entityCache.put(cacheKey, dtos);
        return dtos;
    }

    private SunriseSunsetDTO convertToDTO(SunriseSunsetEntity entity) {
        List<Integer> locationIds = entity.locations.stream()
                .map(location -> location.id)
                .collect(Collectors.toList());
        return new SunriseSunsetDTO(
                entity.id, entity.date, entity.latitude, entity.longitude,
                entity.sunrise, entity.sunset, locationIds
        );
    }

    private SunriseSunsetModel getSunriseSunset(double lat, double lng, String date) {
        String url = String.format("%s?lat=%f&lng=%f&date=%s&formatted=0", SUN_API_URL, lat, lng, date);
        try {
            return restTemplate.getForObject(url, SunriseSunsetModel.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch sunrise/sunset data: " + e.getMessage());
        }
    }
}