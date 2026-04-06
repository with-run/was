package kr.withrun.was.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.withrun.was.global.common.entity.BaseEntity;
import kr.withrun.was.global.common.type.TimeSlot;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(
        name = "user_preference_time_slots",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_preference_time_slots_user_slot",
                columnNames = {"user_id", "time_slot"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreferenceTimeSlot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_preference_time_slot_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_slot", nullable = false, length = 16)
    private TimeSlot timeSlot;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_preference_time_slot_user_running_preference")
    )
    private UserRunningPreference userRunningPreference;

    public static UserPreferenceTimeSlot create(TimeSlot timeSlot) {
        UserPreferenceTimeSlot preferenceTimeSlot = new UserPreferenceTimeSlot();
        preferenceTimeSlot.timeSlot = timeSlot;
        return preferenceTimeSlot;
    }
}
