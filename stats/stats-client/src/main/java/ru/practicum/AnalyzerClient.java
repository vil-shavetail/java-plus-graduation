package ru.practicum;

import com.google.common.collect.Lists;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.*;

import java.util.List;

@Service
public class AnalyzerClient {
    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub recommendationsController;

    /**
     * Рекомендации для пользователя
     * @param userPredictionsRequest
     * @return
     */
    public List<RecommendedEventProto> getRecommendedEventsForUser(UserPredictionsRequestProto userPredictionsRequest) {
        return Lists.newArrayList(recommendationsController.getRecommendationsForUser(userPredictionsRequest));
    }

    /**
     * Список похожих событий
     * @param similarEventsRequest
     * @return
     */
    public List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto similarEventsRequest) {
        return Lists.newArrayList(recommendationsController.getSimilarEvents(similarEventsRequest));
    }

    /**
     * Список количества с данным событием
     * @param interactionsCountRequest
     * @return
     */
    public List<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto interactionsCountRequest) {
        return Lists.newArrayList(recommendationsController.getInteractionsCount(interactionsCountRequest));
    }
}
