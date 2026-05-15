# Explore With Me — микросервисная архитектура

## О проекте

`Explore With Me` — сервис для публикации событий, подачи заявок на участие, модерации пользовательского контента и формирования подборок мероприятий.

В рамках проекта исходное монолитное приложение было декомпозировано на набор микросервисов.  
Внешние запросы маршрутизируются через API Gateway, межсервисное взаимодействие реализовано через OpenFeign, обнаружение сервисов — через Eureka, централизованная конфигурация — через Config Server.

Каждый прикладной сервис использует собственную базу данных PostgreSQL.

---

## Архитектура

Система состоит из следующих сервисов:

- **gateway** — единая точка входа в систему, маршрутизация внешних запросов;
- **discovery-server** — реестр сервисов (Eureka);
- **config-server** — централизованное хранилище конфигурации;
- **stats-server** — сбор и получение статистики просмотров;
- **user-service** — управление пользователями;
- **category-service** — управление категориями событий;
- **event-service** — основной сервис работы с событиями;
- **request-service** — работа с заявками на участие;
- **comment-service** — работа с комментариями и их модерацией;
- **compilation-service** — работа с подборками событий.

---

## Взаимодействие сервисов

### Внешний контур
Все клиентские запросы поступают в **gateway**, который перенаправляет их в соответствующий микросервис.

### Внутренний контур
Межсервисное взаимодействие построено через **OpenFeign**.

Основные зависимости:
- `event-service` обращается к:
    - `user-service` — информация об инициаторе события;
    - `category-service` — информация о категории;
    - `request-service` — количество подтверждённых заявок;
    - `comment-service` — количество комментариев;
    - `stats-server` — статистика просмотров.
- `request-service` обращается к:
    - `event-service` — получение события и проверка его состояния;
    - `user-service` — проверка пользователя.
- `comment-service` обращается к:
    - `event-service` — получение информации о событии;
    - `request-service` — проверка участия пользователя;
    - `user-service` — проверка пользователя.

---

## Конфигурация

Настройки сервисов вынесены в **Config Server**.

### Где находятся конфигурации
- централизованные настройки — в конфигурации `config-server`;
- локальные настройки сервисов — в `application.yml` / `application.properties` соответствующих модулей;
- маршруты Gateway — в конфигурации `gateway`;
- параметры инфраструктуры и баз данных — в `docker-compose`.

---

## Отказоустойчивость

Для повышения устойчивости межсервисного взаимодействия используются:
- **OpenFeign**
- **Resilience4j**
- **Retry**

Для некритичных зависимостей реализована стратегия graceful degradation:  
если зависимый сервис временно недоступен, основной сценарий не завершается ошибкой `5xx`, а возвращает безопасное значение по умолчанию.

Примеры:
- недоступен `stats-server` → `views = 0`;
- недоступен `request-service` → `confirmedRequests = 0`;
- недоступен `comment-service` → `commentCount = 0`;
- недоступен `category-service` → категория возвращается как `unknown`;
- недоступен `user-service` → инициатор возвращается как `unknown`.

---

## Внутренний API

### event-service

#### Internal API
- `GET /internal/events/{eventId}` — получить событие по id для внутренних сервисов;
- `GET /internal/events/category/{categoryId}/exists` — проверить использование категории в событиях.

#### Private API
- `POST /users/{userId}/events` — создать событие;
- `PATCH /users/{userId}/events/{eventId}` — обновить событие;
- `GET /users/{userId}/events` — получить события текущего пользователя;
- `GET /users/{userId}/events/{eventId}` — получить полную информацию о событии текущего пользователя.

#### Admin API
- `GET /admin/events` — поиск событий администратором;
- `PATCH /admin/events/{eventId}` — изменение события и его статуса администратором.

#### Public API
- `GET /events` — получить опубликованные события;
- `GET /events/{eventId}` — получить полную информацию об опубликованном событии.

---

### request-service

#### Private API
- `POST /users/{userId}/requests?eventId={eventId}` — создать заявку на участие;
- `PATCH /users/{userId}/requests/{requestId}/cancel` — отменить заявку;
- `GET /users/{userId}/requests` — получить заявки текущего пользователя.

#### Event owner API
- `GET /users/{userId}/events/{eventId}/requests` — получить заявки на участие в событии;
- `PATCH /users/{userId}/events/{eventId}/requests` — подтвердить или отклонить заявки.

---

### comment-service

#### Public API
- `GET /comments/events/{eventId}` — получить опубликованные комментарии события.

#### Private API
- `POST /users/{userId}/comments?eventId={eventId}` — создать комментарий;
- `DELETE /users/{userId}/comments/{commentId}` — удалить собственный комментарий;
- `GET /users/{userId}/comments` — получить комментарии пользователя.

#### Admin API
- `GET /admin/comments/moderation` — получить комментарии на модерации;
- `PATCH /admin/comments/{commentId}/moderate` — опубликовать или отклонить комментарий.

---

### category-service

#### Public API
- `GET /categories`
- `GET /categories/{catId}`

#### Admin API
- `POST /admin/categories`
- `PATCH /admin/categories/{catId}`
- `DELETE /admin/categories/{catId}`

---

### user-service

#### Admin API
- `POST /admin/users`
- `GET /admin/users`
- `DELETE /admin/users/{userId}`

---

### compilation-service

#### Public API
- `GET /compilations`
- `GET /compilations/{compId}`

#### Admin API
- `POST /admin/compilations`
- `PATCH /admin/compilations/{compId}`
- `DELETE /admin/compilations/{compId}`

---

### stats-server

#### API
- `POST /hit` — сохранить информацию о запросе к endpoint;
- `GET /stats` — получить статистику просмотров.

---

## Внешний API

Спецификации внешнего API:

- основной сервис Explore With Me:  
  https://raw.githubusercontent.com/yandex-praktikum/java-explore-with-me/main/ewm-main-service-spec.json

- сервис статистики:  
  https://raw.githubusercontent.com/yandex-praktikum/java-explore-with-me/main/ewm-stats-service-spec.json