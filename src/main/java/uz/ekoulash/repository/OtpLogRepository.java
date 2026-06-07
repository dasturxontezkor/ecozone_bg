package uz.ekoulash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.ekoulash.entity.OtpLog;

public interface OtpLogRepository extends JpaRepository<OtpLog, Long> {}
