package kr.withrun.was.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.withrun.was.domain.auth.jwt.JwtProvider;
import kr.withrun.was.domain.user.dto.NicknameAvailabilityResponse;
import kr.withrun.was.domain.user.dto.UpdateUserProfileRequest;
import kr.withrun.was.domain.user.dto.UserProfileResponse;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.service.UserProfileService;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "spring.config.import=",
                "spring.cloud.aws.parameterstore.enabled=false"
        }
)
@AutoConfigureMockMvc
@DisplayName("UserProfileController")
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserProfileService userProfileService;

    @Test
    @DisplayName("유효한 access token이면 현재 사용자 프로필을 반환한다")
    void returnsProfileWhenTokenIsValid() throws Exception {
        given(userProfileService.getProfile(10L)).willReturn(
                new UserProfileResponse(
                        "runner",
                        LocalDate.of(1998, 4, 5),
                        Gender.FEMALE,
                        165.5,
                        52.3
                )
        );

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("runner"))
                .andExpect(jsonPath("$.data.height").value(165.5))
                .andExpect(jsonPath("$.data.weight").value(52.3));

        then(userProfileService).should().getProfile(10L);
    }

    @Test
    @DisplayName("유효한 access token이면 닉네임 사용 가능 여부를 반환한다")
    void returnsNicknameAvailabilityWhenTokenIsValid() throws Exception {
        given(userProfileService.getNicknameAvailability("runner-next")).willReturn(
                new NicknameAvailabilityResponse(true)
        );

        mockMvc.perform(get("/api/users/nickname-availability")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(10L))
                        .queryParam("nickname", "runner-next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(true));

        then(userProfileService).should().getNicknameAvailability("runner-next");
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 닉네임 사용 가능 여부 조회는 인증 필요 응답을 반환한다")
    void returnsUnauthorizedWhenNicknameAvailabilityAuthorizationHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/api/users/nickname-availability")
                        .queryParam("nickname", "runner-next"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.UNAUTHORIZED.getCode()));

        then(userProfileService).should(never()).getNicknameAvailability(any());
    }

    @Test
    @DisplayName("유효한 access token이면 현재 사용자 프로필 저장을 위임한다")
    void updatesProfileWhenTokenIsValid() throws Exception {
        // 컨트롤러 테스트에서는 서비스 구현보다 "인증된 userId가 정확히 전달되는지"가 핵심입니다.
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "runner",
                LocalDate.of(1998, 4, 5),
                Gender.FEMALE,
                165.5,
                52.3
        );

        mockMvc.perform(put("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()));

        then(userProfileService).should().updateProfile(eq(10L), eq(request));
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증 필요 응답을 반환한다")
    void returnsUnauthorizedWhenAuthorizationHeaderIsMissing() throws Exception {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "runner",
                LocalDate.of(1998, 4, 5),
                Gender.FEMALE,
                165.5,
                52.3
        );

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.UNAUTHORIZED.getCode()));

        then(userProfileService).should(never()).updateProfile(eq(10L), any(UpdateUserProfileRequest.class));
    }

    @Test
    @DisplayName("잘못된 토큰이면 TOKEN_INVALID 응답을 반환한다")
    void returnsUnauthorizedWhenTokenIsInvalid() throws Exception {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "runner",
                LocalDate.of(1998, 4, 5),
                Gender.FEMALE,
                165.5,
                52.3
        );

        mockMvc.perform(put("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.TOKEN_INVALID.getCode()));

        then(userProfileService).should(never()).updateProfile(eq(10L), any(UpdateUserProfileRequest.class));
    }

    @Test
    @DisplayName("본문 검증에 실패하면 INVALID_INPUT_VALUE 응답을 반환한다")
    void returnsBadRequestWhenBodyValidationFails() throws Exception {
        String invalidBody = """
                {
                  "nickname": " ",
                  "birthDate": "1998-04-05",
                  "gender": "FEMALE",
                  "height": 165.5,
                  "weight": 52.3
                }
                """;

        mockMvc.perform(put("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));

        then(userProfileService).should(never()).updateProfile(eq(10L), any(UpdateUserProfileRequest.class));
    }

    private String bearerToken(Long userId) {
        // 실제 JwtProvider로 만든 access token을 써서 보안 필터와 컨트롤러 연결을 함께 검증합니다.
        return "Bearer " + jwtProvider.generateTokenPair(completedUser(userId), "google").accessToken();
    }

    private User completedUser(Long id) {
        User user = User.createPendingSocialUser();
        setField(user, "id", id);
        user.completeProfile(
                "runner",
                LocalDate.of(1999, 1, 2),
                Gender.MALE,
                180.0,
                72.5
        );
        return user;
    }

    private void setField(Object target, String fieldName, Object value) {
        Class<?> currentClass = target.getClass();
        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException exception) {
                currentClass = currentClass.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Failed to set field " + fieldName, exception);
            }
        }

        throw new IllegalArgumentException("Field not found: " + fieldName);
    }
}
