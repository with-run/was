package kr.withrun.was.domain.user.service;

import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.user.dto.UpdateUserRunningPreferenceRequest;
import kr.withrun.was.domain.user.dto.UserRunningPreferenceResponse;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.entity.UserRunningPreference;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.domain.user.repository.UserRunningPreferenceRepository;
import kr.withrun.was.domain.user.type.Purpose;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.common.type.TimeSlot;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRunningPreferenceService")
class UserRunningPreferenceServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRunningPreferenceRepository userRunningPreferenceRepository;

    @InjectMocks
    private UserRunningPreferenceService userRunningPreferenceService;

    @Test
    @DisplayName("러닝 기본 설정이 없으면 빈 응답을 반환한다")
    void returnsEmptyRunningPreferenceWhenPreferenceDoesNotExist() {
        User user = User.createPendingSocialUser();
        setField(user, "id", 1L);

        given(userRepository.findNotDeletedUser(1L)).willReturn(Optional.of(user));

        UserRunningPreferenceResponse response = userRunningPreferenceService.getRunningPreference(1L);

        assertThat(response.purposes()).isEmpty();
        assertThat(response.timeSlots()).isEmpty();
        assertThat(response.preferredDistanceKm()).isNull();
        assertThat(response.preferredDifficulty()).isNull();
        assertThat(response.courseTypes()).isEmpty();
    }

    @Test
    @DisplayName("러닝 기본 설정이 없으면 새로 생성해 저장한다")
    void createsRunningPreferenceWhenPreferenceDoesNotExist() {
        User user = User.createPendingSocialUser();
        setField(user, "id", 1L);
        UpdateUserRunningPreferenceRequest request = new UpdateUserRunningPreferenceRequest(
                List.of(Purpose.HEALTH_MAINTENANCE, Purpose.DIET),
                List.of(TimeSlot.AFTERNOON, TimeSlot.EVENING),
                7.5,
                Difficulty.MEDIUM,
                List.of(CourseType.URBAN, CourseType.PARK)
        );

        given(userRepository.findNotDeletedUser(1L)).willReturn(Optional.of(user));

        userRunningPreferenceService.updateRunningPreference(1L, request);

        UserRunningPreference preference = user.getUserRunningPreference();
        assertThat(preference).isNotNull();
        assertThat(preference.getPreferredDistanceKm()).isEqualTo(7.5);
        assertThat(preference.getPreferredDifficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(preference.getPurposes())
                .extracting(purpose -> purpose.getPurpose())
                .containsExactly(Purpose.HEALTH_MAINTENANCE, Purpose.DIET);
        assertThat(preference.getTimeSlots())
                .extracting(timeSlot -> timeSlot.getTimeSlot())
                .containsExactly(TimeSlot.AFTERNOON, TimeSlot.EVENING);
        assertThat(preference.getCourseTypes())
                .extracting(courseType -> courseType.getCourseType())
                .containsExactly(CourseType.URBAN, CourseType.PARK);

        then(userRunningPreferenceRepository).should().save(any(UserRunningPreference.class));
    }

    @Test
    @DisplayName("기존 러닝 기본 설정은 다중 선택값 기준으로 교체 저장한다")
    void updatesExistingRunningPreference() {
        User user = User.createPendingSocialUser();
        setField(user, "id", 2L);
        UserRunningPreference existingPreference = UserRunningPreference.create(
                user,
                Difficulty.EASY,
                5.0
        );
        existingPreference.replacePurposes(List.of(Purpose.DIET));
        existingPreference.replaceTimeSlots(List.of(TimeSlot.MORNING));
        existingPreference.replaceCourseTypes(List.of(CourseType.PARK));
        user.assignRunningPreference(existingPreference);

        UpdateUserRunningPreferenceRequest request = new UpdateUserRunningPreferenceRequest(
                List.of(Purpose.ENDURANCE_IMPROVEMENT, Purpose.RACE_PREPARATION),
                List.of(TimeSlot.EVENING, TimeSlot.DAWN),
                15.0,
                Difficulty.HARD,
                List.of(CourseType.MOUNTAIN_TRAIL, CourseType.TRACK)
        );

        given(userRepository.findNotDeletedUser(2L)).willReturn(Optional.of(user));

        userRunningPreferenceService.updateRunningPreference(2L, request);

        assertThat(existingPreference.getPreferredDistanceKm()).isEqualTo(15.0);
        assertThat(existingPreference.getPreferredDifficulty()).isEqualTo(Difficulty.HARD);
        assertThat(existingPreference.getPurposes())
                .extracting(purpose -> purpose.getPurpose())
                .containsExactly(Purpose.ENDURANCE_IMPROVEMENT, Purpose.RACE_PREPARATION);
        assertThat(existingPreference.getTimeSlots())
                .extracting(timeSlot -> timeSlot.getTimeSlot())
                .containsExactly(TimeSlot.EVENING, TimeSlot.DAWN);
        assertThat(existingPreference.getCourseTypes())
                .extracting(courseType -> courseType.getCourseType())
                .containsExactly(CourseType.MOUNTAIN_TRAIL, CourseType.TRACK);
    }

    @Test
    @DisplayName("없는 사용자는 USER_NOT_FOUND를 반환한다")
    void throwsWhenUserIsNotFound() {
        UpdateUserRunningPreferenceRequest request = new UpdateUserRunningPreferenceRequest(
                List.of(Purpose.DIET),
                List.of(TimeSlot.MORNING),
                5.0,
                Difficulty.MEDIUM,
                List.of(CourseType.PARK)
        );

        given(userRepository.findNotDeletedUser(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userRunningPreferenceService.updateRunningPreference(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getResponseCode())
                .isEqualTo(ResponseCode.USER_NOT_FOUND);
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
