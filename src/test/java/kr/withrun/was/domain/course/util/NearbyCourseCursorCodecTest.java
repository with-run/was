package kr.withrun.was.domain.course.util;

import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("주변 코스 커서 코덱")
class NearbyCourseCursorCodecTest {

    @DisplayName("같은 입력은 항상 같은 값으로 인코딩한다")
    @Test
    void encodesDeterministicallyForSameInput() {
        String first = NearbyCourseCursorCodec.encode(0.123456, 123, 456L);
        String second = NearbyCourseCursorCodec.encode(0.123456, 123, 456L);

        assertThat(first).isEqualTo(second);
    }

    @DisplayName("인코딩한 커서를 원래 키 값으로 디코딩한다")
    @Test
    void decodesEncodedCursorBackToOriginalKeys() {
        String cursor = NearbyCourseCursorCodec.encode(0.321, 321, 654L);

        NearbyCourseCursorCodec.CursorPayload payload = NearbyCourseCursorCodec.decode(cursor);

        assertThat(payload.lastScaledScore()).isEqualTo(NearbyCourseCursorCodec.scaleScore(0.321));
        assertThat(payload.lastDistanceFromUserM()).isEqualTo(321);
        assertThat(payload.lastCourseId()).isEqualTo(654L);
    }

    @DisplayName("유효하지 않은 Base64 입력이면 잘못된 커서 예외를 던진다")
    @Test
    void throwsInvalidCursorWhenBase64InputIsInvalid() {
        assertThatThrownBy(() -> NearbyCourseCursorCodec.decode("%%%"))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_CURSOR);
    }

    @DisplayName("디코딩된 페이로드 형식이 잘못되면 잘못된 커서 예외를 던진다")
    @Test
    void throwsInvalidCursorWhenDecodedPayloadIsMalformed() {
        assertThatThrownBy(() -> NearbyCourseCursorCodec.decode("MTIz"))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_CURSOR);
    }
}
