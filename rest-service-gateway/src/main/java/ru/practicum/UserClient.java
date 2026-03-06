package ru.practicum;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.dto.user.UserDto;

import java.util.Optional;

@FeignClient(name = "user-service", path = "/users")
public interface UserClient {
    @GetMapping("/{userId}")
    Optional<UserDto> findById(@PathVariable Long userId);

    @GetMapping("/{userId}/check")
    boolean existsById(@PathVariable("userId") Long userId);
}