package kr.withrun.was.domain.course.entity;

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
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.RouteType;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.global.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "courses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseEntity {

    @Id
    @Column(name = "course_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CourseStatus status;

    @Column(name = "distance_m", nullable = false)
    private Integer distanceM;

    @Column(name = "elevation_gain_m", nullable = false)
    private Integer elevationGainM;

    @Column(name = "snapshot_image_url")
    private String snapshotImageUrl;

    @Column(name = "navigation_bundle_url")
    private String navigationBundleUrl;

    @Column(name = "start_latitude", nullable = false)
    private Double startLatitude;

    @Column(name = "start_longitude", nullable = false)
    private Double startLongitude;

    @Column(name = "end_latitude")
    private Double endLatitude;

    @Column(name = "end_longitude")
    private Double endLongitude;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "coordinates", columnDefinition = "jsonb", nullable = false)
    private Coordinates coordinates;

    @Column(name = "promoted_at")
    private LocalDateTime promotedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_type")
    private RouteType routeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by_user_id",
            foreignKey = @ForeignKey(name = "fk_courses_created_by")
    )
    private User user;

    @Builder
    private Course(
            String title,
            CourseStatus status,
            Integer distanceM,
            Integer elevationGainM,
            String snapshotImageUrl,
            String navigationBundleUrl,
            Double startLatitude,
            Double startLongitude,
            Double endLatitude,
            Double endLongitude,
            Coordinates coordinates,
            LocalDateTime promotedAt,
            RouteType routeType,
            User user
    ) {
        this.title = title;
        this.status = status;
        this.distanceM = distanceM;
        this.elevationGainM = elevationGainM;
        this.snapshotImageUrl = snapshotImageUrl;
        this.navigationBundleUrl = navigationBundleUrl;
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
        this.endLatitude = endLatitude;
        this.endLongitude = endLongitude;
        this.coordinates = coordinates;
        this.promotedAt = promotedAt;
        this.routeType = routeType;
        this.user = user;
    }

    public void promoteToOfficialIfEligible(long likeCount) {
        if (status != CourseStatus.COMMUNITY || likeCount < 10) {
            return;
        }

        this.status = CourseStatus.OFFICIAL;
        if (this.promotedAt == null) {
            this.promotedAt = LocalDateTime.now();
        }
    }

    public void updateNavigationBundleUrl(String navigationBundleUrl) {
        this.navigationBundleUrl = navigationBundleUrl;
    }

}
