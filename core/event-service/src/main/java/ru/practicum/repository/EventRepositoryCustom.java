package ru.practicum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.practicum.entity.Event;
import ru.practicum.params.AdminEventsParam;
import ru.practicum.params.PublicEventsParam;

public interface EventRepositoryCustom {

    Page<Event> getEventsPublic(PublicEventsParam publicEventsParam, Pageable pageable);

    Page<Event> searchEventsByAdmin(AdminEventsParam adminEventsParam, Pageable pageable);
}
