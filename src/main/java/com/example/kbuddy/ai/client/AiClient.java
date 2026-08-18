package com.example.kbuddy.ai.client;

import com.example.kbuddy.ai.config.AiProperties;
import com.example.kbuddy.ai.dto.AiChatRequest;
import com.example.kbuddy.ai.dto.AiChatResponse;
import com.example.kbuddy.ai.dto.AiRecommendationRequest;
import com.example.kbuddy.ai.dto.AiRecommendationResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.function.Supplier;

@Component
public class AiClient {

    private static final String RECOMMENDATIONS_PATH = "/recommend";
    private static final String CHAT_PATH = "/ai/chat";
    private static final String API_KEY_HEADER = "X-Internal-API-Key";

    private final RestClient recommendationRestClient;
    private final RestClient chatRestClient;
    private final AiProperties aiProperties;

    public AiClient(
            @Qualifier("aiRecommendationRestClient") RestClient recommendationRestClient,
            @Qualifier("aiChatRestClient") RestClient chatRestClient,
            AiProperties aiProperties
    ) {
        this.recommendationRestClient = recommendationRestClient;
        this.chatRestClient = chatRestClient;
        this.aiProperties = aiProperties;
    }

    public AiRecommendationResponse recommendations(AiRecommendationRequest request) {
        return executeWithRetry(() -> recommendationRestClient.post()
                .uri(RECOMMENDATIONS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER, aiProperties.internalApiKey())
                .body(request)
                .retrieve()
                .body(AiRecommendationResponse.class));
    }

    public AiChatResponse chat(AiChatRequest request) {
        return executeWithRetry(() -> chatRestClient.post()
                .uri(CHAT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .header(API_KEY_HEADER, aiProperties.internalApiKey())
                .body(request)
                .retrieve()
                .body(AiChatResponse.class));
    }

    /**
     * 4xx는 즉시 그대로 전파(재시도 없음). 그 외 통신/서버 오류는 최대 1회만 재시도한다.
     * SimpleClientHttpRequestFactory(HttpURLConnection) 특성상 read timeout이
     * ResourceAccessException이 아니라 일반 RestClientException으로 래핑되어 올 수 있으므로,
     * 타입 하나만으로 판단하지 않고 cause 체인을 직접 검사해 분류한다.
     */
    private <T> T executeWithRetry(Supplier<T> call) {
        try {
            return call.get();
        } catch (HttpClientErrorException e) {
            throw e;
        } catch (RestClientException e) {
            if (!isRetryable(e)) {
                throw toAiClientException(e);
            }
            try {
                return call.get();
            } catch (RestClientException retryFailure) {
                throw toAiClientException(retryFailure);
            }
        }
    }

    private boolean isRetryable(RestClientException e) {
        return e instanceof HttpServerErrorException || isTimeout(e) || isNetworkFailure(e);
    }

    private AiClientException toAiClientException(RestClientException e) {
        if (e instanceof HttpServerErrorException) {
            return new AiClientException(AiClientException.Reason.SERVER_UNAVAILABLE, "FastAPI server error", e);
        }
        if (isTimeout(e)) {
            return new AiClientException(AiClientException.Reason.REQUEST_TIMEOUT, "FastAPI request timed out", e);
        }
        if (isNetworkFailure(e)) {
            return new AiClientException(AiClientException.Reason.SERVER_UNAVAILABLE, "FastAPI communication failed", e);
        }
        return new AiClientException(AiClientException.Reason.RESPONSE_INVALID, "Invalid FastAPI response", e);
    }

    private boolean isTimeout(Throwable e) {
        return containsCause(e, SocketTimeoutException.class);
    }

    private boolean isNetworkFailure(Throwable e) {
        return e instanceof ResourceAccessException
                || containsCause(e, ConnectException.class)
                || containsCause(e, UnknownHostException.class);
    }

    private boolean containsCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
