package ru.practicum.mapper;

import ru.practicum.dto.admin.NewUserRequest;
import ru.practicum.dto.admin.UserDto;
import ru.practicum.entity.User;

public class UserMapper {

    public static UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public static User toEntity(NewUserRequest newUserRequest) {
        return User.builder()
                .name(newUserRequest.getName())
                .email(newUserRequest.getEmail())
                .build();
    }
}