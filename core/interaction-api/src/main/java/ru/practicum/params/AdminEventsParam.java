package ru.practicum.params;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.enums.State;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminEventsParam {

    private Long[] users;
    private State[] states;
    private Long[] categories;
    private String rangeStart;
    private String rangeEnd;
    private Integer from;
    private Integer size;
}
