package kr.withrun.was.domain.navigation.service.bundle.polyline;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PolylineNavigationProperties {

    private final String language;
    private final double noiseFloorMeters;
    private final double anchorWindowMeters;
    private final double mergeDistanceMeters;
    private final int straightThresholdDeg;
    private final int mildThresholdDeg;
    private final int normalThresholdDeg;
    private final int uTurnThresholdDeg;

    PolylineNavigationProperties() {
        this("ko-KR", 6.0, 15.0, 24.0, 30, 60, 125, 170);
    }

    PolylineNavigationProperties(
            String language,
            double noiseFloorMeters,
            double anchorWindowMeters,
            double mergeDistanceMeters,
            int straightThresholdDeg,
            int mildThresholdDeg,
            int normalThresholdDeg,
            int uTurnThresholdDeg
    ) {
        this.language = language == null ? "ko-KR" : language;
        this.noiseFloorMeters = noiseFloorMeters;
        this.anchorWindowMeters = anchorWindowMeters;
        this.mergeDistanceMeters = mergeDistanceMeters;
        this.straightThresholdDeg = straightThresholdDeg;
        this.mildThresholdDeg = mildThresholdDeg;
        this.normalThresholdDeg = normalThresholdDeg;
        this.uTurnThresholdDeg = uTurnThresholdDeg;
    }

    @Autowired
    public PolylineNavigationProperties(
            @Value("${app.navigation.polyline.language:ko-KR}") String language,
            @Value("${app.navigation.polyline.noise-floor-meters:6.0}") double noiseFloorMeters,
            @Value("${app.navigation.polyline.anchor-window-meters:15.0}") double anchorWindowMeters,
            @Value("${app.navigation.polyline.merge-distance-meters:24.0}") double mergeDistanceMeters,
            @Value("${app.navigation.polyline.straight-threshold-deg:30}") int straightThresholdDeg,
            @Value("${app.navigation.polyline.mild-threshold-deg:60}") int mildThresholdDeg,
            @Value("${app.navigation.polyline.normal-threshold-deg:125}") int normalThresholdDeg,
            @Value("${app.navigation.polyline.u-turn-threshold-deg:170}") int uTurnThresholdDeg,
            @Value("${app.navigation.polyline.generator:polyline-navigation-bundle}") String ignoredGenerator
    ) {
        this(language, noiseFloorMeters, anchorWindowMeters, mergeDistanceMeters, straightThresholdDeg, mildThresholdDeg, normalThresholdDeg, uTurnThresholdDeg);
    }

    public String language() {
        return language;
    }

    public double noiseFloorMeters() {
        return noiseFloorMeters;
    }

    public double anchorWindowMeters() {
        return anchorWindowMeters;
    }

    public double mergeDistanceMeters() {
        return mergeDistanceMeters;
    }

    public int straightThresholdDeg() {
        return straightThresholdDeg;
    }

    public int mildThresholdDeg() {
        return mildThresholdDeg;
    }

    public int normalThresholdDeg() {
        return normalThresholdDeg;
    }

    public int uTurnThresholdDeg() {
        return uTurnThresholdDeg;
    }
}
