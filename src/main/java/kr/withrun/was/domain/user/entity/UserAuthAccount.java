package kr.withrun.was.domain.user.entity;

// User와 소셜 로그인 계정 정보를 연결해서 저장하는 JPA 엔티티 파일
// 어떤 사용자와 연결됐는지, 구글인지 카카오인지, 그 소셜 서비스 안에서 고유 ID가 뭔지를 담음
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.withrun.was.global.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "user_auth_accounts",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_auth_accounts_provider_identity",
                columnNames = {"provider", "provider_user_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAuthAccount extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private Long id;

    @Column(name = "provider", nullable = false, length = 16)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 191)
    private String providerUserId;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_auth_accounts_user")
    )
    private User user;

    public static UserAuthAccount create(
            User user,
            String provider,
            String providerUserId,
            String email,
            String profileImageUrl
    ) {
        UserAuthAccount account = new UserAuthAccount();
        account.user = user;
        account.provider = provider;
        account.providerUserId = providerUserId;
        account.email = email;
        account.profileImageUrl = profileImageUrl;
        return account;
    }
}
