package kr.withrun.was.domain.navigation.dto.internal.bundle;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NavigationBundleTurnStrength {
    SLIGHT("SLIGHT", "완만"),
    NORMAL("NORMAL", "일반"),
    SHARP("SHARP", "급");

    private final String data;
    private final String label;
}
