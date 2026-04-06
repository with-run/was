package kr.withrun.was.domain.navigation.dto.internal.bundle;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NavigationBundleManeuverType {
    STRAIGHT("STRAIGHT", "직진"),
    SLIGHT_LEFT("SLIGHT_LEFT", "왼쪽으로 살짝 이동"),
    SLIGHT_RIGHT("SLIGHT_RIGHT", "오른쪽으로 살짝 이동"),
    LEFT("LEFT", "좌회전"),
    RIGHT("RIGHT", "우회전"),
    SHARP_LEFT("SHARP_LEFT", "급좌회전"),
    SHARP_RIGHT("SHARP_RIGHT", "급우회전"),
    UTURN("UTURN", "유턴"),
    ARRIVAL("ARRIVAL", "목적지에 도착");

    private final String data;
    private final String label;
}
