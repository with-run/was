package kr.withrun.was.domain.navigation.service.bundle.polyline;

import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleDocument;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class PolylineBundleDocumentFactory {

    private final PolylineShapePreprocessor preprocessor;
    private final PolylineManeuverDetector maneuverDetector;
    private final Clock clock;
    private final PolylineNavigationProperties properties;

    @Autowired
    public PolylineBundleDocumentFactory(
            PolylineShapePreprocessor preprocessor,
            PolylineManeuverDetector maneuverDetector,
            PolylineNavigationProperties properties
    ) {
        this(
                preprocessor,
                maneuverDetector,
                Clock.systemUTC(),
                properties
        );
    }

    PolylineBundleDocumentFactory(
            PolylineShapePreprocessor preprocessor,
            PolylineManeuverDetector maneuverDetector,
            Clock clock,
            PolylineNavigationProperties properties
    ) {
        this.preprocessor = preprocessor;
        this.maneuverDetector = maneuverDetector;
        this.clock = clock;
        this.properties = properties;
    }

    public NavigationBundleDocument build(Coordinates coordinates) {
        PolylineShapePreprocessor.PreprocessedShape preprocessedShape = preprocessor.preprocess(coordinates);

        return new NavigationBundleDocument(
                preprocessedShape.shape(),
                preprocessedShape.segments(),
                maneuverDetector.detect(preprocessedShape),
                preprocessedShape.totalDistanceMeters(),
                new NavigationBundleMetadata(
                        Instant.now(clock),
                        "polyline-navigation-bundle",
                        "v1",
                        properties.language(),
                        "course.coordinates",
                        preprocessedShape.shape().size(),
                        preprocessedShape.segments().size()
                )
        );
    }
}
