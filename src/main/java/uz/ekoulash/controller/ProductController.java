package uz.ekoulash.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uz.ekoulash.entity.User;
import uz.ekoulash.service.ProductService;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /** GET /api/products?q=&cat=&page= */
    @GetMapping("/products")
    public ResponseEntity<?> list(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) String cat,
                                   @RequestParam(defaultValue = "1") int page,
                                   @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("products", productService.getProducts(q, cat, page, user)));
    }

    /** GET /api/products/{id} */
    @GetMapping("/products/{id}")
    public ResponseEntity<?> get(@PathVariable Long id,
                                  @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(Map.of("product", productService.getProduct(id, user)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** POST /api/products/{id}/like */
    @PostMapping("/products/{id}/like")
    public ResponseEntity<?> like(@PathVariable Long id,
                                   @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(productService.toggleLike(id, user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** POST /api/add-product (multipart/form-data) */
    @PostMapping("/add-product")
    public ResponseEntity<?> addProduct(
            @AuthenticationPrincipal User user,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "boshqa") String category,
            @RequestParam(defaultValue = "yaxshi") String condition,
            @RequestParam(defaultValue = "0") int price,
            @RequestParam(defaultValue = "false") boolean isFree,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) String imageUrl, // <-- Flutter-dan keladigan URL-ni ushlash uchun shu qator qo'shildi
            @RequestParam(required = false) MultipartFile image) {
        try {
            // Agar imageUrl kelgan bo'lsa, uni servis qatlamiga ham uzatish kerak.
            // Diqqat: Servis qatlamidagi addProduct metodiga ham ushbu parametrni qo'shib qo'yishni unutmang!
            return ResponseEntity.ok(productService.addProduct(
                    user, title, description, category, condition,
                    price, isFree, location, tags, branchId, lat, lng, imageUrl, image));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Rasm saqlashda xatolik"));
        }
    }

    /** GET /api/my-products */
    @GetMapping("/my-products")
    public ResponseEntity<?> myProducts(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("products", productService.myProducts(user)));
    }
}
