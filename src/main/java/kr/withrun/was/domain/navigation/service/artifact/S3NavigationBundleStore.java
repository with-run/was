package kr.withrun.was.domain.navigation.service.artifact;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleDocument;
import kr.withrun.was.domain.navigation.exception.NavigationBundleGenerationException;
import kr.withrun.was.domain.navigation.type.NavigationBundleFailureCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
/**
 * 생성된 네비게이션 번들 문서를 S3 객체로 저장하는 구현체입니다.
 */
public class S3NavigationBundleStore implements NavigationBundleStore {

    private final S3Client s3Client;
    private final ObjectMapper objectMapper;

    @Value("${app.s3.bucket:}")
    private String bucket;

    /**
     * S3 업로드와 JSON 직렬화에 필요한 의존성을 주입받는 생성자입니다.
     */
    public S3NavigationBundleStore(S3Client s3Client, ObjectMapper objectMapper) {
        this.s3Client = s3Client;
        this.objectMapper = objectMapper;
    }

    @Override
    /**
     * 코스별 latest navigation bundle JSON 을 S3 에 업로드하고 저장 key 를 반환합니다.
     */
    public String storeLatest(Long courseId, NavigationBundleDocument document) {
        if (!StringUtils.hasText(bucket)) {
            throw new NavigationBundleGenerationException(
                    NavigationBundleFailureCode.BUNDLE_STORAGE_FAILED,
                    "S3 bucket is not configured for navigation bundle storage"
            );
        }

        byte[] payload = serializeDocument(document);
        String objectKey = "navigation/latest/" + courseId + ".json";

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType("application/json")
                .contentLength((long) payload.length)
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromBytes(payload));
        } catch (RuntimeException exception) {
            throw new NavigationBundleGenerationException(
                    NavigationBundleFailureCode.BUNDLE_STORAGE_FAILED,
                    "Failed to store latest navigation bundle",
                    exception
            );
        }

        return objectKey;
    }

    /**
     * 번들 문서를 S3 업로드용 JSON 바이트 배열로 직렬화합니다.
     */
    private byte[] serializeDocument(NavigationBundleDocument document) {
        try {
            return objectMapper.writeValueAsBytes(document);
        } catch (JsonProcessingException exception) {
            throw new NavigationBundleGenerationException(
                    NavigationBundleFailureCode.BUNDLE_STORAGE_FAILED,
                    "Failed to serialize latest navigation bundle",
                    exception
            );
        }
    }
}
