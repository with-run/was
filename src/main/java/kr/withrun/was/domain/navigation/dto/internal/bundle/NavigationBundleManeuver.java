package kr.withrun.was.domain.navigation.dto.internal.bundle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 클라이언트용 번들에 저장되는 maneuver 정규화 결과입니다.
 */
public record NavigationBundleManeuver(
        @JsonProperty("type") NavigationBundleManeuverType maneuverType,
        NavigationBundleTurnStrength strength,
        NavigationBundleManeuverSampleAction sampleAction,
        String instruction,
        int shapeIndex,
        double cumulativeDistanceMeters,
        Integer incomingAnchorShapeIndex,
        Integer outgoingAnchorShapeIndex,
        Integer bearingBeforeDegrees,
        Integer bearingAfterDegrees,
        Integer deltaDegrees,
        @JsonIgnore String verbalTransitionAlertInstruction,
        @JsonIgnore String verbalPreTransitionInstruction,
        @JsonIgnore String verbalPostTransitionInstruction,
        @JsonIgnore Boolean verbalMultiCue,
        @JsonIgnore String verbalSuccinctTransitionInstruction,
        @JsonIgnore Integer type,
        @JsonIgnore Integer bearingBefore,
        @JsonIgnore Integer bearingAfter,
        @JsonIgnore List<NavigationBundleVoiceInstruction> voiceInstructions,
        @JsonIgnore int beginShapeIndex,
        @JsonIgnore int endShapeIndex,
        @JsonIgnore double distanceMeters,
        @JsonIgnore double startDistanceMeters,
        @JsonIgnore double endDistanceMeters
) {

    public NavigationBundleManeuver {
        voiceInstructions = voiceInstructions == null ? List.of() : List.copyOf(voiceInstructions);
    }

    public NavigationBundleManeuver(
            NavigationBundleManeuverType maneuverType,
            NavigationBundleTurnStrength strength,
            NavigationBundleManeuverSampleAction sampleAction,
            String instruction,
            int shapeIndex,
            double cumulativeDistanceMeters,
            Integer incomingAnchorShapeIndex,
            Integer outgoingAnchorShapeIndex,
            Integer bearingBeforeDegrees,
            Integer bearingAfterDegrees,
            Integer deltaDegrees
    ) {
        this(
                maneuverType,
                strength,
                sampleAction,
                instruction,
                shapeIndex,
                cumulativeDistanceMeters,
                incomingAnchorShapeIndex,
                outgoingAnchorShapeIndex,
                bearingBeforeDegrees,
                bearingAfterDegrees,
                deltaDegrees,
                null,
                null,
                null,
                null,
                null,
                null,
                bearingBeforeDegrees,
                bearingAfterDegrees,
                List.of(),
                shapeIndex,
                shapeIndex,
                0d,
                cumulativeDistanceMeters,
                cumulativeDistanceMeters
        );
    }

    public NavigationBundleManeuver(
            String instruction,
            String verbalTransitionAlertInstruction,
            String verbalPreTransitionInstruction,
            String verbalPostTransitionInstruction,
            Boolean verbalMultiCue,
            String verbalSuccinctTransitionInstruction,
            Integer type,
            NavigationBundleManeuverSampleAction sampleAction,
            Integer bearingBefore,
            Integer bearingAfter,
            List<NavigationBundleVoiceInstruction> voiceInstructions,
            int beginShapeIndex,
            int endShapeIndex,
            double distanceMeters,
            double cumulativeDistanceMeters,
            double startDistanceMeters,
            double endDistanceMeters
    ) {
        this(
                inferManeuverType(sampleAction),
                inferStrength(sampleAction),
                sampleAction,
                instruction,
                endShapeIndex,
                cumulativeDistanceMeters,
                beginShapeIndex,
                endShapeIndex,
                bearingBefore,
                bearingAfter,
                calculateDeltaDegrees(bearingBefore, bearingAfter),
                verbalTransitionAlertInstruction,
                verbalPreTransitionInstruction,
                verbalPostTransitionInstruction,
                verbalMultiCue,
                verbalSuccinctTransitionInstruction,
                type,
                bearingBefore,
                bearingAfter,
                voiceInstructions,
                beginShapeIndex,
                endShapeIndex,
                distanceMeters,
                startDistanceMeters,
                endDistanceMeters
        );
    }

    private static NavigationBundleManeuverType inferManeuverType(NavigationBundleManeuverSampleAction sampleAction) {
        if (sampleAction == null) {
            return NavigationBundleManeuverType.STRAIGHT;
        }

        return switch (sampleAction) {
            case RIGHT -> NavigationBundleManeuverType.RIGHT;
            case LEFT -> NavigationBundleManeuverType.LEFT;
            case UTURN -> NavigationBundleManeuverType.UTURN;
            case ARRIVAL -> NavigationBundleManeuverType.ARRIVAL;
            case STRAIGHT -> NavigationBundleManeuverType.STRAIGHT;
        };
    }

    private static NavigationBundleTurnStrength inferStrength(NavigationBundleManeuverSampleAction sampleAction) {
        if (sampleAction == null || sampleAction == NavigationBundleManeuverSampleAction.STRAIGHT || sampleAction == NavigationBundleManeuverSampleAction.ARRIVAL) {
            return null;
        }
        if (sampleAction == NavigationBundleManeuverSampleAction.UTURN) {
            return NavigationBundleTurnStrength.SHARP;
        }
        return NavigationBundleTurnStrength.NORMAL;
    }

    private static Integer calculateDeltaDegrees(Integer bearingBefore, Integer bearingAfter) {
        if (bearingBefore == null || bearingAfter == null) {
            return null;
        }

        int delta = Math.abs(bearingAfter - bearingBefore) % 360;
        return delta > 180 ? 360 - delta : delta;
    }
}
