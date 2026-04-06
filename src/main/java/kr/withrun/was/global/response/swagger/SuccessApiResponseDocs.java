package kr.withrun.was.global.response.swagger;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.course.dto.BookmarkedCoursesResponse;
import kr.withrun.was.domain.course.dto.CourseBookmarkAddResponse;
import kr.withrun.was.domain.course.dto.CourseBookmarkRemoveResponse;
import kr.withrun.was.domain.course.dto.CourseDetailResponse;
import kr.withrun.was.domain.course.dto.CourseFilterResponse;
import kr.withrun.was.domain.course.dto.CourseGhostDetailResponse;
import kr.withrun.was.domain.course.dto.CourseGhostLeaderboardResponse;
import kr.withrun.was.domain.course.dto.CourseLikeStatusResponse;
import kr.withrun.was.domain.course.dto.CourseNavigationMetaResponse;
import kr.withrun.was.domain.course.dto.CourseRegisterMetaResponse;
import kr.withrun.was.domain.course.dto.CourseSurveyMetaResponse;
import kr.withrun.was.domain.course.dto.CreateCourseReviewResponse;
import kr.withrun.was.domain.course.dto.NearbyCoursesResponse;
import kr.withrun.was.domain.course.dto.NearbyGhostCoursesResponse;
import kr.withrun.was.domain.running.dto.CreateRunningSessionDetailsResponse;
import kr.withrun.was.domain.running.dto.CreateRunningSessionResponse;
import kr.withrun.was.domain.running.dto.CreateRunningSessionSplitResponse;
import kr.withrun.was.domain.running.dto.GhostRunningResultResponse;
import kr.withrun.was.domain.running.dto.PastRunningSessionsResponse;
import kr.withrun.was.domain.running.dto.RegisterRunningSessionCourseResponse;
import kr.withrun.was.domain.running.dto.RunningSessionDetailResponse;
import kr.withrun.was.domain.user.dto.UserCalendarMonthlyResponse;
import kr.withrun.was.domain.user.dto.UserCalendarSummaryResponse;
import kr.withrun.was.domain.user.dto.UserCalendarWeeklyResponse;
import kr.withrun.was.domain.auth.dto.AuthMeResponse;
import kr.withrun.was.domain.auth.dto.ReissueAccessTokenResponse;

public final class SuccessApiResponseDocs {

    private SuccessApiResponseDocs() {
    }

    @Schema(name = "CourseRegisterMetaApiResponse", description = "코스 등록 메타 조회 성공 응답")
    public record CourseRegisterMetaApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "코스 등록 메타 응답 데이터")
            CourseRegisterMetaResponse data
    ) {
    }

    @Schema(name = "CourseSurveyMetaApiResponse", description = "설문 메타 조회 성공 응답")
    public record CourseSurveyMetaApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "설문 메타 응답 데이터")
            CourseSurveyMetaResponse data
    ) {
    }

    @Schema(name = "CourseFilterApiResponse", description = "코스 필터 조회 성공 응답")
    public record CourseFilterApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "코스 필터 응답 데이터")
            CourseFilterResponse data
    ) {
    }

    @Schema(name = "CourseNavigationMetaApiResponse", description = "코스 navigation 메타 조회 성공 응답")
    public record CourseNavigationMetaApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "코스 navigation 메타 응답 데이터")
            CourseNavigationMetaResponse data
    ) {
    }

    @Schema(name = "CourseDetailApiResponse", description = "코스 상세 조회 성공 응답")
    public record CourseDetailApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "코스 상세 데이터")
            CourseDetailResponse data
    ) {
    }

    @Schema(name = "CourseGhostDetailApiResponse", description = "코스 고스트 상세 조회 성공 응답")
    public record CourseGhostDetailApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "코스 고스트 상세 데이터")
            CourseGhostDetailResponse data
    ) {
    }

    @Schema(name = "CourseGhostLeaderboardApiResponse", description = "코스 고스트 리더보드 조회 성공 응답")
    public record CourseGhostLeaderboardApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "코스 고스트 리더보드 데이터")
            CourseGhostLeaderboardResponse data
    ) {
    }

    @Schema(name = "NearbyCoursesApiResponse", description = "주변 코스 조회 성공 응답")
    public record NearbyCoursesApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "주변 코스 목록 데이터")
            NearbyCoursesResponse data
    ) {
    }

    @Schema(name = "RecommendedNearbyCoursesApiResponse", description = "추천 코스 조회 성공 응답")
    public record RecommendedNearbyCoursesApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "추천 코스 목록 데이터")
            NearbyCoursesResponse data
    ) {
    }

    @Schema(name = "NearbyGhostCoursesApiResponse", description = "주변 고스트 런 코스 조회 성공 응답")
    public record NearbyGhostCoursesApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "주변 고스트 런 코스 목록 데이터")
            NearbyGhostCoursesResponse data
    ) {
    }

    @Schema(name = "CourseBookmarkAddApiResponse", description = "코스 북마크 추가 성공 응답")
    public record CourseBookmarkAddApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "북마크 추가 결과 데이터")
            CourseBookmarkAddResponse data
    ) {
    }

    @Schema(name = "CourseBookmarkRemoveApiResponse", description = "코스 북마크 해제 성공 응답")
    public record CourseBookmarkRemoveApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "북마크 해제 결과 데이터")
            CourseBookmarkRemoveResponse data
    ) {
    }

    @Schema(name = "BookmarkedCoursesApiResponse", description = "북마크 목록 조회 성공 응답")
    public record BookmarkedCoursesApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "북마크한 코스 목록 데이터")
            BookmarkedCoursesResponse data
    ) {
    }

    @Schema(name = "CourseLikeStatusApiResponse", description = "코스 좋아요 상태 변경 성공 응답")
    public record CourseLikeStatusApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "좋아요 상태 데이터")
            CourseLikeStatusResponse data
    ) {
    }

    @Schema(name = "CreateCourseReviewApiResponse", description = "코스 리뷰 작성 성공 응답")
    public record CreateCourseReviewApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S002")
            String code,
            @Schema(description = "성공 응답 메시지", example = "리소스가 생성되었습니다.")
            String message,
            @Schema(description = "생성된 리뷰 데이터")
            CreateCourseReviewResponse data
    ) {
    }

    @Schema(name = "CreateRunningSessionApiResponse", description = "러닝 세션 생성 성공 응답")
    public record CreateRunningSessionApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S002")
            String code,
            @Schema(description = "성공 응답 메시지", example = "리소스가 생성되었습니다.")
            String message,
            @Schema(description = "생성된 러닝 세션 데이터")
            CreateRunningSessionResponse data
    ) {
    }

    @Schema(name = "CreateRunningSessionDetailsApiResponse", description = "러닝 세션 상세 데이터 저장 성공 응답")
    public record CreateRunningSessionDetailsApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S002")
            String code,
            @Schema(description = "성공 응답 메시지", example = "리소스가 생성되었습니다.")
            String message,
            @Schema(description = "저장된 러닝 세션 상세 데이터")
            CreateRunningSessionDetailsResponse data
    ) {
    }

    @Schema(name = "GhostRunningResultApiResponse", description = "고스트 러닝 결과 조회 성공 응답")
    public record GhostRunningResultApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "고스트 러닝 결과 데이터")
            GhostRunningResultResponse data
    ) {
    }

    @Schema(name = "RunningSessionDetailApiResponse", description = "러닝 세션 상세 조회 성공 응답")
    public record RunningSessionDetailApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "러닝 세션 상세 데이터")
            RunningSessionDetailResponse data
    ) {
    }

    @Schema(name = "PastRunningSessionsApiResponse", description = "과거 러닝 세션 조회 성공 응답")
    public record PastRunningSessionsApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "과거 러닝 세션 목록 데이터")
            PastRunningSessionsResponse data
    ) {
    }

    @Schema(name = "RegisterRunningSessionCourseApiResponse", description = "러닝 기록 코스 등록 성공 응답")
    public record RegisterRunningSessionCourseApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S002")
            String code,
            @Schema(description = "성공 응답 메시지", example = "리소스가 생성되었습니다.")
            String message,
            @Schema(description = "등록된 코스 데이터")
            RegisterRunningSessionCourseResponse data
    ) {
    }

    @Schema(name = "CreatedApiResponse", description = "생성 성공 공통 응답")
    public record CreatedApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S002")
            String code,
            @Schema(description = "성공 응답 메시지", example = "리소스가 생성되었습니다.")
            String message
    ) {
    }

    @Schema(name = "CreateRunningSessionSplitApiResponse", description = "러닝 세션 구간 기록 저장 성공 응답")
    public record CreateRunningSessionSplitApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S002")
            String code,
            @Schema(description = "성공 응답 메시지", example = "리소스가 생성되었습니다.")
            String message,
            @Schema(description = "저장된 구간 기록 데이터")
            CreateRunningSessionSplitResponse data
    ) {
    }

    @Schema(name = "UserCalendarMonthlyApiResponse", description = "월간 캘린더 조회 성공 응답")
    public record UserCalendarMonthlyApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "월간 캘린더 데이터")
            UserCalendarMonthlyResponse data
    ) {
    }

    @Schema(name = "UserCalendarSummaryApiResponse", description = "사용자 러닝 요약 조회 성공 응답")
    public record UserCalendarSummaryApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "누적 러닝 요약 데이터")
            UserCalendarSummaryResponse data
    ) {
    }

    @Schema(name = "UserCalendarWeeklyApiResponse", description = "사용자 주간 러닝 요약 조회 성공 응답")
    public record UserCalendarWeeklyApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "주간 러닝 요약 데이터")
            UserCalendarWeeklyResponse data
    ) {
    }

        @Schema(name = "AuthMeApiResponse", description = "현재 로그인 사용자 조회 성공 응답")
    public record AuthMeApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "현재 로그인 사용자 데이터")
            AuthMeResponse data
    ) {
    }

    @Schema(name = "ReissueAccessTokenApiResponse", description = "access token 재발급 성공 응답")
    public record ReissueAccessTokenApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S001")
            String code,
            @Schema(description = "성공 응답 메시지", example = "요청이 성공했습니다.")
            String message,
            @Schema(description = "재발급된 access token 데이터")
            ReissueAccessTokenResponse data
    ) {
    }

    @Schema(name = "LogoutApiResponse", description = "로그아웃 성공 응답")
    public record LogoutApiResponse(
            @Schema(description = "요청 성공 여부", example = "true")
            boolean success,
            @Schema(description = "성공 응답 코드", example = "S102")
            String code,
            @Schema(description = "성공 응답 메시지", example = "로그아웃이 완료되었습니다.")
            String message
    ) {
    }
}
