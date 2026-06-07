package uz.ekoulash.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    private String uid;                   // 8 xonali unique ID (A1B2C3D4)

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String phone;

    private String password;

    private String avatar;

    @Column(name = "tg_id")
    private Long tgId;

    @Column(name = "tg_username")
    private String tgUsername;

    @Column(length = 5)
    private String lang = "uz";

    @Builder.Default
    private Integer score = 0;

    private String certificate;            // bronze / silver / gold

    @Column(name = "sub_type")
    private String subType;               // monthly / yearly

    @Column(name = "sub_end")
    private LocalDateTime subEnd;

    private String region;
    private String email;
    private String gender;

    /** OTP kodi (SMS jo'natilgan) */
    private String code;

    @Column(name = "code_sent_at")
    private Long codeSentAt;              // Unix timestamp (seconds)

    @Builder.Default
    private Boolean verified = false;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;

    /**
     * Firebase Cloud Messaging token.
     * Flutter: FirebaseMessaging.instance.getToken() dan olinadi.
     * Push notification yuborish uchun ishlatiladi.
     */
    @Column(name = "fcm_token", length = 512)
    private String fcmToken;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Relations ──────────────────────────────────────────────────────────
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Product> products;

    public enum Role { USER, ADMIN }
}
