package com.example.kbuddy.ai.client;

import com.example.kbuddy.ai.config.AiProperties;
import com.example.kbuddy.ai.dto.AiChatRequest;
import com.example.kbuddy.ai.dto.AiChatResponse;
import com.example.kbuddy.ai.dto.AiRecommendationPriority;
import com.example.kbuddy.ai.dto.AiRecommendationRequest;
import com.example.kbuddy.ai.dto.AiRecommendationResponse;
import com.example.kbuddy.ai.dto.AiRecommendationType;
import com.example.kbuddy.ai.dto.AiTrigger;
import com.example.kbuddy.ai.dto.AiUser;
import com.example.kbuddy.notification.entity.NotificationCategory;
import com.example.kbuddy.user.entity.HousingType;
import com.example.kbuddy.user.entity.PartTimeStatus;
import com.example.kbuddy.user.entity.TopikLevel;
import com.example.kbuddy.user.entity.UserStatus;
import com.example.kbuddy.user.entity.VisaType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiClientTest {

    private static final String BASE_URL = "http://localhost:8000";
    private static final String API_KEY = "test-internal-api-key";
    private static final AiProperties AI_PROPERTIES =
            new AiProperties(BASE_URL, API_KEY, Duration.ofSeconds(10), Duration.ofSeconds(20), Duration.ofSeconds(30));

    private MockRestServiceServer mockServer;
    private ServerSocket rawServerSocket;

    @AfterEach
    void tearDown() throws IOException {
        if (rawServerSocket != null && !rawServerSocket.isClosed()) {
            rawServerSocket.close();
        }
    }

    private AiClient buildAiClientWithMockServer() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        return new AiClient(restClient, restClient, AI_PROPERTIES);
    }

    private AiUser sampleUser() {
        return new AiUser(
                1L,
                "Vietnam",
                2003,
                UserStatus.UNDERGRADUATE,
                "Kyungpook National University",
                LocalDate.of(2026, 2, 20),
                VisaType.D2,
                true,
                LocalDate.of(2027, 2, 19),
                HousingType.DORMITORY,
                true,
                PartTimeStatus.NOT_PLANNED,
                null,
                false,
                TopikLevel.LEVEL_3,
                TopikLevel.LEVEL_5,
                "ko"
        );
    }

    private String minimalRecommendationResponseJson() {
        return """
                {
                  "userId": 1,
                  "summary": "사용자의 현재 상황 요약",
                  "recommendations": []
                }
                """;
    }

    // ---------- Recommendation Request 직렬화 (1, 2, 3) ----------

    @Test
    void recommendations_호출시_user와_trigger가_계약대로_JSON으로_직렬화된다() {
        AiClient aiClient = buildAiClientWithMockServer();
        AiRecommendationRequest request = new AiRecommendationRequest(
                sampleUser(), new AiTrigger("VISA_EXPIRATION", 30));

        String expectedJson = """
                {
                  "user": {
                    "userId": 1,
                    "nationality": "Vietnam",
                    "birthYear": 2003,
                    "userStatus": "UNDERGRADUATE",
                    "schoolName": "Kyungpook National University",
                    "entryDate": "2026-02-20",
                    "visaType": "D2",
                    "hasAlienRegistration": true,
                    "stayExpirationDate": "2027-02-19",
                    "housingType": "DORMITORY",
                    "isParentSupported": true,
                    "partTimeStatus": "NOT_PLANNED",
                    "partTimeStartDate": null,
                    "hasPartTimePermit": false,
                    "currentTopikLevel": "LEVEL_3",
                    "targetTopikLevel": "LEVEL_5",
                    "language": "ko"
                  },
                  "trigger": {
                    "type": "VISA_EXPIRATION",
                    "daysRemaining": 30
                  }
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/recommend"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(expectedJson, JsonCompareMode.STRICT))
                .andRespond(withSuccess(minimalRecommendationResponseJson(), MediaType.APPLICATION_JSON));

        aiClient.recommendations(request);

        mockServer.verify();
    }

    @Test
    void partTimeStatus가_SEARCHING이면_커스텀_변환없이_Java_enum_name_그대로_직렬화된다() {
        AiClient aiClient = buildAiClientWithMockServer();
        AiUser userSearchingPartTime = new AiUser(
                1L, "Vietnam", 2003, UserStatus.UNDERGRADUATE, "성공회대학교",
                LocalDate.of(2026, 6, 1), VisaType.D2, true, LocalDate.of(2026, 12, 1),
                HousingType.DORMITORY, true, PartTimeStatus.SEARCHING, null, false,
                TopikLevel.LEVEL_3, TopikLevel.LEVEL_4, "ko"
        );
        AiRecommendationRequest request = new AiRecommendationRequest(
                userSearchingPartTime, new AiTrigger("VISA_EXPIRATION", 30));

        mockServer.expect(requestTo(BASE_URL + "/recommend"))
                .andExpect(content().json("""
                        {"user": {"partTimeStatus": "SEARCHING", "language": "ko"}}
                        """, JsonCompareMode.LENIENT))
                .andRespond(withSuccess(minimalRecommendationResponseJson(), MediaType.APPLICATION_JSON));

        aiClient.recommendations(request);

        mockServer.verify();
    }

    // ---------- Recommendation Response 역직렬화 (4, 5, 6, 7, 8, 9) ----------

    @Test
    void 정상_Response는_AiRecommendationResponse로_역직렬화되고_summary와_detail이_계약대로_매핑된다() {
        AiClient aiClient = buildAiClientWithMockServer();
        String responseJson = """
                {
                  "userId": 1,
                  "summary": "사용자의 현재 상황 요약",
                  "recommendations": [
                    {
                      "type": "LAW",
                      "priority": "HIGH",
                      "title": "체류기간 연장 신청을 확인하세요",
                      "reason": "체류기간 만료일이 가까워지고 있습니다.",
                      "detail": {
                        "category": "VISA",
                        "lawName": "출입국관리법",
                        "sourceName": "국가법령정보센터"
                      }
                    }
                  ]
                }
                """;
        mockServer.expect(requestTo(BASE_URL + "/recommend"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        AiRecommendationResponse response = aiClient.recommendations(
                new AiRecommendationRequest(sampleUser(), new AiTrigger("VISA_EXPIRATION", 30)));

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.summary()).isEqualTo("사용자의 현재 상황 요약");
        assertThat(response.recommendations()).hasSize(1);
        var recommendation = response.recommendations().get(0);
        assertThat(recommendation.type()).isEqualTo(AiRecommendationType.LAW);
        assertThat(recommendation.priority()).isEqualTo(AiRecommendationPriority.HIGH);
        assertThat(recommendation.title()).isEqualTo("체류기간 연장 신청을 확인하세요");
        assertThat(recommendation.reason()).isEqualTo("체류기간 만료일이 가까워지고 있습니다.");
        assertThat(recommendation.detail())
                .containsEntry("category", "VISA")
                .containsEntry("lawName", "출입국관리법")
                .containsEntry("sourceName", "국가법령정보센터");
    }

    @Test
    void recommendations_배열에_여러_항목이_있으면_모두_역직렬화되고_HIGH_MEDIUM_LOW가_정상_처리된다() {
        AiClient aiClient = buildAiClientWithMockServer();
        String responseJson = """
                {
                  "userId": 1,
                  "summary": "요약",
                  "recommendations": [
                    {
                      "type": "LAW",
                      "priority": "HIGH",
                      "title": "법령 안내",
                      "reason": "이유1",
                      "detail": {"key": "value1"}
                    },
                    {
                      "type": "UNIVERSITY",
                      "priority": "LOW",
                      "title": "대학 안내",
                      "reason": "이유2",
                      "detail": {"key": "value2"}
                    }
                  ]
                }
                """;
        mockServer.expect(requestTo(BASE_URL + "/recommend"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        AiRecommendationResponse response = aiClient.recommendations(
                new AiRecommendationRequest(sampleUser(), new AiTrigger("VISA_EXPIRATION", 30)));

        assertThat(response.recommendations()).hasSize(2);
        assertThat(response.recommendations().get(0).priority()).isEqualTo(AiRecommendationPriority.HIGH);
        assertThat(response.recommendations().get(0).type()).isEqualTo(AiRecommendationType.LAW);
        assertThat(response.recommendations().get(1).priority()).isEqualTo(AiRecommendationPriority.LOW);
        assertThat(response.recommendations().get(1).type()).isEqualTo(AiRecommendationType.UNIVERSITY);
    }

    @Test
    void detail이_빈_객체면_빈_Map으로_역직렬화된다() {
        AiClient aiClient = buildAiClientWithMockServer();
        String responseJson = """
                {
                  "userId": 1,
                  "summary": "요약",
                  "recommendations": [
                    {
                      "type": "UNIVERSITY",
                      "priority": "MEDIUM",
                      "title": "제목",
                      "reason": "이유",
                      "detail": {}
                    }
                  ]
                }
                """;
        mockServer.expect(requestTo(BASE_URL + "/recommend"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        AiRecommendationResponse response = aiClient.recommendations(
                new AiRecommendationRequest(sampleUser(), new AiTrigger("VISA_EXPIRATION", 30)));

        assertThat(response.recommendations().get(0).detail()).isEqualTo(Map.of());
    }

    @Test
    void 허용되지_않은_type값이_오면_역직렬화에_실패하고_AiClientException_RESPONSE_INVALID가_발생한다() {
        AiClient aiClient = buildAiClientWithMockServer();
        String responseJson = """
                {
                  "userId": 1,
                  "summary": "요약",
                  "recommendations": [
                    {
                      "type": "UNKNOWN_TYPE",
                      "priority": "MEDIUM",
                      "title": "제목",
                      "reason": "이유",
                      "detail": {}
                    }
                  ]
                }
                """;
        mockServer.expect(requestTo(BASE_URL + "/recommend"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> aiClient.recommendations(
                new AiRecommendationRequest(sampleUser(), new AiTrigger("VISA_EXPIRATION", 30))))
                .isInstanceOf(AiClientException.class)
                .extracting(ex -> ((AiClientException) ex).getReason())
                .isEqualTo(AiClientException.Reason.RESPONSE_INVALID);
    }

    // ---------- Chat Request/Response (10, 11, 12, 13, 14, 15) ----------

    @Test
    void chat_호출시_user와_message가_계약대로_JSON으로_직렬화된다() {
        AiClient aiClient = buildAiClientWithMockServer();
        AiChatRequest request = new AiChatRequest(sampleUser(), "D-2 비자인데 아르바이트를 시작하려면 어떻게 해야 해?");

        String expectedJson = """
                {
                  "user": {
                    "userId": 1,
                    "nationality": "Vietnam",
                    "birthYear": 2003,
                    "userStatus": "UNDERGRADUATE",
                    "schoolName": "Kyungpook National University",
                    "entryDate": "2026-02-20",
                    "visaType": "D2",
                    "hasAlienRegistration": true,
                    "stayExpirationDate": "2027-02-19",
                    "housingType": "DORMITORY",
                    "isParentSupported": true,
                    "partTimeStatus": "NOT_PLANNED",
                    "partTimeStartDate": null,
                    "hasPartTimePermit": false,
                    "currentTopikLevel": "LEVEL_3",
                    "targetTopikLevel": "LEVEL_5",
                    "language": "ko"
                  },
                  "message": "D-2 비자인데 아르바이트를 시작하려면 어떻게 해야 해?"
                }
                """;

        mockServer.expect(requestTo(BASE_URL + "/ai/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(expectedJson, JsonCompareMode.STRICT))
                .andRespond(withSuccess("""
                        {"answer": "답변", "category": "PART_TIME", "suggestedQuestions": [], "sources": []}
                        """, MediaType.APPLICATION_JSON));

        aiClient.chat(request);

        mockServer.verify();
    }

    @Test
    void 정상_Chat_Response는_AiChatResponse로_역직렬화된다() {
        AiClient aiClient = buildAiClientWithMockServer();
        String responseJson = """
                {
                  "answer": "AI 답변 내용",
                  "category": "PART_TIME",
                  "suggestedQuestions": [
                    "아르바이트 허가가 필요한가요?",
                    "근무 가능 시간은 어떻게 되나요?"
                  ],
                  "sources": [
                    {
                      "dataset": "law",
                      "title": "유학생 아르바이트 허가",
                      "sourceName": "하이코리아, 국가법령정보센터",
                      "article": "관련 조항"
                    }
                  ]
                }
                """;
        mockServer.expect(requestTo(BASE_URL + "/ai/chat"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        AiChatResponse response = aiClient.chat(
                new AiChatRequest(sampleUser(), "D-2 비자인데 아르바이트를 시작하려면 어떻게 해야 해?"));

        assertThat(response.answer()).isEqualTo("AI 답변 내용");
        assertThat(response.category()).isEqualTo(NotificationCategory.PART_TIME);
        assertThat(response.suggestedQuestions()).containsExactly(
                "아르바이트 허가가 필요한가요?", "근무 가능 시간은 어떻게 되나요?");
        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().get(0)).containsEntry("dataset", "law");
    }

    @Test
    void Chat_Response의_sources가_빈_배열이면_빈_리스트로_역직렬화된다() {
        AiClient aiClient = buildAiClientWithMockServer();
        String responseJson = """
                {"answer": "답변", "category": "SUPPORT", "suggestedQuestions": [], "sources": []}
                """;
        mockServer.expect(requestTo(BASE_URL + "/ai/chat"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        AiChatResponse response = aiClient.chat(
                new AiChatRequest(sampleUser(), "질문"));

        assertThat(response.sources()).isEmpty();
        assertThat(response.suggestedQuestions()).isEmpty();
    }

    // ---------- HTTP Header / Endpoint (16, 17, 18, 19) ----------

    @Test
    void 모든_요청에_Content_Type과_X_Internal_API_Key_헤더가_전달된다() {
        AiClient aiClient = buildAiClientWithMockServer();

        mockServer.expect(requestTo(BASE_URL + "/recommend"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header("X-Internal-API-Key", API_KEY))
                .andRespond(withSuccess(minimalRecommendationResponseJson(), MediaType.APPLICATION_JSON));

        aiClient.recommendations(new AiRecommendationRequest(sampleUser(), new AiTrigger("VISA_EXPIRATION", 30)));

        mockServer.verify();
    }

    // ---------- Retry: 5xx (20) ----------

    @Test
    void 서버오류_5xx_응답이면_한_번_재시도하고_두번째_요청이_성공하면_정상_응답을_반환한다() {
        AiClient aiClient = buildAiClientWithMockServer();

        mockServer.expect(requestTo(BASE_URL + "/recommend"))
                .andRespond(withServerError());
        mockServer.expect(requestTo(BASE_URL + "/recommend"))
                .andRespond(withSuccess(minimalRecommendationResponseJson(), MediaType.APPLICATION_JSON));

        AiRecommendationResponse response = aiClient.recommendations(
                new AiRecommendationRequest(sampleUser(), new AiTrigger("VISA_EXPIRATION", 30)));

        assertThat(response.userId()).isEqualTo(1L);
        mockServer.verify();
    }

    @Test
    void 서버오류_5xx가_재시도_이후에도_계속되면_AiClientException_SERVER_UNAVAILABLE이_발생하고_요청은_정확히_2번만_일어난다() {
        AiClient aiClient = buildAiClientWithMockServer();

        mockServer.expect(requestTo(BASE_URL + "/recommend"))
                .andRespond(withServerError());
        mockServer.expect(requestTo(BASE_URL + "/recommend"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> aiClient.recommendations(
                new AiRecommendationRequest(sampleUser(), new AiTrigger("VISA_EXPIRATION", 30))))
                .isInstanceOf(AiClientException.class)
                .extracting(ex -> ((AiClientException) ex).getReason())
                .isEqualTo(AiClientException.Reason.SERVER_UNAVAILABLE);

        mockServer.verify();
    }

    // ---------- No retry: 4xx (22) ----------

    @Test
    void 클라이언트오류_4xx_응답이면_재시도하지_않고_HttpClientErrorException을_그대로_던진다() {
        AiClient aiClient = buildAiClientWithMockServer();

        mockServer.expect(requestTo(BASE_URL + "/recommend"))
                .andRespond(withStatus(HttpStatus.valueOf(422))
                        .body("{\"detail\": \"invalid request\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> aiClient.recommendations(
                new AiRecommendationRequest(sampleUser(), new AiTrigger("VISA_EXPIRATION", 30))))
                .isInstanceOf(HttpClientErrorException.class);

        mockServer.verify();
    }

    // ---------- Retry: timeout (21) ----------
    // com.sun.net.httpserver.HttpServer / MockRestServiceServer는 사용하지 않는다.
    // 이 세션 환경에서는 java.nio.channels.Selector.open()이 Windows Unix-Domain-Socket
    // wakeup pipe를 열지 못해(Gradle 데몬과 동일한 원인) HttpServer 기반 접근이 실패하므로,
    // NIO Selector를 쓰지 않는 순수 블로킹 I/O(java.net.ServerSocket/Socket)로 실제 read timeout을 재현한다.

    @Test
    void read_timeout_발생시_한_번_재시도하고_두번째_요청이_성공하면_정상_응답을_반환한다() throws IOException {
        AtomicInteger requestCount = new AtomicInteger(0);
        int port = startRawServer(requestCount, true, false);

        RestClient restClient = buildTimeoutTestRestClient(port, 300);
        AiClient aiClient = new AiClient(restClient, restClient, AI_PROPERTIES);

        AiRecommendationResponse response = aiClient.recommendations(
                new AiRecommendationRequest(sampleUser(), new AiTrigger("VISA_EXPIRATION", 30)));

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(requestCount.get()).isEqualTo(2);
    }

    @Test
    void read_timeout이_재시도_이후에도_계속되면_AiClientException_REQUEST_TIMEOUT이_발생하고_요청은_정확히_2번만_일어난다() throws IOException {
        AtomicInteger requestCount = new AtomicInteger(0);
        int port = startRawServer(requestCount, true, true);

        RestClient restClient = buildTimeoutTestRestClient(port, 300);
        AiClient aiClient = new AiClient(restClient, restClient, AI_PROPERTIES);

        assertThatThrownBy(() -> aiClient.recommendations(
                new AiRecommendationRequest(sampleUser(), new AiTrigger("VISA_EXPIRATION", 30))))
                .isInstanceOf(AiClientException.class)
                .extracting(ex -> ((AiClientException) ex).getReason())
                .isEqualTo(AiClientException.Reason.REQUEST_TIMEOUT);

        assertThat(requestCount.get()).isEqualTo(2);
    }

    /**
     * 블로킹 I/O(ServerSocket)만으로 최소한의 raw HTTP 서버를 띄운다.
     * hang=true인 연결은 클라이언트의 read timeout보다 훨씬 오래 응답을 보내지 않고
     * 대기만 하다가(따라서 절대 write하지 않는다) 조용히 닫는다.
     * hang=false인 연결은 즉시 고정된 JSON 200 응답을 돌려준다.
     */
    private int startRawServer(AtomicInteger requestCount, boolean firstRequestHangs, boolean secondRequestHangs) throws IOException {
        rawServerSocket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        int port = rawServerSocket.getLocalPort();

        Thread acceptThread = new Thread(() -> {
            try {
                while (!rawServerSocket.isClosed()) {
                    Socket socket = rawServerSocket.accept();
                    int count = requestCount.incrementAndGet();
                    boolean hang = count == 1 ? firstRequestHangs : secondRequestHangs;
                    new Thread(() -> handleRawRequest(socket, hang)).start();
                }
            } catch (IOException ignored) {
                // 테스트 종료 시 rawServerSocket.close()로 인해 발생하는 정상적인 accept 중단
            }
        });
        acceptThread.setDaemon(true);
        acceptThread.start();

        return port;
    }

    private void handleRawRequest(Socket socket, boolean hang) {
        try {
            if (hang) {
                Thread.sleep(10_000);
                return;
            }
            byte[] bodyBytes = minimalRecommendationResponseJson().getBytes(StandardCharsets.UTF_8);
            String headers = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: application/json\r\n"
                    + "Content-Length: " + bodyBytes.length + "\r\n"
                    + "Connection: close\r\n"
                    + "\r\n";
            OutputStream out = socket.getOutputStream();
            out.write(headers.getBytes(StandardCharsets.US_ASCII));
            out.write(bodyBytes);
            out.flush();
            // Windows 루프백 소켓은 write 직후 바로 close()하면 클라이언트가 다 읽기 전에
            // FIN 대신 RST를 보낼 수 있다(널리 알려진 현상). 클라이언트가 응답을 온전히
            // 읽을 시간을 준 뒤 닫아 테스트에서 불필요한 connection reset을 방지한다.
            Thread.sleep(200);
        } catch (IOException | InterruptedException ignored) {
            // 테스트 목적의 최소 서버이므로 실패는 클라이언트 측 타임아웃으로 자연히 드러난다.
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private RestClient buildTimeoutTestRestClient(int port, int readTimeoutMillis) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMillis));
        return RestClient.builder()
                .baseUrl("http://127.0.0.1:" + port)
                .requestFactory(requestFactory)
                .build();
    }
}
