package kr.withrun.was.domain.user.service;

import kr.withrun.was.domain.user.dto.NicknameAvailabilityResponse;
import kr.withrun.was.domain.user.dto.UpdateUserProfileRequest;
import kr.withrun.was.domain.user.dto.UserProfileResponse;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.UserAuthAccountRepository;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserAuthAccountRepository userAuthAccountRepository;

    public UserProfileResponse getProfile(Long userId) {
        // 조회/수정 모두 soft delete 되지 않은 활성 사용자만 대상으로 합니다.
        User user = userRepository.findNotDeletedUser(userId)
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));

        return UserProfileResponse.from(user);
    }

    public NicknameAvailabilityResponse getNicknameAvailability(String nickname) {
        String normalizedNickname = nickname.trim();
        validateNickname(normalizedNickname);

        return new NicknameAvailabilityResponse(
                !userRepository.existsActiveUserByNickname(normalizedNickname)
        );
    }

    @Transactional
    public void updateProfile(Long userId, UpdateUserProfileRequest request) {
        // 인증된 현재 사용자만 수정 가능하므로 path variable 대신 access token에서 얻은 userId로 조회합니다.
        User user = userRepository.findNotDeletedUser(userId)
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));

        // DTO의 @NotBlank만으로는 앞뒤 공백 제거 후 빈 문자열 여부를 알 수 없어서 서비스에서 한 번 더 다룹니다.
        String nickname = request.nickname().trim();
        validateNickname(nickname);

        // 본인 닉네임 유지 변경은 허용하고, 다른 활성 사용자와의 충돌만 막습니다.
        if (userRepository.existsByNicknameAndIdNot(nickname, userId)) {
            throw new CustomException(ResponseCode.NICKNAME_ALREADY_EXISTS);
        }

        try {
            // 엔티티의 일관성 검증 로직은 User.completeProfile에 그대로 위임합니다.
            // 이 API는 pending user의 온보딩 완료와 기존 user의 프로필 수정 둘 다 처리합니다.
            user.completeProfile(
                    nickname,
                    request.birthDate(),
                    request.gender(),
                    request.height(),
                    request.weight()
            );
        } catch (IllegalArgumentException exception) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }
    }

    @Transactional
    public void deleteProfile(Long userId) {
        User user = userRepository.findNotDeletedUser(userId)
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));

        // 탈퇴 후 같은 소셜 계정으로 재가입할 수 있도록 provider 연결 정보는 먼저 제거합니다.
        userAuthAccountRepository.findById(userId)
                .ifPresent(userAuthAccountRepository::delete);

        // User 엔티티의 @SQLDelete 가 실제로는 deleted_at 을 채우는 soft delete 를 수행합니다.
        userRepository.delete(user);
    }

    private void validateNickname(String nickname) {
        // 닉네임 길이 제한은 User 컬럼 길이(16)와 맞춰둡니다.
        if (nickname.isBlank() || nickname.length() > 16) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }
    }
}
