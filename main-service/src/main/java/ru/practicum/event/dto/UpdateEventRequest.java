package ru.practicum.event.dto;

import ru.practicum.event.Location;

public interface UpdateEventRequest {
    String getAnnotation();
    String getDescription();
    String getTitle();
    Boolean getPaid();
    Integer getParticipantLimit();
    Boolean getRequestModeration();
    Location getLocation();
    Long getCategory();
    String getEventDate();
}
