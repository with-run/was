package kr.withrun.was.domain.file.controller;

import kr.withrun.was.domain.file.service.S3FileService;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FileController.class, properties = "spring.config.import=")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("파일 컨트롤러")
class FileControllerTest {

    static {
        System.setProperty("spring.cloud.aws.parameterstore.enabled", "false");
        System.setProperty("spring.config.import", "");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private S3FileService s3FileService;

    @AfterAll
    static void clearConfigOverrides() {
        System.clearProperty("spring.cloud.aws.parameterstore.enabled");
        System.clearProperty("spring.config.import");
    }

    @DisplayName("코스 스냅샷 업로드 응답을 생성 성공 포맷으로 감싼다")
    @Test
    void wrapsUploadCourseSnapshotResponseInCreatedEnvelope() throws Exception {
        long courseId = 91L;
        byte[] fileBytes = "png-image".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "snapshot.png",
                "image/png",
                fileBytes
        );
        given(s3FileService.uploadCourseSnapshot(fileBytes, courseId))
                .willReturn("course-snapshot/91.png");

        mockMvc.perform(multipart("/api/files/course-snapshots/{courseId}", courseId)
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.CREATED.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.CREATED.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

        then(s3FileService).should().uploadCourseSnapshot(fileBytes, courseId);
    }

    @DisplayName("업로드 파일이 비어 있으면 잘못된 입력 응답을 반환한다")
    @Test
    void returnsBadRequestWhenUploadFileIsEmpty() throws Exception {
        long courseId = 91L;
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.png",
                "image/png",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/files/course-snapshots/{courseId}", courseId)
                        .file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.EMPTY_FILE.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.EMPTY_FILE.getMessage()));

        then(s3FileService).shouldHaveNoInteractions();
    }
}
