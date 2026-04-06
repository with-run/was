package kr.withrun.was.domain.file.service;

import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Service
public class CloudFrontSignedUrlService {

    @Value("${app.cdn.base-url:}")
    private String cdnBaseUrl;

    @Value("${app.cloudfront.key-pair-id:}")
    private String keyPairId;

    @Value("${app.cloudfront.private-key:}")
    private String privateKeyPem;

    @Value("${app.cloudfront.signed-url-expiration-seconds:300}")
    private long signedUrlExpirationSeconds;

    private volatile PrivateKey cachedPrivateKey;

    public String generateSignedUrl(String objectKeyOrUrl) {
        String objectKey = normalizeObjectKey(objectKeyOrUrl);
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }

        String resourceUrl = buildResourceUrl(objectKey);
        if (!isSigningConfigured()) {
            return resourceUrl;
        }

        long expiresAtEpochSeconds = Instant.now().getEpochSecond() + Math.max(1L, signedUrlExpirationSeconds);
        String policy = createCannedPolicy(resourceUrl, expiresAtEpochSeconds);

        try {
            Signature signer = Signature.getInstance("SHA1withRSA");
            signer.initSign(loadPrivateKey());
            signer.update(policy.getBytes(StandardCharsets.UTF_8));
            String encodedSignature = toCloudFrontSafeBase64(signer.sign());

            String separator = resourceUrl.contains("?") ? "&" : "?";
            return resourceUrl
                    + separator + "Expires=" + expiresAtEpochSeconds
                    + "&Signature=" + encodedSignature
                    + "&Key-Pair-Id=" + keyPairId;
        } catch (Exception exception) {
            log.error("CloudFront signed URL 생성 실패. objectKey={}", objectKey, exception);
            throw new CustomException(ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    public String normalizeObjectKey(String objectKeyOrUrl) {
        if (!StringUtils.hasText(objectKeyOrUrl)) {
            return null;
        }

        String trimmed = objectKeyOrUrl.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            try {
                URI uri = URI.create(trimmed);
                String path = uri.getPath();
                if (!StringUtils.hasText(path)) {
                    return null;
                }
                return stripLeadingSlash(path);
            } catch (IllegalArgumentException exception) {
                log.warn("유효하지 않은 스냅샷 URL 형식입니다. value={}", trimmed);
                throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
            }
        }

        return stripLeadingSlash(trimmed);
    }

    private boolean isSigningConfigured() {
        return StringUtils.hasText(keyPairId) && StringUtils.hasText(privateKeyPem);
    }

    private String buildResourceUrl(String objectKey) {
        if (!StringUtils.hasText(cdnBaseUrl)) {
            return objectKey;
        }

        String trimmedBaseUrl = cdnBaseUrl.endsWith("/")
                ? cdnBaseUrl.substring(0, cdnBaseUrl.length() - 1)
                : cdnBaseUrl;
        return trimmedBaseUrl + "/" + stripLeadingSlash(objectKey);
    }

    private String createCannedPolicy(String resourceUrl, long expiresAtEpochSeconds) {
        return "{\"Statement\":[{\"Resource\":\""
                + resourceUrl
                + "\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":"
                + expiresAtEpochSeconds
                + "}}}]}";
    }

    private PrivateKey loadPrivateKey() throws Exception {
        PrivateKey localCachedKey = cachedPrivateKey;
        if (localCachedKey != null) {
            return localCachedKey;
        }

        synchronized (this) {
            if (cachedPrivateKey != null) {
                return cachedPrivateKey;
            }

            String normalizedPem = privateKeyPem.replace("\\n", "\n").trim();
            String pemBody = normalizedPem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decodedKey = Base64.getDecoder().decode(pemBody);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
            cachedPrivateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);
            return cachedPrivateKey;
        }
    }

    private String toCloudFrontSafeBase64(byte[] value) {
        return Base64.getEncoder()
                .encodeToString(value)
                .replace('+', '-')
                .replace('=', '_')
                .replace('/', '~');
    }

    private String stripLeadingSlash(String value) {
        String normalized = value;
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }
}
