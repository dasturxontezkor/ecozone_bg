package uz.ekoulash.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Mahsulot bo'yicha chat xabarlari.
 * Xaridor ↔ Sotuvchi o'rtasidagi muloqot.
 *
 * MUHIM: "buyer" field — bu chat kimga tegishli ekanini belgilaydi.
 *   - Xaridor yozsa: sender=xaridor, buyer=xaridor
 *   - Sotuvchi javob bersa: sender=sotuvchi, buyer=xaridor (kim bilan gaplashmoqchi)
 *
 * Bu yondashuv bitta mahsulotda N ta xaridor bilan alohida chatlarni
 * to'g'ri filtrlash imkonini beradi.
 */
@Entity
@Table(name = "product_messages",
        indexes = {
                @Index(name = "idx_pm_product_buyer", columnList = "product_id, buyer_id"),
                @Index(name = "idx_pm_created_at", columnList = "created_at")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Xabar tegishli mahsulot */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Xabar yuborgan foydalanuvchi (xaridor yoki sotuvchi) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * Bu chat kimga tegishli (xaridor).
     * - Xaridor yozsa: buyer = xaridor o'zi
     * - Sotuvchi javob bersa: buyer = shu xaridor (frontend buyerId yuboradi)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    /** Xabar o'qilganmi? false = yangi/o'qilmagan, true = o'qilgan */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}