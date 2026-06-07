package uz.ekoulash.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    private Long id;                      // activation_code = id (5 xonali)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    private String category = "boshqa";

    @Builder.Default
    private String condition = "yaxshi";

    @Builder.Default
    private Integer price = 0;

    @Column(name = "is_free")
    @Builder.Default
    private Boolean isFree = false;

    private String location;
    private String tags;
    private String image;

    @Builder.Default
    private Integer views = 0;

    @Builder.Default
    private Integer likes = 0;

    /** Mahsulot admin tomonidan tasdiqlangandan so'ng ko'rinadi */
    @Builder.Default
    private Boolean active = false;

    /** Egasi "Sotildi/Berib bo'ldim" bosganida true bo'ladi */
    @Column(name = "sold")
    @Builder.Default
    private Boolean sold = false;

    @Column(name = "activation_code", unique = true)
    private String activationCode;        // 5-digit string

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    private Double lat;
    private Double lng;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}