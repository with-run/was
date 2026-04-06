package kr.withrun.was.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.global.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "user_calendars",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_calendars_user_date",
                columnNames = {"user_id", "calendar_date"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCalendar extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_calendar_id")
    private Long id;

    @Column(name = "calendar_date", nullable = false)
    private LocalDate calendarDate;

    @Column(name = "total_distance_m", nullable = false)
    private Integer totalDistanceM = 0;

    @Column(name = "total_duration_sec", nullable = false)
    private Integer totalDurationSec = 0;

    @Column(name = "total_calories_kcal", nullable = false)
    private Integer totalCaloriesKcal = 0;

    @Column(name = "course_run_count", nullable = false)
    private Integer courseRunCount = 0;

    @Column(name = "free_run_count", nullable = false)
    private Integer freeRunCount = 0;

    @Column(name = "ghost_run_count", nullable = false)
    private Integer ghostRunCount = 0;

    @Column(name = "representative_snapshot_url")
    private String representativeSnapshotUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_calendars_user")
    )
    private User user;

    public static UserCalendar create(User user, LocalDate calendarDate) {
        UserCalendar userCalendar = new UserCalendar();
        userCalendar.user = user;
        userCalendar.calendarDate = calendarDate;
        return userCalendar;
    }

    public void recordCompletedRun(
            RunningMode runningMode,
            Integer distanceM,
            Integer durationSec,
            Integer caloriesKcal,
            String representativeSnapshotUrl
    ) {
        this.totalDistanceM += zeroIfNull(distanceM);
        this.totalDurationSec += zeroIfNull(durationSec);
        this.totalCaloriesKcal += zeroIfNull(caloriesKcal);

        switch (runningMode) {
            case COURSE -> this.courseRunCount++;
            case FREE -> this.freeRunCount++;
            case GHOST -> this.ghostRunCount++;
        }

        if (representativeSnapshotUrl != null && !representativeSnapshotUrl.isBlank()) {
            this.representativeSnapshotUrl = representativeSnapshotUrl;
        }
    }

    private static int zeroIfNull(Integer value) {
        return value == null ? 0 : value;
    }
}
