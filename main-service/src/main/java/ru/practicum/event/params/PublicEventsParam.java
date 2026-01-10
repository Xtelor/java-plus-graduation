package ru.practicum.event.params;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.event.SortEvents;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicEventsParam {

    private String text;
    private Long[] categories;
    private Boolean Paid;
    private String rangeStart;
    private String rangeEnd;
    private Boolean onlyAvailable;
    private SortEvents sort;
    private Integer from;
    private Integer size;
}
