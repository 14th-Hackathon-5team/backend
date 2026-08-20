package com.example.kbuddy.job.client;

import com.example.kbuddy.job.config.SeoulJobProperties;
import com.example.kbuddy.job.dto.SeoulJobApiEnvelope;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SeoulJobClientTest {

    private static final String BASE_URL = "http://localhost:8088";
    private static final String API_KEY = "test-api-key";
    private static final SeoulJobProperties PROPERTIES =
            new SeoulJobProperties(BASE_URL, API_KEY, Duration.ofSeconds(10));

    private MockRestServiceServer mockServer;

    private SeoulJobClient buildClientWithMockServer() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        return new SeoulJobClient(builder.build(), PROPERTIES);
    }

    @Test
    void search는_apiKey_startIndex_endIndex를_경로에_담아_요청한다() {
        SeoulJobClient client = buildClientWithMockServer();
        mockServer.expect(requestTo(BASE_URL + "/" + API_KEY + "/json/GlobalJobSearch/1/20/"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"GlobalJobSearch": {"list_total_count": 0, "row": []}}
                        """, MediaType.APPLICATION_JSON));

        client.search(1, 20);

        mockServer.verify();
    }

    @Test
    void 정상_응답은_SeoulJobApiEnvelope로_역직렬화된다() {
        SeoulJobClient client = buildClientWithMockServer();
        String responseJson = """
                {
                  "GlobalJobSearch": {
                    "list_total_count": 1,
                    "row": [
                      {
                        "TITL_NM": "카페 아르바이트",
                        "CONT": "설명입니다",
                        "WRIT_NM": "홍길동",
                        "LANG_GB": "한국어",
                        "REG_DT": "2026-08-01",
                        "UPD_DT": "2026-08-02"
                      }
                    ]
                  }
                }
                """;
        mockServer.expect(requestTo(BASE_URL + "/" + API_KEY + "/json/GlobalJobSearch/1/20/"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        SeoulJobApiEnvelope envelope = client.search(1, 20);

        assertThat(envelope.globalJobSearch().listTotalCount()).isEqualTo(1);
        assertThat(envelope.globalJobSearch().row()).hasSize(1);
        var item = envelope.globalJobSearch().row().get(0);
        assertThat(item.titlNm()).isEqualTo("카페 아르바이트");
        assertThat(item.cont()).isEqualTo("설명입니다");
        assertThat(item.writNm()).isEqualTo("홍길동");
        assertThat(item.langGb()).isEqualTo("한국어");
        assertThat(item.regDt()).isEqualTo("2026-08-01");
        assertThat(item.updDt()).isEqualTo("2026-08-02");
    }

    @Test
    void 서버오류_5xx_응답이면_SeoulJobClientException_SERVER_UNAVAILABLE이_발생한다() {
        SeoulJobClient client = buildClientWithMockServer();
        mockServer.expect(requestTo(BASE_URL + "/" + API_KEY + "/json/GlobalJobSearch/1/20/"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.search(1, 20))
                .isInstanceOf(SeoulJobClientException.class)
                .extracting(e -> ((SeoulJobClientException) e).getReason())
                .isEqualTo(SeoulJobClientException.Reason.SERVER_UNAVAILABLE);
    }

    @Test
    void 응답_형식이_계약과_다르면_SeoulJobClientException_RESPONSE_INVALID가_발생한다() {
        SeoulJobClient client = buildClientWithMockServer();
        mockServer.expect(requestTo(BASE_URL + "/" + API_KEY + "/json/GlobalJobSearch/1/20/"))
                .andRespond(withSuccess("이건 JSON이 아닙니다", MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> client.search(1, 20))
                .isInstanceOf(SeoulJobClientException.class)
                .extracting(e -> ((SeoulJobClientException) e).getReason())
                .isEqualTo(SeoulJobClientException.Reason.RESPONSE_INVALID);
    }
}
