package ru.practicum.kafka.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "collector.kafka.producer.topic")
public class KafkaConfig {
    @NotBlank(message = "Actions cannot be empty.")
    String actions;
}