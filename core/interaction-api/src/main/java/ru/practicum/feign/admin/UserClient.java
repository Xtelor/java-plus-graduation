package ru.practicum.feign.admin;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.admin.NewUserRequest;
import ru.practicum.dto.admin.UserDto;

import java.util.List;

@FeignClient(name = "user-service", path = "/admin/users")
public interface UserClient {

    // Создание пользователя
    @PostMapping
    UserDto createUser(@Valid @RequestBody NewUserRequest newUserRequest);

    // Получение списка пользователей
    @GetMapping
    List<UserDto> getUsers(
            @RequestParam(value = "ids", required = false) List<Long> ids,
            @RequestParam(value = "from", defaultValue = "0") Integer from,
            @RequestParam(value = "size", defaultValue = "10") Integer size);

    // Удаление пользователя
    @DeleteMapping("/{userId}")
    void deleteUser(@PathVariable("userId") Long userId);
}
