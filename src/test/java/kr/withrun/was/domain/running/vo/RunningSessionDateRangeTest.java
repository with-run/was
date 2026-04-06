package kr.withrun.was.domain.running.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("과거 러닝 세션 날짜 범위")
class RunningSessionDateRangeTest {

    @DisplayName("연도만 있으면 연간 범위를 계산한다")
    @Test
    void resolvesYearRange() {
        RunningSessionDateRange range = RunningSessionDateRange.of(2026, null, null);

        assertThat(range.startInclusive()).isEqualTo(LocalDate.of(2026, 1, 1).atStartOfDay());
        assertThat(range.endExclusive()).isEqualTo(LocalDate.of(2027, 1, 1).atStartOfDay());
    }

    @DisplayName("연월일이 모두 있으면 일간 범위를 계산한다")
    @Test
    void resolvesDayRange() {
        RunningSessionDateRange range = RunningSessionDateRange.of(2026, 3, 16);

        assertThat(range.startInclusive()).isEqualTo(LocalDate.of(2026, 3, 16).atStartOfDay());
        assertThat(range.endExclusive()).isEqualTo(LocalDate.of(2026, 3, 17).atStartOfDay());
    }

    @DisplayName("필터가 없으면 범위를 비운다")
    @Test
    void returnsEmptyRangeWhenNoDateFilterExists() {
        RunningSessionDateRange range = RunningSessionDateRange.of(null, null, null);

        assertThat(range.startInclusive()).isNull();
        assertThat(range.endExclusive()).isNull();
    }
}
