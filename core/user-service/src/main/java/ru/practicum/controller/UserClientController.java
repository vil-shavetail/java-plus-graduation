package ru.practicum.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.user.UserDto;
import ru.practicum.service.UserService;

import java.util.Optional;

@RestController
@RequestMapping("/client/users")
@RequiredArgsConstructor
public class UserClientController {
    private final UserService userService;

    @GetMapping("/{userId}")
    public Optional<UserDto> getUserById(@PathVariable @Positive Long userId) {
        return userService.findById(userId);
    }

    @GetMapping("/{userId}/check")
    public boolean checkUserExists(@PathVariable @Positive Long userId) {
        return userService.existsById(userId);
    }
}