package com.example.kbuddy.news.client;

import com.example.kbuddy.ai.config.AiProperties;
import com.example.kbuddy.news.dto.NewsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NewsClientTest {

    private static final String BASE_URL = "http://localhost:8000";
    private static final String API_KEY = "test-internal-api-key";
    private static final AiProperties AI_PROPERTIES =
            new AiProperties(BASE_URL, API_KEY, Duration.ofSeconds(10), Duration.ofSeconds(20), Duration.ofSeconds(30));

    private MockRestServiceServer mockServer;

    private NewsClient buildClientWithMockServer() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        return new NewsClient(builder.build(), AI_PROPERTIES);
    }

    @Test
    void language가_ko이면_news_language_ko_쿼리스트링으로_요청한다() {
        NewsClient client = buildClientWithMockServer();
        mockServer.expect(requestTo(BASE_URL + "/news?language=ko"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-API-Key", API_KEY))
                .andRespond(withSuccess("""
                        {"news": []}
                        """, MediaType.APPLICATION_JSON));

        client.getNews("ko");

        mockServer.verify();
    }

    @Test
    void language가_en이면_news_language_en_쿼리스트링으로_요청한다() {
        NewsClient client = buildClientWithMockServer();
        mockServer.expect(requestTo(BASE_URL + "/news?language=en"))
                .andRespond(withSuccess("""
                        {"news": []}
                        """, MediaType.APPLICATION_JSON));

        client.getNews("en");

        mockServer.verify();
    }

    @Test
    void language가_null이면_쿼리스트링_없이_news를_요청한다() {
        NewsClient client = buildClientWithMockServer();
        mockServer.expect(requestTo(BASE_URL + "/news"))
                .andRespond(withSuccess("""
                        {"news": []}
                        """, MediaType.APPLICATION_JSON));

        client.getNews(null);

        mockServer.verify();
    }

    @Test
    void 정상_응답은_NewsResponse로_역직렬화된다() {
        NewsClient client = buildClientWithMockServer();
        String responseJson = """
                {
                  "news": [
                    {
                      "title": "외국인 유학생 지원 정책 확대",
                      "threeLineSummary": ["요약1", "요약2", "요약3"],
                      "detailedSummary": "상세 요약입니다",
                      "link": "https://example.com/news/1"
                    }
                  ]
                }
                """;
        mockServer.expect(requestTo(BASE_URL + "/news?language=ko"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        NewsResponse response = client.getNews("ko");

        assertThat(response.news()).hasSize(1);
        var item = response.news().get(0);
        assertThat(item.title()).isEqualTo("외국인 유학생 지원 정책 확대");
        assertThat(item.threeLineSummary()).containsExactly("요약1", "요약2", "요약3");
        assertThat(item.detailedSummary()).isEqualTo("상세 요약입니다");
        assertThat(item.link()).isEqualTo("https://example.com/news/1");
    }

    @Test
    void 서버오류_5xx_응답이면_NewsClientException_SERVER_UNAVAILABLE이_발생한다() {
        NewsClient client = buildClientWithMockServer();
        mockServer.expect(requestTo(BASE_URL + "/news?language=ko"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.getNews("ko"))
                .isInstanceOf(NewsClientException.class)
                .extracting(e -> ((NewsClientException) e).getReason())
                .isEqualTo(NewsClientException.Reason.SERVER_UNAVAILABLE);
    }
}
