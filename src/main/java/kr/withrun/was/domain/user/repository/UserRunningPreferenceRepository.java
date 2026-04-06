package kr.withrun.was.domain.user.repository;

import kr.withrun.was.domain.user.entity.UserRunningPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRunningPreferenceRepository extends JpaRepository<UserRunningPreference, Long> {
}
