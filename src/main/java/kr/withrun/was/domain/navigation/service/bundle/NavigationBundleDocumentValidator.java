package kr.withrun.was.domain.navigation.service.bundle;

import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleDocument;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuver;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuverType;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleSegment;
import kr.withrun.was.domain.navigation.exception.NavigationBundleGenerationException;
import kr.withrun.was.domain.navigation.type.NavigationBundleFailureCode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
/**
 * 생성된 latest navigation bundle 문서가 최소 요구사항을 만족하는지 검증합니다.
 */
public class NavigationBundleDocumentValidator {

    private static final double DISTANCE_TOLERANCE_METERS = 1d;

    /**
     * shape, edge, 거리 정보가 유효한지 확인하고 문제가 있으면 예외를 던집니다.
     */
    public void validate(NavigationBundleDocument document) {
        if (document.shape() == null || document.shape().size() < 2) {
            throw new NavigationBundleGenerationException(
                    NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                    "Latest navigation bundle shape must contain at least two points"
            );
        }

        if (document.metadata() == null || document.totalDistanceMeters() < 0d) {
            throw new NavigationBundleGenerationException(
                    NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                    "Latest navigation bundle metadata and total distance must be valid"
            );
        }

        validateSegments(document.segments(), document.shape().size(), document.totalDistanceMeters());
        validateManeuvers(document.maneuvers(), document.shape().size(), document.totalDistanceMeters());
    }

    private void validateSegments(List<NavigationBundleSegment> segments, int shapeSize, double totalDistanceMeters) {
        if (segments == null || segments.size() != shapeSize - 1) {
            throw new NavigationBundleGenerationException(
                    NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                    "Latest navigation bundle segments must match shape continuity"
            );
        }

        double previousEndCumulativeDistanceMeters = 0d;
        for (int segmentIndex = 0; segmentIndex < segments.size(); segmentIndex++) {
            NavigationBundleSegment segment = segments.get(segmentIndex);
            if (segment == null
                    || segment.startShapeIndex() != segmentIndex
                    || segment.endShapeIndex() != segmentIndex + 1) {
                throw new NavigationBundleGenerationException(
                        NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                        "Latest navigation bundle segment shape range is invalid"
                );
            }

            if (segment.distanceMeters() < 0d
                    || segment.startCumulativeDistanceMeters() < 0d
                    || segment.endCumulativeDistanceMeters() < 0d
                    || segment.startCumulativeDistanceMeters() > segment.endCumulativeDistanceMeters()) {
                throw new NavigationBundleGenerationException(
                        NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                        "Latest navigation bundle segment distances are invalid"
                );
            }

            if (segmentIndex == 0) {
                if (Math.abs(segment.startCumulativeDistanceMeters()) > DISTANCE_TOLERANCE_METERS) {
                    throw new NavigationBundleGenerationException(
                            NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                            "Latest navigation bundle first segment must start at zero distance"
                    );
                }
            } else if (Math.abs(segment.startCumulativeDistanceMeters() - previousEndCumulativeDistanceMeters) > DISTANCE_TOLERANCE_METERS) {
                throw new NavigationBundleGenerationException(
                        NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                        "Latest navigation bundle segment cumulative distances must be contiguous"
                );
            }

            previousEndCumulativeDistanceMeters = segment.endCumulativeDistanceMeters();
        }

        if (Math.abs(previousEndCumulativeDistanceMeters - totalDistanceMeters) > DISTANCE_TOLERANCE_METERS) {
            throw new NavigationBundleGenerationException(
                    NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                    "Latest navigation bundle total distance and segments must match"
            );
        }
    }

    private void validateManeuvers(List<NavigationBundleManeuver> maneuvers, int shapeSize, double totalDistanceMeters) {
        if (maneuvers == null || maneuvers.isEmpty()) {
            throw new NavigationBundleGenerationException(
                    NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                    "Latest navigation bundle maneuvers must not be empty"
            );
        }

        double previousCumulativeDistanceMeters = -1d;
        for (NavigationBundleManeuver maneuver : maneuvers) {
            if (maneuver == null
                    || maneuver.shapeIndex() < 0
                    || maneuver.shapeIndex() >= shapeSize
                    || maneuver.cumulativeDistanceMeters() < 0d
                    || maneuver.cumulativeDistanceMeters() > totalDistanceMeters + DISTANCE_TOLERANCE_METERS) {
                throw new NavigationBundleGenerationException(
                        NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                        "Latest navigation bundle maneuver data is invalid"
                );
            }

            if (maneuver.cumulativeDistanceMeters() + DISTANCE_TOLERANCE_METERS < previousCumulativeDistanceMeters) {
                throw new NavigationBundleGenerationException(
                        NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                        "Latest navigation bundle maneuvers must be ordered"
                );
            }

            previousCumulativeDistanceMeters = maneuver.cumulativeDistanceMeters();
        }

        NavigationBundleManeuver finalManeuver = maneuvers.get(maneuvers.size() - 1);
        if (finalManeuver.maneuverType() != NavigationBundleManeuverType.ARRIVAL
                || finalManeuver.shapeIndex() != shapeSize - 1) {
            throw new NavigationBundleGenerationException(
                    NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                    "Latest navigation bundle must end with an arrival maneuver at the final shape index"
            );
        }
    }
}
