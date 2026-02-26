package ru.practicum;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "main-service.url")
public class EwmServiceClient {
    private final RestClient restClient;

    public EwmServiceClient(@Value("${main-service.url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    // методы в зависимости от требований ФЗ ( swagger ).
}
