package kr.withrun.was.domain.course.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("좌표 값 객체")
class CoordinatesTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DisplayName("객체 배열 형태 좌표를 Coordinates로 역직렬화한다")
    @Test
    void deserializesObjectArray() throws Exception {
        Coordinates coordinates = objectMapper.readValue("""
                [
                  { "latitude": 35.832645, "longitude": 128.69337, "elevationM": 0.0 },
                  { "latitude": 35.833784, "longitude": 128.693551, "elevationM": -9.0 }
                ]
                """, Coordinates.class);

        assertThat(coordinates).isEqualTo(new Coordinates(List.of(
                point(35.832645, 128.69337, 0.0),
                point(35.833784, 128.693551, -9.0)
        )));
    }

    @DisplayName("Coordinates는 객체 배열 형태로 직렬화한다")
    @Test
    void serializesToObjectArray() throws Exception {
        String json = objectMapper.writeValueAsString(new Coordinates(List.of(
                point(35.832645, 128.69337, 0.0),
                point(35.833784, 128.693551, -9.0)
        )));

        assertThat(json).isEqualTo(
                "[{\"latitude\":35.832645,\"longitude\":128.69337,\"elevationM\":0.0},{\"latitude\":35.833784,\"longitude\":128.693551,\"elevationM\":-9.0}]"
        );
    }

    private GeoPoint point(double latitude, double longitude, double elevationM) {
        return new GeoPoint(latitude, longitude, elevationM);
    }
}
