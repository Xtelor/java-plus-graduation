package ru.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.admin.NewUserRequest;
import ru.practicum.dto.admin.UserDto;
import ru.practicum.feign.admin.UserClient;
import ru.practicum.service.UserService;

import java.util.List;

@RestController
@RequestMapping(path = "/admin/users")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AdminUserController implements UserClient {

    private final UserService userService;

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody NewUserRequest newUserRequest) {
        log.info("POST запрос на создание пользователя: {}", newUserRequest);
        return userService.createUser(newUserRequest);
    }

    @Override
    @GetMapping
    public List<UserDto> getUsers(
            @RequestParam(required = false) List<Long> ids,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {

        log.info("GET запрос на получение пользователей с IDs: {}, from: {}, size: {}", ids, from, size);

        PageRequest pageRequest = PageRequest.of(from / size, size);
        return userService.getUsers(ids, pageRequest);
    }

    @Override
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long userId) {
        log.info("DELETE запрос на удаление пользователя с ID: {}", userId);
        userService.deleteUser(userId);
    }
}