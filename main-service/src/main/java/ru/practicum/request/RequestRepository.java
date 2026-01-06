package ru.practicum.request;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestStatus;

import java.util.List;

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
