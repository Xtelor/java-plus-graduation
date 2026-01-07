package ru.practicum.event.dto;

import lombok.*;
import ru.practicum.category.CategoryDto;
import ru.practicum.event.Location;
import ru.practicum.user.UserShortDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventFullDto {

    private String annotation;
    private CategoryDto category;
    private String createdOn;
    private String description;
    private String eventDate;
    private Long id;
    private UserShortDto initiator;
    private Location location;
    private boolean paid;
    private Integer participationLimit;
    private String publishedOn;
    private boolean requestModeration;
    private String state;
    private String title;
    private Integer views;
}
