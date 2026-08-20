package com.example.kbuddy.news.client;

import com.example.kbuddy.ai.config.AiProperties;
import com.example.kbuddy.news.dto.NewsResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

@Component
public class NewsClient {

    private static final String NEWS_PATH = "/news";
    private static final String API_KEY_HEADER = "X-Internal-API-Key";

    private final RestClient restClient;
    private final AiProperties aiProperties;

    public NewsClient(
            @Qualifier("aiNewsRestClient") RestClient aiNewsRestClient,
            AiProperties aiProperties
    ) {
        this.restClient = aiNewsRestClient;
        this.aiProperties = aiProperties;
    }

    /**
     * language는 AI 서버와 합의된 "ko"/"en" 코드({@link com.example.kbuddy.ai.dto.AiUser#languageCode}
     * 로 변환된 값)여야 한다. null이면 쿼리 파라미터 없이 호출해 AI 서버의 기본 언어 처리에 맡긴다.
     */
    public NewsResponse getNews(String language) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(NEWS_PATH);
                        if (language != null) {
                            uriBuilder.queryParam("language", language);
                        }
                        return uriBuilder.build();
                    })
                    .header(API_KEY_HEADER, aiProperties.internalApiKey())
                    .retrieve()
                    .body(NewsResponse.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new NewsClientException(NewsClientException.Reason.SERVER_UNAVAILABLE, "AI news API error response", e);
        } catch (RestClientException e) {
            if (isTimeout(e)) {
                throw new NewsClientException(NewsClientException.Reason.REQUEST_TIMEOUT, "AI news API request timed out", e);
            }
            if (isNetworkFailure(e)) {
                throw new NewsClientException(NewsClientException.Reason.SERVER_UNAVAILABLE, "AI news API communication failed", e);
            }
            throw new NewsClientException(NewsClientException.Reason.RESPONSE_INVALID, "Invalid AI news API response", e);
        }
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
