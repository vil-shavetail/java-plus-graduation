package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.kafka.config.KafkaConfig;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectorService {
    private final KafkaTemplate<String, UserActionAvro> kafkaTemplate;
    private final KafkaConfig kafkaConfig;

    public void registerUserAction(UserActionProto proto) {
        log.debug("Converting user action {} to Avro format.", proto.getUserId());
        try {
            UserActionAvro avro = UserActionAvro.newBuilder()
                    .setUserId(proto.getUserId())
                    .setEventId(proto.getEventId())
                    .setActionType(mapActionType(proto.getActionType()))
                    .setTimestamp(mapTimestamp(proto.getTimestamp()))
                    .build();

            String topic = kafkaConfig.getActions();
            kafkaTemplate.send(topic, avro)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("The message was successfully sent to Kafka: {}", result.getRecordMetadata());
                        } else {
                            log.error("Error sending a message to Kafka for a user {}", avro.getUserId(), ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Couldn't process user action: {}", e.getMessage());
            throw new IllegalArgumentException("Avro data generation error", e);
        }
    }

    private ActionTypeAvro mapActionType(ActionTypeProto protoType) {
        return switch (protoType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> {
                log.error("Unknown type of action: {}", protoType);
                throw new IllegalArgumentException("Unsupported type of action: " + protoType);
            }
        };
    }

    private Instant mapTimestamp(com.google.protobuf.Timestamp protoTimestamp) {
        return Instant.ofEpochSecond(
                protoTimestamp.getSeconds(),
                protoTimestamp.getNanos()
        );
    }
}