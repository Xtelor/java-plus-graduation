package ru.practicum.dto.events;

import lombok.*;
import ru.practicum.dto.admin.UserShortDto;
import ru.practicum.dto.categories.CategoryDto;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventShortDto {

    private String annotation;
    private CategoryDto category;
    private Long confirmedRequests;
    private String eventDate;
    private Long id;
    private UserShortDto initiator;
    private Boolean paid;
    private String title;
    private Long views;
}
