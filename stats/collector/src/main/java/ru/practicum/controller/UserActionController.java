package ru.practicum.controller;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.kafka.producer.UserActionProducer;

import java.time.Instant;

@GrpcService
public class UserActionController extends UserActionControllerGrpc.UserActionControllerImplBase {
    private final UserActionProducer userActionProducer;

    public UserActionController(UserActionProducer userActionProducer) {
        this.userActionProducer = userActionProducer;
    }

    @Override
    public void collectUserAction(UserActionProto request,
                                  StreamObserver<Empty> responseObserver) {

        // Преобразуем gRPC-сообщение в Avro-объект
        UserActionAvro avroMessage = convertToAvro(request);

        // Отправляем в Kafka
        userActionProducer.sendUserAction(avroMessage);

        // Возвращаем пустой ответ
        responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    private UserActionAvro convertToAvro(UserActionProto proto) {
        UserActionAvro avro = new UserActionAvro();
        avro.setUserId(proto.getUserId());
        avro.setEventId(proto.getEventId());

        // Конвертируем тип действия
        ActionTypeAvro actionType = switch (proto.getActionType()) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> throw new IllegalArgumentException("Unknown action type: " + proto.getActionType());
        };
        avro.setActionType(actionType);

        // Конвертируем timestamp
        avro.setTimestamp(Instant.ofEpochSecond(proto.getTimestamp().getSeconds() * 1000L +
                proto.getTimestamp().getNanos() / 1_000_000));

        return avro;
    }
}
