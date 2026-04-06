package kr.withrun.was.domain.user.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import kr.withrun.was.global.common.entity.BaseEntity;
import kr.withrun.was.global.common.type.Difficulty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Entity
@Table(name = "user_running_preferences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRunningPreference extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_difficulty", nullable = false, length = 16)
    private Difficulty preferredDifficulty;

    @Column(name = "preferred_distance_km", nullable = false)
    private Double preferredDistanceKm;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_user_running_preferences_user"))
    private User user;

    @OneToMany(mappedBy = "userRunningPreference", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private final List<UserPreferencePurpose> purposes = new ArrayList<>();

    @OneToMany(mappedBy = "userRunningPreference", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private final List<UserPreferenceCourseType> courseTypes = new ArrayList<>();

    @OneToMany(mappedBy = "userRunningPreference", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id asc")
    private final List<UserPreferenceTimeSlot> timeSlots = new ArrayList<>();

    public static UserRunningPreference create(
            User user,
            Difficulty preferredDifficulty,
            Double preferredDistanceKm
    ) {
        // user_id를 PK로 공유하는 1:1 엔티티라 생성 시점에 user를 반드시 함께 묶습니다.
        UserRunningPreference preference = new UserRunningPreference();
        preference.user = user;
        preference.preferredDifficulty = preferredDifficulty;
        preference.preferredDistanceKm = preferredDistanceKm;
        return preference;
    }

    public void updatePreferredSettings(
            Difficulty preferredDifficulty,
            Double preferredDistanceKm
    ) {
        this.preferredDifficulty = preferredDifficulty;
        this.preferredDistanceKm = preferredDistanceKm;
    }

    public void addPurpose(UserPreferencePurpose purpose) {
        purposes.add(purpose);
        purpose.setUserRunningPreference(this);
    }

    public void addCourseType(UserPreferenceCourseType courseType) {
        courseTypes.add(courseType);
        courseType.setUserRunningPreference(this);
    }

    public void addTimeSlot(UserPreferenceTimeSlot timeSlot) {
        timeSlots.add(timeSlot);
        timeSlot.setUserRunningPreference(this);
    }

    public void replacePurposes(List<kr.withrun.was.domain.user.type.Purpose> purposes) {
        // 기존 값을 유지한 채 일부만 바꾸는 경우 clear 후 재삽입하면 unique 제약에 걸릴 수 있어 diff 기반으로 교체합니다.
        Set<kr.withrun.was.domain.user.type.Purpose> nextPurposes = new LinkedHashSet<>(purposes);

        this.purposes.removeIf(currentPurpose -> !nextPurposes.contains(currentPurpose.getPurpose()));

        Set<kr.withrun.was.domain.user.type.Purpose> existingPurposes = this.purposes.stream()
                .map(UserPreferencePurpose::getPurpose)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        nextPurposes.stream()
                .filter(nextPurpose -> !existingPurposes.contains(nextPurpose))
                .map(UserPreferencePurpose::create)
                .forEach(this::addPurpose);
    }

    public void replaceCourseTypes(List<kr.withrun.was.domain.course.type.CourseType> courseTypes) {
        Set<kr.withrun.was.domain.course.type.CourseType> nextCourseTypes = new LinkedHashSet<>(courseTypes);

        this.courseTypes.removeIf(
                currentCourseType -> !nextCourseTypes.contains(currentCourseType.getCourseType())
        );

        Set<kr.withrun.was.domain.course.type.CourseType> existingCourseTypes = this.courseTypes.stream()
                .map(UserPreferenceCourseType::getCourseType)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        nextCourseTypes.stream()
                .filter(nextCourseType -> !existingCourseTypes.contains(nextCourseType))
                .map(UserPreferenceCourseType::create)
                .forEach(this::addCourseType);
    }

    public void replaceTimeSlots(List<kr.withrun.was.global.common.type.TimeSlot> timeSlots) {
        Set<kr.withrun.was.global.common.type.TimeSlot> nextTimeSlots = new LinkedHashSet<>(timeSlots);

        this.timeSlots.removeIf(currentTimeSlot -> !nextTimeSlots.contains(currentTimeSlot.getTimeSlot()));

        Set<kr.withrun.was.global.common.type.TimeSlot> existingTimeSlots = this.timeSlots.stream()
                .map(UserPreferenceTimeSlot::getTimeSlot)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        nextTimeSlots.stream()
                .filter(nextTimeSlot -> !existingTimeSlots.contains(nextTimeSlot))
                .map(UserPreferenceTimeSlot::create)
                .forEach(this::addTimeSlot);
    }
}
