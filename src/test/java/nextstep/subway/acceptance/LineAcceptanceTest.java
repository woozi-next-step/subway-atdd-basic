package nextstep.subway.acceptance;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("지하철 노선 관리 기능")
class LineAcceptanceTest extends AcceptanceTest {
    /**
     * When 지하철 노선 생성을 요청 하면
     * Then 지하철 노선 생성이 성공한다.
     */
    @DisplayName("지하철 노선 생성")
    @Test
    void createLine() {
        // given
        final String 신분당선 = "신분당선";
        final String 빨강색 = "bg-red-600";

        // when
        ExtractableResponse<Response> response = 정상적인_지하철_노선_생성을_요청한다(신분당선, 빨강색);

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value()),
                () -> assertThat(response.header("Location")).isNotBlank(),
                () -> assertThat(response.header("Date")).isNotBlank(),
                () -> assertThat(response.contentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE),
                () -> assertThat(response.body().jsonPath().get("id").equals(1)),
                () -> assertThat(response.body().jsonPath().get("name").equals(신분당선)),
                () -> assertThat(response.body().jsonPath().get("color").equals(빨강색))
                // 시간과 관련된 테스트는 어떻게 해야할지 궁금합니다!
        );
    }

    /**
     * Given 지하철 노선 생성을 요청 하고
     * When 같은 이름으로 지하철 노선 생성을 요청 하면
     * Then 지하철 노선 생성이 실패한다.
     */
    @DisplayName("지하철 노선 이름 중복")
    @Test
    void createDuplicateLineName() {
        // given
        final String 신분당선 = "신분당선";
        정상적인_지하철_노선_생성을_요청한다(신분당선, "bg-red-600");

        // when
        final ExtractableResponse<Response> response = 정상적인_지하철_노선_생성을_요청한다(신분당선, "bg-green-600");

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT.value()),
                () -> assertThat(response.body().jsonPath().get("message").equals("duplicate line name occurred"))
        );
    }

    /**
     * Given 지하철 노선 생성을 요청 하고
     * Given 새로운 지하철 노선 생성을 요청 하고
     * When 지하철 노선 목록 조회를 요청 하면
     * Then 두 노선이 포함된 지하철 노선 목록을 응답받는다
     */
    @DisplayName("지하철 노선 목록 조회")
    @Test
    void getLines() {
        // given
        final String 신분당선 = "신분당선";
        final String 빨강색 = "bg-red-600";
        정상적인_지하철_노선_생성을_요청한다(신분당선, 빨강색);

        final String 이호선 = "2호선";
        final String 초록색 = "bg-green-600";
        정상적인_지하철_노선_생성을_요청한다(이호선, 초록색);

        // when
        final ExtractableResponse<Response> response = RestAssured.given().log().all()
                .accept(ContentType.JSON)
                .header(HttpHeaders.HOST, "localhost:" + port)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get("/lines")
                .then().log().all()
                .extract();

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(response.contentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE),
                () -> assertThat(response.header("Date")).isNotBlank(),
                () -> assertThat(response.jsonPath().getList("id")).contains(1, 2),
                () -> assertThat(response.jsonPath().getList("name")).contains(신분당선, 이호선),
                () -> assertThat(response.jsonPath().getList("color")).contains(빨강색, 초록색)
        );
    }

    /**
     * Given 지하철 노선 생성을 요청 하고
     * When 생성한 지하철 노선 조회를 요청 하면
     * Then 생성한 지하철 노선을 응답받는다
     */
    @DisplayName("지하철 노선 조회")
    @Test
    void getLine() {
        // given
        final String 신분당선 = "신분당선";
        final String 빨강색 = "bg-red-600";
        final ExtractableResponse<Response> saveResponse = 정상적인_지하철_노선_생성을_요청한다(신분당선, 빨강색);
        final Long lineId = Long.valueOf(saveResponse.body().jsonPath().get("id").toString());

        // when
        final ExtractableResponse<Response> response = RestAssured.given().log().all()
                .accept(ContentType.JSON)
                .header(HttpHeaders.HOST, "localhost:" + port)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .get(saveResponse.header("Location"))
                .then().log().all()
                .extract();

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(response.contentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE),
                () -> assertThat(response.header("Date")).isNotBlank(),
                () -> assertThat(response.body().jsonPath().get("id").equals(lineId)),
                () -> assertThat(response.body().jsonPath().get("name").equals(신분당선)),
                () -> assertThat(response.body().jsonPath().get("color").equals(빨강색))
        );
    }

    /**
     * Given 지하철 노선 생성을 요청 하고
     * When 지하철 노선의 정보 수정을 요청 하면
     * Then 지하철 노선의 정보 수정은 성공한다.
     */
    @DisplayName("지하철 노선 수정")
    @Test
    void updateLine() {
        // given
        final ExtractableResponse<Response> saveResponse = 정상적인_지하철_노선_생성을_요청한다("신분당선", "bg-red-600");

        final Map<String, String> params = new HashMap<>();
        final String updatedName = "구분당선";
        final String updatedColor = "bg-blue-600";
        params.put("name", updatedName);
        params.put("color", updatedColor);

        // when
        final ExtractableResponse<Response> updateResponse = RestAssured.given().log().all()
                .accept(ContentType.ANY)
                .header(HttpHeaders.HOST, "localhost:" + port)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(params)
                .when()
                .put(saveResponse.header("Location")) // 모든 데이터를 변경하고 있어서 put 으로 했습니다.
                .then().log().all()
                .extract();

        // then
        assertAll(
                () -> assertThat(updateResponse.statusCode()).isEqualTo(HttpStatus.OK.value()),
                () -> assertThat(updateResponse.header("Date")).isNotBlank()
        );
    }

    /**
     * Given 지하철 노선 생성을 요청 하고
     * When 생성한 지하철 노선 삭제를 요청 하면
     * Then 생성한 지하철 노선 삭제가 성공한다.
     */
    @DisplayName("지하철 노선 삭제")
    @Test
    void deleteLine() {
        // given
        final ExtractableResponse<Response> saveResponse = 정상적인_지하철_노선_생성을_요청한다("신분당선", "bg-red-600");

        // when
        final ExtractableResponse<Response> response = RestAssured.given().log().all()
                .accept(ContentType.ANY)
                .header(HttpHeaders.HOST, "localhost:" + port)
                .when()
                .delete(saveResponse.header("Location"))
                .then().log().all()
                .extract();

        // then
        assertAll(
                () -> assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value()),
                () -> assertThat(response.header("Date")).isNotBlank()
        );
    }

    private ExtractableResponse<Response> 정상적인_지하철_노선_생성을_요청한다(final String name, final String color) {
        final Map<String, String> params = 정상적인_지하철_노선_생성_데이터를_만든다(name, color);
        ExtractableResponse<Response> response = RestAssured.given().log().all()
                .body(params)
                .accept(ContentType.ANY)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when()
                .post("/lines")
                .then().log().all()
                .extract();
        return response;
    }

    private Map<String, String> 정상적인_지하철_노선_생성_데이터를_만든다(final String name, final String color) {
        final Map<String, String> params = new HashMap<>();
        params.put("name", name);
        params.put("color", color);
        return params;
    }
}
