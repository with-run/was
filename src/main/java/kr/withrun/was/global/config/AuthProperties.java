package kr.withrun.was.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Getter란 속성값을 가져오는 메서드를 자동으로 생성해준다.
// Setter란 속성값을 설정하는 메서드를 자동으로 생성해준다.
// Component란 스프링 빈으로 등록한다. new로 생성하지 않고 스프링이 관리한다.
// ConfigurationProperties란 application.yml 파일에서 app.auth로 시작하는 속성을 주입받는다.
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private String frontendBaseUrl;
    private String mobileDeepLinkBase;
    private List<String> allowedOrigins = new ArrayList<>();

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null
                ? new ArrayList<>()
                : allowedOrigins.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(origin -> !origin.isBlank())
                        .toList();
    }
}
