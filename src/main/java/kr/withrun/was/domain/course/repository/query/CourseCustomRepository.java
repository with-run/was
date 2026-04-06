package kr.withrun.was.domain.course.repository.query;

import kr.withrun.was.domain.course.dto.PreferredDistanceRange;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.repository.query.dto.NearbyCoursePageRow;
import kr.withrun.was.domain.course.repository.query.dto.NearbyRecommendationCandidateRow;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.NearbyCourseSortBy;
import kr.withrun.was.domain.course.type.NearbyGhostCourseSortBy;

import java.util.List;
import java.util.Optional;

public interface CourseCustomRepository {

    Optional<Course> findNotDeletedCourse(Long courseId);

    List<NearbyRecommendationCandidateRow> findNearbyRecommendationCandidates(
            List<PreferredDistanceRange> preferredDistanceMs,
            double targetLatitude,
            double targetLongitude,
            double userLatitude,
            double userLongitude,
            int radiusM
    );

    List<NearbyCoursePageRow> findNearbyCoursePageRows(
            List<PreferredDistanceRange> preferredDistanceMs,
            double targetLatitude,
            double targetLongitude,
            double userLatitude,
            double userLongitude,
            CourseStatus status,
            NearbyCourseSortBy sortBy,
            int radiusM,
            int page,
            int size
    );

    List<NearbyCoursePageRow> findNearbyGhostCoursePageRows(
            List<PreferredDistanceRange> preferredDistanceMs,
            double targetLatitude,
            double targetLongitude,
            double userLatitude,
            double userLongitude,
            NearbyGhostCourseSortBy sortBy,
            int radiusM,
            int page,
            int size
    );

    long countNearbyCourses(
            List<PreferredDistanceRange> preferredDistanceMs,
            double targetLatitude,
            double targetLongitude,
            CourseStatus status,
            int radiusM
    );

    long countNearbyGhostCourses(
            List<PreferredDistanceRange> preferredDistanceMs,
            double targetLatitude,
            double targetLongitude,
            int radiusM
    );
}
