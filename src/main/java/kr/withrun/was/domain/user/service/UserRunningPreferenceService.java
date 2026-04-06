package kr.withrun.was.domain.user.service;

import kr.withrun.was.domain.user.dto.UpdateUserRunningPreferenceRequest;
import kr.withrun.was.domain.user.dto.UserRunningPreferenceResponse;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.entity.UserRunningPreference;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.domain.user.repository.UserRunningPreferenceRepository;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserRunningPreferenceService {

    private final UserRepository userRepository;
    private final UserRunningPreferenceRepository userRunningPreferenceRepository;

    public UserRunningPreferenceResponse getRunningPreference(Long userId) {
        // 아직 기본 설정을 저장하지 않은 사용자도 있을 수 있어 null-safe 응답으로 변환합니다.
        User user = findUser(userId);
        return UserRunningPreferenceResponse.from(user.getUserRunningPreference());
    }

    @Transactional
    public void updateRunningPreference(Long userId, UpdateUserRunningPreferenceRequest request) {
        User user = findUser(userId);
        UserRunningPreference runningPreference = user.getUserRunningPreference();

        if (runningPreference == null) {
            // 첫 저장 시에는 사용자와 1:1로 연결된 선호 엔티티를 새로 만듭니다.
            runningPreference = UserRunningPreference.create(
                    user,
                    request.preferredDifficulty(),
                    request.preferredDistanceKm()
            );
            user.assignRunningPreference(runningPreference);
        } else {
            // 이후 수정은 거리/난이도 같은 단일 컬럼만 갱신합니다.
            runningPreference.updatePreferredSettings(
                    request.preferredDifficulty(),
                    request.preferredDistanceKm()
            );
        }

        // 온보딩/설정 화면에서 다중 선택한 항목을 카테고리별 컬렉션으로 그대로 저장합니다.
        runningPreference.replacePurposes(request.purposes());
        runningPreference.replaceTimeSlots(request.timeSlots());
        runningPreference.replaceCourseTypes(request.courseTypes());

        userRunningPreferenceRepository.save(runningPreference);
    }

    private User findUser(Long userId) {
        // 조회 API와 수정 API가 같은 사용자 조회 규칙을 쓰도록 내부 메서드로 고정합니다.
        return userRepository.findNotDeletedUser(userId)
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));
    }
}
