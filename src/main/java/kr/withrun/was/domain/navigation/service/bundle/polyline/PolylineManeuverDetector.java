package kr.withrun.was.domain.navigation.service.bundle.polyline;

import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuver;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuverSampleAction;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuverType;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleSegment;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleShapePoint;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleTurnStrength;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PolylineManeuverDetector {

    private final PolylineNavigationProperties properties;
    private final PolylineInstructionFormatter instructionFormatter;

    @Autowired
    public PolylineManeuverDetector(
            PolylineNavigationProperties properties,
            PolylineInstructionFormatter instructionFormatter
    ) {
        this.properties = properties;
        this.instructionFormatter = instructionFormatter;
    }

    public List<NavigationBundleManeuver> detect(PolylineShapePreprocessor.PreprocessedShape preprocessedShape) {
        List<NavigationBundleShapePoint> shape = preprocessedShape.shape();
        List<NavigationBundleSegment> segments = preprocessedShape.segments();
        double[] cumulativeAtShape = cumulativeAtShape(shape.size(), segments);

        List<CandidateManeuver> candidates = new ArrayList<>();
        int incomingCursor = 0;
        int outgoingCursor = shape.size() > 1 ? 1 : 0;
        for (int shapeIndex = 1; shapeIndex < shape.size() - 1; shapeIndex++) {
            double currentDistanceMeters = cumulativeAtShape[shapeIndex];

            double incomingTargetDistanceMeters = Math.max(0d, currentDistanceMeters - properties.anchorWindowMeters());
            while (incomingCursor + 1 < shapeIndex && cumulativeAtShape[incomingCursor + 1] <= incomingTargetDistanceMeters) {
                incomingCursor++;
            }
            int incomingAnchorIndex = pickClosestIndex(
                    incomingCursor,
                    Math.min(shapeIndex - 1, incomingCursor + 1),
                    incomingTargetDistanceMeters,
                    cumulativeAtShape
            );

            double outgoingTargetDistanceMeters = Math.min(
                    preprocessedShape.totalDistanceMeters(),
                    currentDistanceMeters + properties.anchorWindowMeters()
            );
            if (outgoingCursor <= shapeIndex) {
                outgoingCursor = shapeIndex + 1;
            }
            while (outgoingCursor < shape.size() - 1 && cumulativeAtShape[outgoingCursor] < outgoingTargetDistanceMeters) {
                outgoingCursor++;
            }
            int outgoingAnchorIndex = pickClosestIndex(
                    Math.max(shapeIndex + 1, outgoingCursor - 1),
                    Math.max(shapeIndex + 1, outgoingCursor),
                    outgoingTargetDistanceMeters,
                    cumulativeAtShape
            );

            double incomingSpanMeters = currentDistanceMeters - cumulativeAtShape[incomingAnchorIndex];
            double outgoingSpanMeters = cumulativeAtShape[outgoingAnchorIndex] - currentDistanceMeters;
            if (incomingSpanMeters < properties.noiseFloorMeters()
                    || outgoingSpanMeters < properties.noiseFloorMeters()
                    || segments.get(shapeIndex - 1).distanceMeters() + segments.get(shapeIndex).distanceMeters() < properties.noiseFloorMeters()) {
                continue;
            }

            Integer bearingBeforeDegrees = calculateBearing(shape.get(incomingAnchorIndex), shape.get(shapeIndex));
            Integer bearingAfterDegrees = calculateBearing(shape.get(shapeIndex), shape.get(outgoingAnchorIndex));
            if (bearingBeforeDegrees == null || bearingAfterDegrees == null) {
                continue;
            }

            CandidateManeuver candidate = classifyCandidate(
                    shapeIndex,
                    currentDistanceMeters,
                    incomingAnchorIndex,
                    outgoingAnchorIndex,
                    bearingBeforeDegrees,
                    bearingAfterDegrees
            );
            if (candidate != null) {
                candidates.add(candidate);
            }
        }

        List<CandidateManeuver> mergedCandidates = mergeCandidates(candidates);
        List<NavigationBundleManeuver> maneuvers = new ArrayList<>(mergedCandidates.size() + 1);
        for (CandidateManeuver candidate : mergedCandidates) {
            maneuvers.add(candidate.toNavigationBundleManeuver(instructionFormatter, properties.language()));
        }

        maneuvers.add(new NavigationBundleManeuver(
                NavigationBundleManeuverType.ARRIVAL,
                null,
                NavigationBundleManeuverSampleAction.ARRIVAL,
                instructionFormatter.format(NavigationBundleManeuverType.ARRIVAL, properties.language()),
                shape.size() - 1,
                preprocessedShape.totalDistanceMeters(),
                null,
                null,
                null,
                null,
                null
        ));

        return List.copyOf(maneuvers);
    }

    private double[] cumulativeAtShape(int shapeSize, List<NavigationBundleSegment> segments) {
        double[] cumulativeAtShape = new double[shapeSize];
        for (NavigationBundleSegment segment : segments) {
            cumulativeAtShape[segment.endShapeIndex()] = segment.endCumulativeDistanceMeters();
        }
        return cumulativeAtShape;
    }

    private CandidateManeuver classifyCandidate(
            int shapeIndex,
            double cumulativeDistanceMeters,
            int incomingAnchorIndex,
            int outgoingAnchorIndex,
            int bearingBeforeDegrees,
            int bearingAfterDegrees
    ) {
        int signedDeltaDegrees = (int) Math.round(((bearingAfterDegrees - bearingBeforeDegrees + 540d) % 360d) - 180d);
        int absoluteDeltaDegrees = Math.abs(signedDeltaDegrees);
        if (absoluteDeltaDegrees < properties.straightThresholdDeg()) {
            return null;
        }

        if (absoluteDeltaDegrees >= properties.uTurnThresholdDeg()) {
            return new CandidateManeuver(
                    NavigationBundleManeuverType.UTURN,
                    NavigationBundleTurnStrength.SHARP,
                    NavigationBundleManeuverSampleAction.UTURN,
                    shapeIndex,
                    cumulativeDistanceMeters,
                    incomingAnchorIndex,
                    outgoingAnchorIndex,
                    bearingBeforeDegrees,
                    bearingAfterDegrees,
                    absoluteDeltaDegrees
            );
        }

        boolean rightTurn = signedDeltaDegrees > 0;
        if (absoluteDeltaDegrees < properties.mildThresholdDeg()) {
            return createDirectionalCandidate(
                    rightTurn ? NavigationBundleManeuverType.SLIGHT_RIGHT : NavigationBundleManeuverType.SLIGHT_LEFT,
                    NavigationBundleTurnStrength.SLIGHT,
                    rightTurn ? NavigationBundleManeuverSampleAction.RIGHT : NavigationBundleManeuverSampleAction.LEFT,
                    shapeIndex,
                    cumulativeDistanceMeters,
                    incomingAnchorIndex,
                    outgoingAnchorIndex,
                    bearingBeforeDegrees,
                    bearingAfterDegrees,
                    absoluteDeltaDegrees
            );
        }
        if (absoluteDeltaDegrees < properties.normalThresholdDeg()) {
            return createDirectionalCandidate(
                    rightTurn ? NavigationBundleManeuverType.RIGHT : NavigationBundleManeuverType.LEFT,
                    NavigationBundleTurnStrength.NORMAL,
                    rightTurn ? NavigationBundleManeuverSampleAction.RIGHT : NavigationBundleManeuverSampleAction.LEFT,
                    shapeIndex,
                    cumulativeDistanceMeters,
                    incomingAnchorIndex,
                    outgoingAnchorIndex,
                    bearingBeforeDegrees,
                    bearingAfterDegrees,
                    absoluteDeltaDegrees
            );
        }

        return createDirectionalCandidate(
                rightTurn ? NavigationBundleManeuverType.SHARP_RIGHT : NavigationBundleManeuverType.SHARP_LEFT,
                NavigationBundleTurnStrength.SHARP,
                rightTurn ? NavigationBundleManeuverSampleAction.RIGHT : NavigationBundleManeuverSampleAction.LEFT,
                shapeIndex,
                cumulativeDistanceMeters,
                incomingAnchorIndex,
                outgoingAnchorIndex,
                bearingBeforeDegrees,
                bearingAfterDegrees,
                absoluteDeltaDegrees
        );
    }

    private CandidateManeuver createDirectionalCandidate(
            NavigationBundleManeuverType maneuverType,
            NavigationBundleTurnStrength strength,
            NavigationBundleManeuverSampleAction sampleAction,
            int shapeIndex,
            double cumulativeDistanceMeters,
            int incomingAnchorIndex,
            int outgoingAnchorIndex,
            int bearingBeforeDegrees,
            int bearingAfterDegrees,
            int absoluteDeltaDegrees
    ) {
        return new CandidateManeuver(
                maneuverType,
                strength,
                sampleAction,
                shapeIndex,
                cumulativeDistanceMeters,
                incomingAnchorIndex,
                outgoingAnchorIndex,
                bearingBeforeDegrees,
                bearingAfterDegrees,
                absoluteDeltaDegrees
        );
    }

    private List<CandidateManeuver> mergeCandidates(List<CandidateManeuver> candidates) {
        List<CandidateManeuver> mergedCandidates = new ArrayList<>();
        for (CandidateManeuver candidate : candidates) {
            if (mergedCandidates.isEmpty()) {
                mergedCandidates.add(candidate);
                continue;
            }

            CandidateManeuver previous = mergedCandidates.getLast();
            if (sameDirectionFamily(previous, candidate)
                    && candidate.cumulativeDistanceMeters() - previous.cumulativeDistanceMeters() <= properties.mergeDistanceMeters()) {
                if (candidate.deltaDegrees() > previous.deltaDegrees()) {
                    mergedCandidates.set(mergedCandidates.size() - 1, candidate);
                }
                continue;
            }

            mergedCandidates.add(candidate);
        }
        return List.copyOf(mergedCandidates);
    }

    private boolean sameDirectionFamily(CandidateManeuver left, CandidateManeuver right) {
        return directionFamily(left.maneuverType()) != null
                && directionFamily(left.maneuverType()) == directionFamily(right.maneuverType());
    }

    private DirectionFamily directionFamily(NavigationBundleManeuverType maneuverType) {
        return switch (maneuverType) {
            case SLIGHT_LEFT, LEFT, SHARP_LEFT -> DirectionFamily.LEFT;
            case SLIGHT_RIGHT, RIGHT, SHARP_RIGHT -> DirectionFamily.RIGHT;
            default -> null;
        };
    }

    private int pickClosestIndex(int firstIndex, int secondIndex, double targetDistanceMeters, double[] cumulativeAtShape) {
        int boundedFirstIndex = Math.max(0, Math.min(firstIndex, cumulativeAtShape.length - 1));
        int boundedSecondIndex = Math.max(0, Math.min(secondIndex, cumulativeAtShape.length - 1));
        if (Math.abs(cumulativeAtShape[boundedSecondIndex] - targetDistanceMeters)
                < Math.abs(cumulativeAtShape[boundedFirstIndex] - targetDistanceMeters)) {
            return boundedSecondIndex;
        }
        return boundedFirstIndex;
    }

    private Integer calculateBearing(NavigationBundleShapePoint start, NavigationBundleShapePoint end) {
        if (Double.compare(start.lat(), end.lat()) == 0 && Double.compare(start.lon(), end.lon()) == 0) {
            return null;
        }

        double startLatitudeRadians = Math.toRadians(start.lat());
        double endLatitudeRadians = Math.toRadians(end.lat());
        double longitudeDeltaRadians = Math.toRadians(end.lon() - start.lon());

        double y = Math.sin(longitudeDeltaRadians) * Math.cos(endLatitudeRadians);
        double x = Math.cos(startLatitudeRadians) * Math.sin(endLatitudeRadians)
                - Math.sin(startLatitudeRadians) * Math.cos(endLatitudeRadians) * Math.cos(longitudeDeltaRadians);
        double bearingDegrees = Math.toDegrees(Math.atan2(y, x));
        return (int) Math.round((bearingDegrees + 360d) % 360d);
    }

    private enum DirectionFamily {
        LEFT,
        RIGHT
    }

    private record CandidateManeuver(
            NavigationBundleManeuverType maneuverType,
            NavigationBundleTurnStrength strength,
            NavigationBundleManeuverSampleAction sampleAction,
            int shapeIndex,
            double cumulativeDistanceMeters,
            int incomingAnchorShapeIndex,
            int outgoingAnchorShapeIndex,
            int bearingBeforeDegrees,
            int bearingAfterDegrees,
            int deltaDegrees
    ) {

        private NavigationBundleManeuver toNavigationBundleManeuver(
                PolylineInstructionFormatter instructionFormatter,
                String language
        ) {
            return new NavigationBundleManeuver(
                    maneuverType,
                    strength,
                    sampleAction,
                    instructionFormatter.format(maneuverType, language),
                    shapeIndex,
                    cumulativeDistanceMeters,
                    incomingAnchorShapeIndex,
                    outgoingAnchorShapeIndex,
                    bearingBeforeDegrees,
                    bearingAfterDegrees,
                    deltaDegrees
            );
        }
    }
}
