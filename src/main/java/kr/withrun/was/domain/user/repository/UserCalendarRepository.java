package kr.withrun.was.domain.user.repository;

import kr.withrun.was.domain.user.entity.UserCalendar;
import kr.withrun.was.domain.user.repository.query.UserCalendarCustomRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCalendarRepository extends JpaRepository<UserCalendar, Long>, UserCalendarCustomRepository {
}
