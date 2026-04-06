package kr.withrun.was.domain.meta;

import kr.withrun.was.domain.course.controller.CourseController;
import kr.withrun.was.domain.course.dto.CourseFilterResponse;
import kr.withrun.was.domain.course.dto.CourseNavigationMetaResponse;
import kr.withrun.was.domain.course.dto.CourseRegisterMetaResponse;
import kr.withrun.was.domain.course.dto.CourseSurveyMetaResponse;
import kr.withrun.was.domain.course.service.CourseService;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {CourseMetaController.class, CourseController.class}, properties = "spring.config.import=")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("코스 메타 조회 컨트롤러")
class CourseMetaControllerTest {

    static {
        System.setProperty("spring.cloud.aws.parameterstore.enabled", "false");
        System.setProperty("spring.config.import", "");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @DisplayName("코스 등록 메타 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsCourseRegisterMetaResponseInSuccessEnvelope() throws Exception {
        CourseRegisterMetaResponse response = new CourseRegisterMetaResponse(
                List.of(
                        new CourseRegisterMetaResponse.CourseTypeOption("RIVERSIDE", "강변"),
                        new CourseRegisterMetaResponse.CourseTypeOption("PARK", "공원")
                ),
                List.of(
                        new CourseRegisterMetaResponse.ModeOption("COMMUNITY", "커뮤니티 코스"),
                        new CourseRegisterMetaResponse.ModeOption("PRIVATE", "개인 코스")
                ),
                List.of(
                        new CourseRegisterMetaResponse.RouteTypeOption("LOOP", "순환형"),
                        new CourseRegisterMetaResponse.RouteTypeOption("OUT_AND_BACK", "왕복형")
                ),
                List.of(
                        new CourseRegisterMetaResponse.DifficultyOption("EASY", "쉬움"),
                        new CourseRegisterMetaResponse.DifficultyOption("MEDIUM", "보통")
                )
        );
        given(courseService.findCourseRegisterMeta()).willReturn(response);

        mockMvc.perform(get("/api/meta/course/register"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.courseTypes[0].data").value("RIVERSIDE"))
                .andExpect(jsonPath("$.data.courseTypes[0].label").value("강변"))
                .andExpect(jsonPath("$.data.mode[0].data").value("COMMUNITY"))
                .andExpect(jsonPath("$.data.mode[0].label").value("커뮤니티 코스"))
                .andExpect(jsonPath("$.data.mode[1].data").value("PRIVATE"))
                .andExpect(jsonPath("$.data.mode[1].label").value("개인 코스"))
                .andExpect(jsonPath("$.data.routeTypes[0].data").value("LOOP"))
                .andExpect(jsonPath("$.data.routeTypes[0].label").value("순환형"))
                .andExpect(jsonPath("$.data.routeTypes[1].data").value("OUT_AND_BACK"))
                .andExpect(jsonPath("$.data.routeTypes[1].label").value("왕복형"))
                .andExpect(jsonPath("$.data.difficulties[0].data").value("EASY"))
                .andExpect(jsonPath("$.data.difficulties[0].label").value("쉬움"));

        then(courseService).should().findCourseRegisterMeta();
    }

    @DisplayName("설문 메타 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsSurveyMetaResponseInSuccessEnvelope() throws Exception {
        CourseSurveyMetaResponse response = new CourseSurveyMetaResponse(
                List.of(
                        new CourseSurveyMetaResponse.CourseTypeOption("RIVERSIDE", "강변"),
                        new CourseSurveyMetaResponse.CourseTypeOption("PARK", "공원")
                ),
                List.of(
                        new CourseSurveyMetaResponse.DifficultyOption("EASY", "쉬움"),
                        new CourseSurveyMetaResponse.DifficultyOption("MEDIUM", "보통")
                )
        );
        given(courseService.findSurveyMeta()).willReturn(response);

        mockMvc.perform(get("/api/meta/survey"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.courseTypes[0].data").value("RIVERSIDE"))
                .andExpect(jsonPath("$.data.courseTypes[0].label").value("강변"))
                .andExpect(jsonPath("$.data.difficulties[0].data").value("EASY"))
                .andExpect(jsonPath("$.data.difficulties[0].label").value("쉬움"))
                .andExpect(jsonPath("$.data.mode").doesNotExist());

        then(courseService).should().findSurveyMeta();
    }

    @DisplayName("코스 필터 응답을 meta 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsCourseFiltersResponseInSuccessEnvelopeFromMetaRoute() throws Exception {
        CourseFilterResponse response = new CourseFilterResponse(
                List.of(
                        new CourseFilterResponse.CourseDistanceTypeOption("1", "1km"),
                        new CourseFilterResponse.CourseDistanceTypeOption("3", "3km")
                ),
                List.of(
                        new CourseFilterResponse.CourseTypeOption("RIVERSIDE", "강변"),
                        new CourseFilterResponse.CourseTypeOption("PARK", "공원")
                ),
                List.of(
                        new CourseFilterResponse.DifficultyOption("EASY", "쉬움"),
                        new CourseFilterResponse.DifficultyOption("MEDIUM", "보통")
                )
        );
        given(courseService.findCourseFilters()).willReturn(response);

        mockMvc.perform(get("/api/meta/course/filter/normal-run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.courseDistanceTypes[0].data").value("1"))
                .andExpect(jsonPath("$.data.courseDistanceTypes[0].label").value("1km"))
                .andExpect(jsonPath("$.data.courseTypes[0].data").value("RIVERSIDE"))
                .andExpect(jsonPath("$.data.courseTypes[0].label").value("강변"))
                .andExpect(jsonPath("$.data.difficulties[0].data").value("EASY"))
                .andExpect(jsonPath("$.data.difficulties[0].label").value("쉬움"));

        then(courseService).should().findCourseFilters();
    }

    @DisplayName("코스 navigation 메타 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsCourseNavigationMetaResponseInSuccessEnvelope() throws Exception {
        CourseNavigationMetaResponse response = new CourseNavigationMetaResponse(
                List.of(
                        new CourseNavigationMetaResponse.NavigationOption(
                                "STRAIGHT",
                                "직진"
                        ),
                        new CourseNavigationMetaResponse.NavigationOption(
                                "RIGHT",
                                "우회전"
                        ),
                        new CourseNavigationMetaResponse.NavigationOption(
                                "LEFT",
                                "좌회전"
                        ),
                        new CourseNavigationMetaResponse.NavigationOption(
                                "UTURN",
                                "유턴"
                        ),
                        new CourseNavigationMetaResponse.NavigationOption(
                                "ARRIVAL",
                                "도착"
                        )
                )
        );
        given(courseService.findCourseNavigationMeta()).willReturn(response);

        mockMvc.perform(get("/api/meta/course/navigation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.navigations[0].data").value("STRAIGHT"))
                .andExpect(jsonPath("$.data.navigations[0].label").value("직진"))
                .andExpect(jsonPath("$.data.navigations[1].data").value("RIGHT"))
                .andExpect(jsonPath("$.data.navigations[1].label").value("우회전"))
                .andExpect(jsonPath("$.data.navigations[2].data").value("LEFT"))
                .andExpect(jsonPath("$.data.navigations[2].label").value("좌회전"))
                .andExpect(jsonPath("$.data.navigations[3].data").value("UTURN"))
                .andExpect(jsonPath("$.data.navigations[3].label").value("유턴"))
                .andExpect(jsonPath("$.data.navigations[4].data").value("ARRIVAL"))
                .andExpect(jsonPath("$.data.navigations[4].label").value("도착"));

        then(courseService).should().findCourseNavigationMeta();
    }


}
