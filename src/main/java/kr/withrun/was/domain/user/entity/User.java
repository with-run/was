package kr.withrun.was.domain.user.entity;

// jakarta, 데이터베이스 설계 도구

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.common.entity.BaseEntity;
import org.hibernate.annotations.SQLDelete;

// lombok을 통해 자바에서 데이터를 담는 객체(Entity나 DTO)를 쉽게 만들 수 있음.
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE user_id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    private static final LocalDate PENDING_BIRTH_DATE = LocalDate.of(1900, 1, 1);
    private static final Gender PENDING_GENDER = Gender.MALE;
    private static final double PENDING_HEIGHT_CM = 1.0;
    private static final double PENDING_WEIGHT_KG = 1.0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "nickname", length = 16)
    private String nickname;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 16)
    private Gender gender;

    @Column(name = "height_cm")
    private Double height;

    @Column(name = "weight_kg")
    private Double weight;

    @Column(name = "profile_completed", nullable = false)
    private boolean profileCompleted;

    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private UserRunningPreference userRunningPreference;
    
    // 기존 DB에 사용자 정보가 없다면 
    public static User createPendingSocialUser() {
        User user = new User();
        user.profileCompleted = false;
        return user;
    }

    // 실제 DB에 저장해야 하는 pending 소셜 사용자는 필수 컬럼 제약을 통과할 임시값을 함께 채웁니다.
    public static User createPendingSocialUser(String temporaryNickname) {
        User user = new User();
        user.nickname = temporaryNickname;
        user.birthDate = PENDING_BIRTH_DATE;
        user.gender = PENDING_GENDER;
        user.height = PENDING_HEIGHT_CM;
        user.weight = PENDING_WEIGHT_KG;
        user.profileCompleted = false;
        return user;
    }

    // 온보딩이 끝났을 대 비어있던 사용자 프로필을 채우는 함수
    public void completeProfile(
        String nickname,
        LocalDate birthDate,
        Gender gender,
        Double height,
        Double weight
    ) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("nickname must not be blank");
        }
        if (birthDate == null) {
            throw new IllegalArgumentException("birthDate must not be null");
        }
        if (gender == null) {
            throw new IllegalArgumentException("gender must not be null");
        }
        if (height == null || height <= 0) {
            throw new IllegalArgumentException("height must be positive");
        }
        if (weight == null || weight <= 0) {
            throw new IllegalArgumentException("weight must be positive");
        }

        this.nickname = nickname.trim();
        this.birthDate = birthDate;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.profileCompleted = true;
    }

    public void assignRunningPreference(UserRunningPreference userRunningPreference) {
        this.userRunningPreference = userRunningPreference;
    }

}
