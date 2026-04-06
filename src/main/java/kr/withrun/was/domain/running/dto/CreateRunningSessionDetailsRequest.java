package kr.withrun.was.domain.running.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CreateRunningSessionDetailsRequest(
        @NotNull
        @Positive
        Long userId,

        @NotNull
        List<@Valid CreateRunningGpsSampleRequest> gpsSamples,

        @NotNull
        List<@Valid CreateRunningHealthSampleRequest> healthSamples,

        @NotNull
        List<@Valid CreateRunningSessionSplitRequest> splits
) {
}
