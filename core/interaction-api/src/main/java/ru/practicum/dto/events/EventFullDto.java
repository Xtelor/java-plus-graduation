package ru.practicum.dto.events;

import lombok.*;
import ru.practicum.dto.admin.UserShortDto;
import ru.practicum.dto.categories.CategoryDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventFullDto {

    private String annotation;
    private CategoryDto category;
    private Long confirmedRequests;
    private String createdOn;
    private String description;
    private String eventDate;
    private Long id;
    private UserShortDto initiator;
    private LocationDto location;
    private Boolean paid;
    private Integer participantLimit;
    private String publishedOn;
    private Boolean requestModeration;
    private String state;
    private String title;
    private Long views;
    private Long commentCount;
}
