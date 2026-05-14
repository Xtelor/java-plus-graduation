package ru.practicum.mappers;

import ru.practicum.dto.events.LocationDto;
import ru.practicum.models.Location;

public class LocationMapper {

    public static LocationDto toDto(Location location) {

        if (location == null) {
            return null;
        }

        return LocationDto.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .build();
    }

    public static Location toEntity(LocationDto dto) {

        if (dto == null) {
            return null;
        }

        Location location = new Location();
        location.setLat(dto.getLat());
        location.setLon(dto.getLon());

        return location;
    }
}
