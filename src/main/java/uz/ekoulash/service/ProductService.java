package uz.ekoulash.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uz.ekoulash.dto.ProductDto;
import uz.ekoulash.entity.Product;
import uz.ekoulash.entity.User;
import uz.ekoulash.repository.BranchRepository;
import uz.ekoulash.repository.ProductLikeRepository;
import uz.ekoulash.repository.ProductRepository;
import uz.ekoulash.repository.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepo;
    private final ProductLikeRepository likeRepo;
    private final BranchRepository branchRepo;
    private final UserRepository userRepo;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    // ── List products ──────────────────────────────────────────────────────
    public List<ProductDto> getProducts(String q, String cat, int page, User currentUser) {
        var pageable = PageRequest.of(page - 1, 20);
        String qParam = (q == null || q.isBlank()) ? "" : q.trim();
        String catParam = (cat == null || cat.isBlank()) ? "" : cat.trim();
        return productRepo.findActive(qParam, catParam, pageable)
                .stream()
                .map(p -> toDto(p, currentUser))
                .toList();
    }

    // ── Single product ─────────────────────────────────────────────────────
    @Transactional
    public ProductDto getProduct(Long id, User currentUser) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mahsulot topilmadi"));
        // Ko'rishlar — owner bo'lmasa increment
        boolean isOwner = currentUser != null && p.getUser().getId().equals(currentUser.getId());
        if (!isOwner) {
            p.setViews(p.getViews() + 1);
            productRepo.save(p);
        }
        return toDto(p, currentUser);
    }

    // ── Like / unlike ──────────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> toggleLike(Long id, User user) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mahsulot topilmadi"));

        var existing = likeRepo.findByProductAndUser(p, user);
        boolean liked;
        if (existing.isPresent()) {
            likeRepo.delete(existing.get());
            p.setLikes(Math.max(0, p.getLikes() - 1));
            liked = false;
        } else {
            likeRepo.save(uz.ekoulash.entity.ProductLike.builder().product(p).user(user).build());
            p.setLikes(p.getLikes() + 1);
            liked = true;
        }
        productRepo.save(p);
        return Map.of("success", true, "liked", liked, "likes", p.getLikes());
    }

    // ── Add product ────────────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> addProduct(User user,
                                          String title, String description,
                                          String category, String condition,
                                          int price, boolean isFree,
                                          String location, String tags,
                                          Long branchId, Double lat, Double lng,
                                          String imageUrl, // <-- Flutter-dan keladigan URL-ni qabul qilish uchun qo'shildi
                                          MultipartFile image) throws IOException {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Sarlavha kiritilmagan");

        String imgName = null;

        // 1. Agar Flutter-dan tayyor Cloudinary URL manzili kelgan bo'lsa, o'shani ishlatamiz
        if (imageUrl != null && !imageUrl.isBlank()) {
            imgName = imageUrl.trim();
        }
        // 2. Agar eski usulda to'g'ridan-to'g'ri rasm fayli yuborilgan bo'lsa, uni serverga saqlaymiz
        else if (image != null && !image.isEmpty()) {
            String ext = getExt(image.getOriginalFilename());
            imgName = "prod_" + user.getId() + "_" + System.currentTimeMillis() + ext;
            Path path = Paths.get(uploadDir, imgName);
            Files.createDirectories(path.getParent());
            Files.write(path, image.getBytes());
        }

        String actCode = generateActivationCode();
        Long prodId = Long.parseLong(actCode);

        var branch = branchId != null ? branchRepo.findById(branchId).orElse(null) : null;

        productRepo.save(Product.builder()
                .id(prodId)
                .user(user)
                .title(title.trim())
                .description(description != null ? description : "")
                .category(category != null ? category : "boshqa")
                .condition(condition != null ? condition : "yaxshi")
                .price(price)
                .isFree(isFree)
                .location(location != null ? location : "")
                .tags(tags != null ? tags : "")
                .image(imgName) // <-- Endi bu yerga Cloudinary URL yoki lokal rasm nomi muvaffaqiyatli yoziladi
                .active(false)
                .activationCode(actCode)
                .branch(branch)
                .lat(lat)
                .lng(lng)
                .build());

        // Score va sertifikat
        user.setScore(user.getScore() + 10);
        long cnt = productRepo.countByUser(user);
        if      (cnt >= 20) user.setCertificate("gold");
        else if (cnt >= 10) user.setCertificate("silver");
        else if (cnt >= 3)  user.setCertificate("bronze");
        userRepo.save(user);

        return Map.of("success", true, "activationCode", actCode, "productId", actCode);
    }

    // ── My products ────────────────────────────────────────────────────────
    public List<ProductDto> myProducts(User user) {
        return productRepo.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(p -> toDto(p, user))
                .toList();
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private ProductDto toDto(Product p, User currentUser) {
        boolean liked = currentUser != null && likeRepo.existsByProductAndUser(p, currentUser);
        return ProductDto.from(p, liked);
    }

    private String generateActivationCode() {
        Random rnd = new Random();
        String code;
        int attempts = 0;
        do {
            if (++attempts > 1000) throw new RuntimeException("Aktivatsiya kodi yaratib bo'lmadi");
            code = String.valueOf(10000 + rnd.nextInt(90000));
        } while (productRepo.existsByActivationCode(code));
        return code;
    }

    private String getExt(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.')).toLowerCase();
    }
}