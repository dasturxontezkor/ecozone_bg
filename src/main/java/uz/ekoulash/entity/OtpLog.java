package uz.ekoulash.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OtpLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String phone;
    private String code;

    @Column(name = "sent_at")
    private Long sentAt;

    @Builder.Default
    private Integer attempts = 0;
}
