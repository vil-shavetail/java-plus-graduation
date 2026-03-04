package ru.practicum;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.DTO.RequestStatisticDto;
import ru.practicum.DTO.ResponseStatisticDto;

import java.util.List;

@FeignClient(name = "stats-server")
public interface StatsClient {
    @PostMapping("/hit")
    void saveHit(@RequestBody RequestStatisticDto requestStatisticDto);

    @GetMapping("/stats")
    List<ResponseStatisticDto> getStats(
            @RequestParam("start") String start,
            @RequestParam("end") String end,
            @RequestParam(value = "uris", required = false) List<String> uris,
            @RequestParam("unique") boolean unique
    );
}