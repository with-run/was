# Private Course 접근 제어 통합 구현 계획

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** `CourseStatus.PRIVATE` 코스에 대해 모든 direct read/list/mutate 흐름에서 동일한 owner-only 규칙을 적용하고, private 존재를 숨겨야 하는 query 흐름에서도 동일한 정책을 유지한다.

**Architecture:** direct read 와 mutation 은 service-layer 공통 access helper 로 통제하고, 목록/탐색/추천 계열은 query 단계에서 `OFFICIAL`/`COMMUNITY`만 노출되도록 필터링한다. `RunningSessionService`의 기존 private-course 규칙을 의미 기준으로 삼고, `CourseService`/`CourseLikeService`/`CourseBookmarkService`/`CourseReviewService`와 recommendation interaction query 까지 같은 기준으로 정렬한다.

**Tech Stack:** Spring Boot 3.5, Spring Security, Spring Data JPA, Querydsl, JUnit 5, Mockito, springdoc OpenAPI

---

## 정책 결정

- `OFFICIAL`, `COMMUNITY` 코스는 인증 사용자라면 조회 및 상호작용 가능하다.
- `PRIVATE` 코스는 코스 소유자만 direct read/detail/ghost/leaderboard/like/bookmark/review/running 에 접근 가능하다.
- 비소유자의 `PRIVATE` 코스 접근은 존재 노출을 막기 위해 `ResponseCode.COURSE_NOT_FOUND`로 처리한다.
- 목록/탐색형 API는 `Option A`를 채택한다. 즉 `PRIVATE` 코스는 소유자에게도 `nearby`, `recommended`, `nearby-ghost`, `bookmark-list` 같은 feed 에서 노출하지 않는다.
- recommendation seed / collaborative score 계산에서도 private-course interaction 은 제외한다.
- 제품이 나중에 소유자 전용 private 목록이 필요하다고 판단하면, 기존 feed 를 바꾸지 않고 별도 `my private courses` 성격의 전용 API 를 추가한다.

## 기준 파일

- 의미 기준 / 기존 정책 앵커
  - `src/main/java/kr/withrun/was/domain/running/service/RunningSessionService.java`
  - `src/test/java/kr/withrun/was/domain/running/service/RunningSessionServiceTest.java`
- direct read / mutation 대상
  - `src/main/java/kr/withrun/was/domain/course/service/CourseService.java`
  - `src/main/java/kr/withrun/was/domain/course/service/CourseLikeService.java`
  - `src/main/java/kr/withrun/was/domain/course/service/CourseBookmarkService.java`
  - `src/main/java/kr/withrun/was/domain/course/service/CourseReviewService.java`
- query / recommendation 대상
  - `src/main/java/kr/withrun/was/domain/course/repository/query/CourseCustomRepositoryImpl.java`
  - `src/main/java/kr/withrun/was/domain/course/repository/query/CourseBookmarkCustomRepositoryImpl.java`
  - `src/main/java/kr/withrun/was/domain/course/repository/query/UserCourseInteractionCustomRepositoryImpl.java`
  - `src/main/java/kr/withrun/was/domain/course/service/recommendation/HybridRecommendationService.java`
- controller / documentation 대상
  - `src/main/java/kr/withrun/was/domain/course/controller/CourseController.java`
  - `src/main/java/kr/withrun/was/domain/course/controller/CourseBookmarkController.java`
  - `src/main/java/kr/withrun/was/domain/course/controller/CourseLikeController.java`
  - `src/main/java/kr/withrun/was/domain/course/controller/CourseReviewController.java`
  - `src/main/java/kr/withrun/was/domain/course/dto/CourseDetailResponse.java`
  - `src/main/java/kr/withrun/was/domain/course/dto/CourseGhostDetailResponse.java`

## 구현 원칙

- direct read 와 mutation 의 접근 제어는 controller 에 중복하지 않고 service-layer 공통 helper 로 모은다.
- raw `findNotDeletedCourse(courseId)`를 쓰는 course domain 서비스는 private 접근 제어를 helper 없이 우회하지 않게 정리한다.
- paged/list/recommendation 계열은 query 단계에서 private row 자체를 제거한다.
- `RunningSessionService`의 기존 private-course 판정 규칙은 그대로 유지하고, 다른 서비스가 그 규칙을 재사용하도록 바꾼다.
- `@PostAuthorize` 같은 사후 판정보다 service helper 와 query predicate 를 우선한다.
- controller 는 `@AuthenticationPrincipal AuthenticatedUser`로 current user 를 받아 service 로 전달하는 역할만 맡는다.

### Task 1: direct read 계약을 테스트로 고정

**Files:**
- Modify: `src/test/java/kr/withrun/was/domain/course/service/CourseServiceTest.java`
- Modify: `src/test/java/kr/withrun/was/domain/course/controller/CourseControllerTest.java`

**Step 1: Write the failing test**

- `CourseServiceTest`에 다음 케이스를 추가한다.
  - owner 는 자신의 `PRIVATE` 코스 detail / ghost-detail / leaderboard 를 조회할 수 있다.
  - non-owner 는 같은 요청에서 `COURSE_NOT_FOUND`를 받는다.
- `CourseControllerTest`에 다음 케이스를 추가한다.
  - `GET /api/courses/{courseId}` 는 principal 을 service 까지 전달한다.
  - `GET /api/courses/{courseId}/ghost-detail` 는 principal 을 service 까지 전달한다.
  - `GET /api/courses/{courseId}/ghost-leaderboard` 는 principal 을 service 까지 전달한다.
  - non-owner private 접근이 404 로 매핑된다.

**Step 2: Run test to verify it fails**

Run:

```bash
./gradlew test --tests "kr.withrun.was.domain.course.service.CourseServiceTest" --tests "kr.withrun.was.domain.course.controller.CourseControllerTest"
```

Expected: `PRIVATE` direct read 관련 신규 테스트가 시그니처 mismatch 또는 권한 검증 누락으로 실패한다.

**Step 3: Write minimal implementation**

- 아직 구현하지 않는다. 이 Task 는 direct read 계약과 controller principal 전달 요구를 실패 테스트로 고정하는 데 집중한다.

**Step 4: Run test to verify it still fails for the expected reason**

- 같은 명령을 다시 실행해서, 실패 원인이 private 접근 정책 미구현인지 확인한다.

**Step 5: Commit**

```bash
git add src/test/java/kr/withrun/was/domain/course/service/CourseServiceTest.java src/test/java/kr/withrun/was/domain/course/controller/CourseControllerTest.java
git commit -m "test(was) : private 코스 direct read 접근 제어 계약 추가"
```

### Task 2: 공통 CourseAccessPolicy 도입

**Files:**
- Create: `src/main/java/kr/withrun/was/domain/course/service/CourseAccessPolicy.java`
- Modify: `src/main/java/kr/withrun/was/domain/running/service/RunningSessionService.java`
- Modify: `src/main/java/kr/withrun/was/domain/course/service/CourseService.java`
- Test: `src/test/java/kr/withrun/was/domain/running/service/RunningSessionServiceTest.java`

**Step 1: Write the failing test**

- `RunningSessionServiceTest`에 helper 분리 이후에도 기존 private-course semantics 가 그대로 유지된다는 regression assertion 이 부족하면 최소 단위로 보강한다.
- 이미 owner / non-owner private semantics 를 충분히 고정하고 있다면, 기존 테스트를 회귀 앵커로 삼고 새 테스트는 helper 연동 포인트만 추가한다.

**Step 2: Run test to verify it fails**

Run:

```bash
./gradlew test --tests "kr.withrun.was.domain.running.service.RunningSessionServiceTest" --tests "kr.withrun.was.domain.course.service.CourseServiceTest"
```

Expected: helper 분리 전이라 direct read 테스트는 아직 실패하고, running semantics 는 회귀 앵커로 유지된다.

**Step 3: Write minimal implementation**

- `CourseAccessPolicy`에 다음 메서드를 만든다.
  - `boolean canAccess(Course course, Long currentUserId)`
  - `Course getAccessibleCourse(CourseRepository courseRepository, Long courseId, Long currentUserId)` 또는 repository 비의존 helper + service wrapper
- 의미는 `RunningSessionService`의 기존 `PRIVATE` owner-only 규칙과 동일해야 한다.
- `RunningSessionService`는 직접 status 체크 대신 새 helper 를 사용하게 바꾼다.

**Step 4: Run test to verify it passes**

Run:

```bash
./gradlew test --tests "kr.withrun.was.domain.running.service.RunningSessionServiceTest" --tests "kr.withrun.was.domain.course.service.CourseServiceTest"
```

Expected: running private-course regression 이 유지되고, direct read 테스트 일부가 green 으로 전환된다.

**Step 5: Commit**

```bash
git add src/main/java/kr/withrun/was/domain/course/service/CourseAccessPolicy.java src/main/java/kr/withrun/was/domain/running/service/RunningSessionService.java src/main/java/kr/withrun/was/domain/course/service/CourseService.java src/test/java/kr/withrun/was/domain/running/service/RunningSessionServiceTest.java
git commit -m "refactor(was) : private 코스 접근 정책을 공통 helper 로 분리"
```

### Task 3: CourseService direct read 와 controller principal 연결

**Files:**
- Modify: `src/main/java/kr/withrun/was/domain/course/service/CourseService.java`
- Modify: `src/main/java/kr/withrun/was/domain/course/controller/CourseController.java`
- Modify: `src/test/java/kr/withrun/was/domain/course/service/CourseServiceTest.java`
- Modify: `src/test/java/kr/withrun/was/domain/course/controller/CourseControllerTest.java`

**Step 1: Write the failing test**

- `findCourseDetail`가 current user id 를 받도록 테스트를 먼저 변경한다.
- `findCourseGhostDetail`도 current user id 를 받도록 테스트를 먼저 변경한다.
- `findCourseGhostLeaderboard`도 current user id 를 받도록 테스트를 먼저 변경한다.

**Step 2: Run test to verify it fails**

Run:

```bash
./gradlew test --tests "kr.withrun.was.domain.course.controller.CourseControllerTest" --tests "kr.withrun.was.domain.course.service.CourseServiceTest"
```

Expected: 시그니처 mismatch 또는 권한 검증 누락으로 실패한다.

**Step 3: Write minimal implementation**

- `CourseController`에서 다음 엔드포인트에 `@AuthenticationPrincipal AuthenticatedUser`를 연결한다.
  - `GET /api/courses/{courseId}`
  - `GET /api/courses/{courseId}/ghost-detail`
  - `GET /api/courses/{courseId}/ghost-leaderboard`
- `CourseService`에서 다음 메서드를 user-aware 시그니처로 바꾼다.
  - `findCourseDetail(Long courseId, Long currentUserId)`
  - `findCourseGhostDetail(Long courseId, Long currentUserId)`
  - `findCourseGhostLeaderboard(Long courseId, Long currentUserId, CourseGhostLeaderboardRequest request)`
- `PRIVATE`인 경우 공통 helper 를 통해 owner-only 로 막는다.
- detail / ghost-detail 의 `isLiked`, `isBookmarked`를 더 이상 하드코딩 `false`로 두지 말고 실제 repository 조회로 채운다.

**Step 4: Run test to verify it passes**

- 같은 명령을 다시 실행한다.

**Step 5: Commit**

```bash
git add src/main/java/kr/withrun/was/domain/course/service/CourseService.java src/main/java/kr/withrun/was/domain/course/controller/CourseController.java src/test/java/kr/withrun/was/domain/course/service/CourseServiceTest.java src/test/java/kr/withrun/was/domain/course/controller/CourseControllerTest.java
git commit -m "fix(was) : private 코스 direct read 를 owner 전용으로 통일"
```

### Task 4: like, bookmark, review mutation 접근 제어 통일

**Files:**
- Modify: `src/main/java/kr/withrun/was/domain/course/service/CourseLikeService.java`
- Modify: `src/main/java/kr/withrun/was/domain/course/service/CourseBookmarkService.java`
- Modify: `src/main/java/kr/withrun/was/domain/course/service/CourseReviewService.java`
- Modify: `src/test/java/kr/withrun/was/domain/course/service/CourseLikeServiceTest.java`
- Modify: `src/test/java/kr/withrun/was/domain/course/service/CourseBookmarkServiceTest.java`
- Modify: `src/test/java/kr/withrun/was/domain/course/service/CourseReviewServiceTest.java`
- Modify: `src/test/java/kr/withrun/was/domain/course/controller/CourseLikeControllerTest.java`
- Modify: `src/test/java/kr/withrun/was/domain/course/controller/CourseBookmarkControllerTest.java`
- Modify: `src/test/java/kr/withrun/was/domain/course/controller/CourseReviewControllerTest.java`

**Step 1: Write the failing test**

- owner 는 자신의 `PRIVATE` 코스에 like / bookmark / review 가능
- non-owner 는 모두 `COURSE_NOT_FOUND`
- controller 는 private non-owner 실패를 404 로 노출

**Step 2: Run test to verify it fails**

Run:

```bash
./gradlew test --tests "kr.withrun.was.domain.course.service.CourseLikeServiceTest" --tests "kr.withrun.was.domain.course.service.CourseBookmarkServiceTest" --tests "kr.withrun.was.domain.course.service.CourseReviewServiceTest" --tests "kr.withrun.was.domain.course.controller.CourseLikeControllerTest" --tests "kr.withrun.was.domain.course.controller.CourseBookmarkControllerTest" --tests "kr.withrun.was.domain.course.controller.CourseReviewControllerTest"
```

Expected: private owner / non-owner mutation 케이스가 실패한다.

**Step 3: Write minimal implementation**

- 세 service 의 `getCourse(...)` 경로를 공통 helper 기반으로 바꾼다.
- mutation 전에 먼저 access check 가 끝나도록 순서를 유지한다.

**Step 4: Run test to verify it passes**

- 같은 명령을 다시 실행한다.

**Step 5: Commit**

```bash
git add src/main/java/kr/withrun/was/domain/course/service/CourseLikeService.java src/main/java/kr/withrun/was/domain/course/service/CourseBookmarkService.java src/main/java/kr/withrun/was/domain/course/service/CourseReviewService.java src/test/java/kr/withrun/was/domain/course/service/CourseLikeServiceTest.java src/test/java/kr/withrun/was/domain/course/service/CourseBookmarkServiceTest.java src/test/java/kr/withrun/was/domain/course/service/CourseReviewServiceTest.java src/test/java/kr/withrun/was/domain/course/controller/CourseLikeControllerTest.java src/test/java/kr/withrun/was/domain/course/controller/CourseBookmarkControllerTest.java src/test/java/kr/withrun/was/domain/course/controller/CourseReviewControllerTest.java
git commit -m "fix(was) : private 코스 상호작용을 owner 전용으로 제한"
```

### Task 5: nearby / recommended / ghost / bookmark-list row 필터 정리

**Files:**
- Modify: `src/main/java/kr/withrun/was/domain/course/repository/query/CourseCustomRepositoryImpl.java`
- Modify: `src/main/java/kr/withrun/was/domain/course/repository/query/CourseBookmarkCustomRepositoryImpl.java`
- Modify: `src/test/java/kr/withrun/was/domain/course/repository/CourseRepositoryRowTest.java`
- Modify: `src/test/java/kr/withrun/was/domain/course/repository/CourseBookmarkRepositoryTest.java`
- Modify: `src/test/java/kr/withrun/was/domain/course/service/NearbyCourseQueryServiceTest.java`

**Step 1: Write the failing test**

- nearby / nearby recommended / nearby ghost 결과에서 `PRIVATE`가 나오지 않아야 한다.
- bookmark list 는 owner 에게도 `PRIVATE`가 나오지 않아야 한다.
- 기존 `CourseBookmarkRepositoryTest`가 이미 이 정책을 일부 고정하고 있으면, 중복 대신 같은 기준을 `CourseRepositoryRowTest`와 `NearbyCourseQueryServiceTest`로 확장한다.

**Step 2: Run test to verify it fails**

Run:

```bash
./gradlew test --tests "kr.withrun.was.domain.course.repository.CourseRepositoryRowTest" --tests "kr.withrun.was.domain.course.repository.CourseBookmarkRepositoryTest" --tests "kr.withrun.was.domain.course.service.NearbyCourseQueryServiceTest"
```

Expected: private row filtering 관련 신규 테스트가 실패한다.

**Step 3: Write minimal implementation**

- `CourseCustomRepositoryImpl` 공통 nearby predicate 에 `OFFICIAL` / `COMMUNITY` status 조건을 추가한다.
- `CourseBookmarkCustomRepositoryImpl`의 public-only 조건을 명시적 helper / predicate 로 정리한다.
- service 응답 계층에서도 private row 가 새지 않도록 기존 query 결과 사용부를 확인한다.

**Step 4: Run test to verify it passes**

- 같은 명령을 다시 실행한다.

**Step 5: Commit**

```bash
git add src/main/java/kr/withrun/was/domain/course/repository/query/CourseCustomRepositoryImpl.java src/main/java/kr/withrun/was/domain/course/repository/query/CourseBookmarkCustomRepositoryImpl.java src/test/java/kr/withrun/was/domain/course/repository/CourseRepositoryRowTest.java src/test/java/kr/withrun/was/domain/course/repository/CourseBookmarkRepositoryTest.java src/test/java/kr/withrun/was/domain/course/service/NearbyCourseQueryServiceTest.java
git commit -m "fix(was) : private 코스를 nearby feed 에서 제외"
```

### Task 6: recommendation interaction 필터 분리

**Files:**
- Modify: `src/main/java/kr/withrun/was/domain/course/repository/query/UserCourseInteractionCustomRepositoryImpl.java`
- Modify: `src/main/java/kr/withrun/was/domain/course/service/recommendation/HybridRecommendationService.java`
- Create: `src/test/java/kr/withrun/was/domain/course/repository/UserCourseInteractionRepositoryTest.java`
- Optional modify: `src/test/java/kr/withrun/was/domain/course/service/recommendation/HybridRecommendationServiceTest.java`

**Step 1: Write the failing test**

- recent interacted course ids 에서 private-course interaction 이 제외되어야 한다.
- collaborative score 계산 input 에서 private-course interaction 이 제외되어야 한다.
- 기존 `NearbyCourseQueryServiceTest`는 mocked recommendation 조합이라 query-level filtering 증명용으로는 부족하므로, repository 전용 테스트를 새로 만든다.

**Step 2: Run test to verify it fails**

Run:

```bash
./gradlew test --tests "kr.withrun.was.domain.course.repository.UserCourseInteractionRepositoryTest"
```

Expected: private interaction filtering 관련 신규 테스트가 실패한다.

**Step 3: Write minimal implementation**

- `UserCourseInteractionCustomRepositoryImpl`에서 interaction query 가 `PRIVATE` 코스를 조인 단계에서 제외하도록 바꾼다.
- `HybridRecommendationService`는 signature 변경 없이 동작 유지가 가능하면 그대로 두고, 필요 시 테스트 정합성만 맞춘다.

**Step 4: Run test to verify it passes**

- 같은 명령을 다시 실행한다.

**Step 5: Commit**

```bash
git add src/main/java/kr/withrun/was/domain/course/repository/query/UserCourseInteractionCustomRepositoryImpl.java src/main/java/kr/withrun/was/domain/course/service/recommendation/HybridRecommendationService.java src/test/java/kr/withrun/was/domain/course/repository/UserCourseInteractionRepositoryTest.java
git commit -m "fix(was) : private 코스 interaction 을 추천 입력에서 제외"
```

### Task 7: Swagger, DTO 설명, stale comment 정리

**Files:**
- Modify: `src/main/java/kr/withrun/was/domain/course/controller/CourseController.java`
- Modify: `src/main/java/kr/withrun/was/domain/course/controller/CourseBookmarkController.java`
- Modify: `src/main/java/kr/withrun/was/domain/course/controller/CourseLikeController.java`
- Modify: `src/main/java/kr/withrun/was/domain/course/controller/CourseReviewController.java`
- Modify: `src/main/java/kr/withrun/was/domain/course/dto/CourseDetailResponse.java`
- Modify: `src/main/java/kr/withrun/was/domain/course/dto/CourseGhostDetailResponse.java`

**Step 1: Write the failing test**

- 별도 자동 테스트가 없다면 현재 문구와 구현이 불일치하는 항목을 체크리스트로 정리한다.

**Step 2: Run closest verification**

Run:

```bash
./gradlew test --tests "kr.withrun.was.WasApplicationTests"
```

Expected: 문서 관련 변경으로 application context regression 이 없다.

**Step 3: Write minimal implementation**

- `isLiked`, `isBookmarked`가 “항상 false”라는 설명을 제거한다.
- `myRecord` 설명을 “현재 사용자가 기록이 없으면 null”로 수정한다.
- detail / ghost / leaderboard / like / bookmark / review endpoint 설명에 private owner-only 및 비소유자 404 정책을 반영한다.
- bookmark list 설명에 public-only feed 정책을 반영한다.

**Step 4: Run verification**

- 같은 명령을 다시 실행한다.

**Step 5: Commit**

```bash
git add src/main/java/kr/withrun/was/domain/course/controller/CourseController.java src/main/java/kr/withrun/was/domain/course/controller/CourseBookmarkController.java src/main/java/kr/withrun/was/domain/course/controller/CourseLikeController.java src/main/java/kr/withrun/was/domain/course/controller/CourseReviewController.java src/main/java/kr/withrun/was/domain/course/dto/CourseDetailResponse.java src/main/java/kr/withrun/was/domain/course/dto/CourseGhostDetailResponse.java
git commit -m "docs(was) : private 코스 접근 정책에 맞게 문서를 정리"
```

## 병렬 작업 분리안

- `CourseAccessPolicy` 계약이 정해진 뒤에는 세 갈래로 나눌 수 있다.
  - A 트랙: `CourseService`, `CourseLikeService`, `CourseBookmarkService`, `CourseReviewService`, 관련 controller 와 direct read / mutation 테스트
  - B 트랙: `CourseCustomRepositoryImpl`, `CourseBookmarkCustomRepositoryImpl`, nearby / ghost / bookmark-list query 테스트
  - C 트랙: `UserCourseInteractionCustomRepositoryImpl`, recommendation interaction 테스트
- Swagger / DTO 정리와 최종 검증은 세 트랙 병합 후 수행한다.

## 추가 테스트 커버리지

- 이미 좋은 회귀 앵커로 존재하는 테스트
  - `src/test/java/kr/withrun/was/domain/running/service/RunningSessionServiceTest.java`
    - owner / non-owner private running semantics 유지 확인
  - `src/test/java/kr/withrun/was/domain/course/repository/CourseBookmarkRepositoryTest.java`
    - bookmark list 에 private row 미포함 기준 유지
- `src/test/java/kr/withrun/was/domain/course/service/CourseServiceTest.java`
  - owner 가 자신의 private detail, ghost-detail, leaderboard 를 조회 가능
  - non-owner 는 같은 요청에서 `COURSE_NOT_FOUND`
- `src/test/java/kr/withrun/was/domain/course/controller/CourseControllerTest.java`
  - detail, ghost-detail, leaderboard endpoint 가 current user id 를 service 로 전달
  - non-owner private 접근이 404 로 매핑됨
- `src/test/java/kr/withrun/was/domain/course/service/CourseLikeServiceTest.java`
  - owner 는 private 코스 like / unlike 가능
  - non-owner 는 `COURSE_NOT_FOUND`
- `src/test/java/kr/withrun/was/domain/course/service/CourseBookmarkServiceTest.java`
  - owner 는 private 코스 bookmark add / remove 가능
  - non-owner 는 `COURSE_NOT_FOUND`
- `src/test/java/kr/withrun/was/domain/course/service/CourseReviewServiceTest.java`
  - owner 는 private 코스 review 생성 가능
  - non-owner 는 `COURSE_NOT_FOUND`
- `src/test/java/kr/withrun/was/domain/course/repository/CourseRepositoryRowTest.java`
  - nearby / recommended / ghost query 에 private row 미포함
- `src/test/java/kr/withrun/was/domain/course/service/NearbyCourseQueryServiceTest.java`
  - service 응답 계층에서도 private row 미포함
- `src/test/java/kr/withrun/was/domain/course/repository/UserCourseInteractionRepositoryTest.java`
  - private-course interaction 이 recommendation seed / collaborative score 에 영향을 주지 않음

## 최종 검증

Run:

```bash
./gradlew test --tests "kr.withrun.was.domain.course.service.CourseServiceTest" --tests "kr.withrun.was.domain.course.controller.CourseControllerTest" --tests "kr.withrun.was.domain.course.service.CourseLikeServiceTest" --tests "kr.withrun.was.domain.course.service.CourseBookmarkServiceTest" --tests "kr.withrun.was.domain.course.service.CourseReviewServiceTest" --tests "kr.withrun.was.domain.course.repository.CourseRepositoryRowTest" --tests "kr.withrun.was.domain.course.repository.CourseBookmarkRepositoryTest" --tests "kr.withrun.was.domain.course.service.NearbyCourseQueryServiceTest" --tests "kr.withrun.was.domain.course.repository.UserCourseInteractionRepositoryTest" --tests "kr.withrun.was.domain.running.service.RunningSessionServiceTest"
./gradlew test
```

Expected:

- private direct read / mutate 는 owner-only 로 통일된다.
- list / recommend / bookmark-list 에서 private row 가 노출되지 않는다.
- recommendation seed / collaborative input 에서 private interaction 이 제거된다.
- `RunningSessionService` 기존 의미가 유지된다.
- Swagger / DTO 설명과 실제 동작이 일치한다.

## 후속 검토 항목

- 제품 요구가 바뀌어 owner-visible private list 가 필요해지면, 기존 nearby / bookmark-list 를 바꾸지 말고 별도 `my private courses` feed 를 설계한다.
