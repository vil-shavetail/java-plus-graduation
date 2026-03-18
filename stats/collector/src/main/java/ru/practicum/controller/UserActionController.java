package ru.practicum.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.service.CollectorService;


@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionController extends UserActionControllerGrpc.UserActionControllerImplBase {
    private final CollectorService collectorService;

    @Override
    public void collectUserAction(UserActionProto request,
                                  StreamObserver<Empty> responseObserver) {
        if (request == null || !request.hasTimestamp()) {
            log.warn("Invalid request received: {}", request);
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("The request must not be null.")
                    .asRuntimeException());
            return;
        }
        try {
            log.debug("Processing user action {} for event {}", request.getUserId(), request.getEventId());
            collectorService.registerUserAction(request);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            handleError(e, responseObserver);
        }
    }

    private void handleError(Exception e, StreamObserver<Empty> responseObserver) {
        Status status;
        if (e instanceof IllegalArgumentException) {
            log.error("Validation error: {}", e.getMessage());
            status = Status.INVALID_ARGUMENT.withDescription(e.getMessage());
        } else {
            log.error("Data collection error: ", e);
            status = Status.INTERNAL.withDescription("Collector service error.");
        }
        responseObserver.onError(status.withCause(e).asRuntimeException());
    }
}