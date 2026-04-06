package kr.withrun.was.domain.file.service;

import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3 파일 서비스 리워드 아이템 업로드")
class S3FileServiceRewardItemTest {

    @Mock
    private S3Client s3Client;

    private S3FileService s3FileService;

    @BeforeEach
    void setUp() {
        s3FileService = new S3FileService(s3Client);
        setField(s3FileService, "bucket", "test-bucket");
    }

    @DisplayName("key prefix가 reward-item/으로 생성된다")
    @Test
    void createsRewardItemKeyWithExpectedPrefix() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String objectKey = s3FileService.uploadRewardItemImage("image-bytes".getBytes(), 15L);

        assertThat(objectKey).startsWith("reward-item/");
    }

    @DisplayName("reward item id 기반 파일명이 생성된다")
    @Test
    void createsRewardItemKeyWithRewardItemId() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        String objectKey = s3FileService.uploadRewardItemImage("image-bytes".getBytes(), 27L);

        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertThat(objectKey).isEqualTo("reward-item/27.png");
        assertThat(requestCaptor.getValue().key()).isEqualTo("reward-item/27.png");
    }

    @DisplayName("빈 바이트 업로드는 허용하지 않는다")
    @Test
    void rejectsEmptyRewardItemImageBytes() {
        assertThatThrownBy(() -> s3FileService.uploadRewardItemImage(new byte[0], 3L))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.EMPTY_FILE);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to set field " + fieldName, exception);
        }
    }
}
