package kr.withrun.was.domain.running.service;

import kr.withrun.was.domain.running.dto.GhostRunningResultResponse;
import kr.withrun.was.domain.running.repository.GhostRunningResultRepository;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class
GhostRunningResultService {

    private final GhostRunningResultRepository ghostRunningResultRepository;

    public GhostRunningResultResponse findGhostRunningResult(Long runningSessionId) {
        return ghostRunningResultRepository.findByRunningSessionId(runningSessionId)
                .map(GhostRunningResultResponse::from)
                .orElseThrow(() -> new CustomException(ResponseCode.GHOST_RUNNING_RESULT_NOT_FOUND));
    }

    public Integer calculateGhostRunningPoint(Integer durationSec, Difficulty difficulty) {
        if (durationSec == null || durationSec <= 0 || difficulty == null) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }

        double durationMinutes = durationSec / 60.0;

        double baseScore = 5_000.0 / (1.0 + Math.pow(durationMinutes / 18.0, 1.55));
        double paceBonus = 320.0 / (Math.log1p(durationMinutes) + 0.8);
        int difficultyBonus = switch (difficulty) {
            case EASY -> 0;
            case MEDIUM -> 120;
            case HARD -> 260;
        };
        return (int) Math.round(baseScore + paceBonus + difficultyBonus);
    }

}
