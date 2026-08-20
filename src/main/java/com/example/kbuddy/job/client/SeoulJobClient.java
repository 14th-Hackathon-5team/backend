package com.example.kbuddy.job.client;

import com.example.kbuddy.job.config.SeoulJobProperties;
import com.example.kbuddy.job.dto.SeoulJobApiEnvelope;
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
public class SeoulJobClient {

    private final RestClient restClient;
    private final SeoulJobProperties properties;

    public SeoulJobClient(RestClient seoulJobRestClient, SeoulJobProperties properties) {
        this.restClient = seoulJobRestClient;
        this.properties = properties;
    }

    public SeoulJobApiEnvelope search(int startIndex, int endIndex) {
        try {
            return restClient.get()
                    .uri("/{apiKey}/json/GlobalJobSearch/{startIndex}/{endIndex}/",
                            properties.apiKey(), startIndex, endIndex)
                    .retrieve()
                    .body(SeoulJobApiEnvelope.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new SeoulJobClientException(SeoulJobClientException.Reason.SERVER_UNAVAILABLE, "Seoul Open API error response", e);
        } catch (RestClientException e) {
            if (isTimeout(e)) {
                throw new SeoulJobClientException(SeoulJobClientException.Reason.REQUEST_TIMEOUT, "Seoul Open API request timed out", e);
            }
            if (isNetworkFailure(e)) {
                throw new SeoulJobClientException(SeoulJobClientException.Reason.SERVER_UNAVAILABLE, "Seoul Open API communication failed", e);
            }
            throw new SeoulJobClientException(SeoulJobClientException.Reason.RESPONSE_INVALID, "Invalid Seoul Open API response", e);
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
