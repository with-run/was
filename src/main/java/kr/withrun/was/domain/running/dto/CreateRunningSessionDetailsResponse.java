package kr.withrun.was.domain.running.dto;

public record CreateRunningSessionDetailsResponse(
        Long runningSessionId,
        Long userId,
        int savedGpsSampleCount,
        int savedHealthSampleCount,
        int savedSplitCount
) {
    public static CreateRunningSessionDetailsResponse of(
            Long runningSessionId,
            Long userId,
            int savedGpsSampleCount,
            int savedHealthSampleCount,
            int savedSplitCount
    ) {
        return new CreateRunningSessionDetailsResponse(
                runningSessionId,
                userId,
                savedGpsSampleCount,
                savedHealthSampleCount,
                savedSplitCount
        );
    }
}
