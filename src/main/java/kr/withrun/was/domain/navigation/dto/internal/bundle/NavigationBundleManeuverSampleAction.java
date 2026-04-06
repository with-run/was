package kr.withrun.was.domain.navigation.dto.internal.bundle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum NavigationBundleManeuverSampleAction {
    STRAIGHT("STRAIGHT", "직진"),
    RIGHT("RIGHT", "우회전"),
    LEFT("LEFT", "좌회전"),
    UTURN("UTURN", "유턴"),
    ARRIVAL("ARRIVAL", "도착");

    private final String data;
    private final String label;

    NavigationBundleManeuverSampleAction(String data, String label) {
        this.data = data;
        this.label = label;
    }

    @JsonValue
    public String jsonValue() {
        return label;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static NavigationBundleManeuverSampleAction fromJsonValue(String jsonValue) {
        for (NavigationBundleManeuverSampleAction value : values()) {
            if (value.label.equals(jsonValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown NavigationBundleManeuverSampleAction: " + jsonValue);
    }
}
