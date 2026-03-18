package ru.practicum.kafka.config;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "aggregator.kafka.producer.topic")
public class KafkaConfig {
    @NotNull(message = "Events cannot be empty.")
    private String events;
}