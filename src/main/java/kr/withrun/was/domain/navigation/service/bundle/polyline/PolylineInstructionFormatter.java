package kr.withrun.was.domain.navigation.service.bundle.polyline;

import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuverType;
import org.springframework.stereotype.Component;

@Component
public class PolylineInstructionFormatter {

    public String format(NavigationBundleManeuverType maneuverType, String language) {
        if (language != null && language.startsWith("ko")) {
            return formatKorean(maneuverType);
        }

        return switch (maneuverType) {
            case SLIGHT_LEFT -> "Slight left";
            case SLIGHT_RIGHT -> "Slight right";
            case LEFT -> "Turn left";
            case RIGHT -> "Turn right";
            case SHARP_LEFT -> "Sharp left";
            case SHARP_RIGHT -> "Sharp right";
            case UTURN -> "Make a U-turn";
            case ARRIVAL -> "Arrive at destination";
            case STRAIGHT -> "Continue straight";
        };
    }

    private String formatKorean(NavigationBundleManeuverType maneuverType) {
        return switch (maneuverType) {
            case SLIGHT_LEFT -> "왼쪽으로 살짝 이동";
            case SLIGHT_RIGHT -> "오른쪽으로 살짝 이동";
            case LEFT -> "좌회전";
            case RIGHT -> "우회전";
            case SHARP_LEFT -> "급좌회전";
            case SHARP_RIGHT -> "급우회전";
            case UTURN -> "유턴";
            case ARRIVAL -> "목적지에 도착";
            case STRAIGHT -> "직진";
        };
    }
}
