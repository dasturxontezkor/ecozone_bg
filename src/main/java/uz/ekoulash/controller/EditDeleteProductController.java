package uz.ekoulash.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.ekoulash.dto.ProductDto;
import uz.ekoulash.entity.Product;
import uz.ekoulash.entity.User;
import uz.ekoulash.repository.ProductLikeRepository;
import uz.ekoulash.repository.ProductMessageRepository;
import uz.ekoulash.repository.ProductRepository;

import java.util.Map;

/**
 * Mahsulotni tahrirlash va o'chirish.
 *
 * POST   /api/edit-product/{id}   → faqat egasi tahrirlaydi
 * DELETE /api/products/{id}       → faqat egasi o'chiradi
 *
 * Javobda to'liq ProductDto qaytadi — Flutter UI ni yangilashi uchun.
 */
@RestController
@RequiredArgsConstructor
public class EditDeleteProductController {

    private final ProductRepository     productRepo;
    private final ProductLikeRepository likeRepo;
    private final ProductMessageRepository messageRepo; // <-- QO'SHILDI
    private final ProductMessageRepository productMessageRepo; // <-- QO'SHILDI


    // ── Tahrirlash ────────────────────────────────────────────────────────

    // ── Tahrirlash ────────────────────────────────────────────────────────

    @PutMapping("/api/edit-product/{id}")
    public ResponseEntity<?> editProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "boshqa") String category,
            @RequestParam(defaultValue = "yaxshi") String condition,
            @RequestParam(defaultValue = "0") int price,
            @RequestParam(defaultValue = "false") boolean isFree,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String imageUrl) {

        Product product = productRepo.findById(id).orElse(null);
        if (product == null) return ResponseEntity.notFound().build();

        if (!product.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Bu mahsulot sizga tegishli emas"));
        }

        product.setTitle(title.trim());
        product.setDescription(description != null ? description.trim() : "");
        product.setCategory(category);
        product.setCondition(condition);
        product.setPrice(isFree ? 0 : price);
        product.setIsFree(isFree);
        product.setLocation(location != null ? location.trim() : "");
        product.setTags(tags != null ? tags.trim() : "");

        // ── RASMNI USHLAB QOLISH LOGIKASI ──
        // Agar foydalanuvchi rasmni butunlay o'chirib yuborish tugmasini bosgan bo'lsa (imageUrl bo'sh string "" bo'lib kelsa):
        if (imageUrl != null && imageUrl.isEmpty()) {
            product.setImage(null);
        }
        // Agar yangi rasm yuklangan bo'lsa yoki eski rasm manzili o'zgarmay kelgan bo'lsa:
        else if (imageUrl != null && !imageUrl.isBlank()) {
            product.setImage(imageUrl);
        }
        // Agar imageUrl parameter sifatida umuman kelmasa (null bo'lsa), product.getImage() eski holatida o'zgarmasdan qoladi.

        productRepo.save(product);

        boolean liked = likeRepo.existsByProductAndUser(product, currentUser);
        ProductDto dto = ProductDto.from(product, liked);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "product", dto
        ));
    }

    // ── O'chirish ─────────────────────────────────────────────────────────

    @DeleteMapping("/api/products/{id}")
    public ResponseEntity<?> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        Product product = productRepo.findById(id).orElse(null);
        if (product == null) return ResponseEntity.notFound().build();

        if (!product.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Bu mahsulot sizga tegishli emas"));
        }

        // ── MUAMMONING TO'LIQ YECHIMI SHU YERDA ──
        // 1. Mahsulotga tegishli barcha layklarni o'chiramiz
        likeRepo.deleteByProductId(id);

        // 2. Mahsulotga tegishli barcha chat xabarlarini o'chiramiz
        productMessageRepo.deleteByProductId(id);

        // 3. Endi xavfsiz holatda mahsulotning o'zini o'chiramiz
        productRepo.delete(product);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Mahsulot o'chirildi"
        ));
    }
}
