package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.entity.Request;
import ru.practicum.enums.RequestStatus;

import java.util.List;
import java.util.Set;

public interface RequestRepository extends JpaRepository<Request, Long> {

    // Поиск всех запросов пользователя
    List<Request> findAllByRequesterId(Long requesterId);

    // Подсчет количества подтвержденных заявок
    Long countByEventIdAndStatus(Long eventId, RequestStatus status);

    // Проверка на дубликаты
    Boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    // Получение списка заявок для конкретного события
    List<Request> findAllByEventId(Long eventId);

    // Получение списка заявок по списку их ID
    List<Request> findAllByIdIn(List<Long> ids);
}
