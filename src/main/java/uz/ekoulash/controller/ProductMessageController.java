package uz.ekoulash.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.ekoulash.entity.Product;
import uz.ekoulash.entity.ProductMessage;
import uz.ekoulash.entity.User;
import uz.ekoulash.repository.ProductMessageRepository;
import uz.ekoulash.repository.ProductRepository;
import uz.ekoulash.repository.UserRepository;
import uz.ekoulash.service.FcmService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mahsulot bo'yicha xaridor ↔ sotuvchi 1-ga-1 chat.
 *
 * GET  /api/products/{id}/messages?buyerId={buyerId}  → xabarlar ro'yxati
 * POST /api/products/{id}/messages                    → xabar yuborish (FCM notification)
 * GET  /api/products/{id}/chat-threads                → sotuvchi uchun: barcha xaridor ro'yxati
 * POST /api/products/{id}/mark-sold                   → active=false
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductMessageController {

    private final ProductRepository        productRepo;
    private final ProductMessageRepository messageRepo;
    private final UserRepository           userRepo;
    private final FcmService               fcmService;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // ── GET /api/products/{id}/messages?buyerId={buyerId} ─────────────────
    @GetMapping("/{id}/messages")
    public ResponseEntity<?> getMessages(
            @PathVariable Long id,
            @RequestParam Long buyerId,
            @AuthenticationPrincipal User currentUser) {

        Product product = productRepo.findById(id).orElse(null);
        if (product == null) return ResponseEntity.notFound().build();

        boolean isOwner = product.getUser().getId().equals(currentUser.getId());
        boolean isBuyer = currentUser.getId().equals(buyerId);
        if (!isOwner && !isBuyer) {
            return ResponseEntity.status(403).body(Map.of("error", "Ruxsat yo'q"));
        }

        List<ProductMessage> msgs = messageRepo.findByProductAndBuyer(product, buyerId);

        // ── Joriy userga kelgan xabarlarni o'qildi deb belgilaymiz ──────
        List<ProductMessage> toMark = msgs.stream()
                .filter(m -> !m.getSender().getId().equals(currentUser.getId())
                        && !Boolean.TRUE.equals(m.getIsRead()))
                .collect(Collectors.toList());
        if (!toMark.isEmpty()) {
            toMark.forEach(m -> m.setIsRead(true));
            messageRepo.saveAll(toMark);
        }

        List<Map<String, Object>> messages = msgs
                .stream()
                .map(m -> Map.<String, Object>of(
                        "id",         m.getId(),
                        "text",       m.getText(),
                        "senderId",   m.getSender().getId(),
                        "senderName", fullName(m.getSender()),
                        "createdAt",  m.getCreatedAt() != null
                                ? m.getCreatedAt().format(FMT) : ""
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("messages", messages));
    }

    // ── POST /api/products/{id}/messages ──────────────────────────────────
    /**
     * Xabar yuborish + FCM push notification.
     *
     * - Xaridor yozsa:   → sotuvchiga notification yuboriladi
     * - Sotuvchi yozsa:  → xaridorga notification yuboriladi
     */
    @PostMapping("/{id}/messages")
    public ResponseEntity<?> sendMessage(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, Object> body) {

        String text = body.getOrDefault("text", "").toString().trim();
        if (text.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Xabar bo'sh bo'lishi mumkin emas"));
        }

        Object buyerIdRaw = body.get("buyerId");
        if (buyerIdRaw == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "buyerId yuborilishi shart"));
        }
        Long buyerId;
        try {
            buyerId = Long.parseLong(buyerIdRaw.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "buyerId noto'g'ri format"));
        }

        Product product = productRepo.findById(id).orElse(null);
        if (product == null) return ResponseEntity.notFound().build();

        User buyer = userRepo.findById(buyerId).orElse(null);
        if (buyer == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Xaridor topilmadi"));
        }

        boolean isOwner = product.getUser().getId().equals(currentUser.getId());
        if (isOwner && buyer.getId().equals(currentUser.getId())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "buyerId o'zingiz bo'lishi mumkin emas"));
        }

        ProductMessage saved = messageRepo.save(
                ProductMessage.builder()
                        .product(product)
                        .sender(currentUser)
                        .buyer(buyer)
                        .text(text)
                        .build()
        );

        // ── FCM Push Notification ─────────────────────────────────────────
        // Xabar yuborganidan so'ng, qabul qiluvchiga notification yuboramiz
        try {
            String senderName = fullName(currentUser);

            if (isOwner) {
                // Sotuvchi yozdi → xaridorga notification
                if (buyer.getFcmToken() != null) {
                    fcmService.sendChatNotification(
                            buyer.getFcmToken(),
                            senderName,
                            text,
                            product.getId(),
                            buyerId
                    );
                }
            } else {
                // Xaridor yozdi → sotuvchiga notification
                User seller = product.getUser();
                if (seller.getFcmToken() != null) {
                    fcmService.sendChatNotification(
                            seller.getFcmToken(),
                            senderName,
                            text,
                            product.getId(),
                            buyerId
                    );
                }
            }
        } catch (Exception e) {
            // Notification yuborishda xato bo'lsa ham xabar saqlanadi
        }

        return ResponseEntity.ok(Map.of(
                "message", Map.of(
                        "id",        saved.getId(),
                        "text",      saved.getText(),
                        "senderId",  currentUser.getId(),
                        "createdAt", saved.getCreatedAt() != null
                                ? saved.getCreatedAt().format(FMT) : ""
                )
        ));
    }

    // ── GET /api/products/{id}/chat-threads ───────────────────────────────
    /**
     * Sotuvchi uchun: mahsulotga xabar yozgan barcha xaridorlar ro'yxati.
     * Qaytaradi: { "threads": [ { buyerId, buyerName, buyerAvatar, lastMessage, lastTime, unreadCount } ] }
     */
    @GetMapping("/{id}/chat-threads")
    public ResponseEntity<?> getChatThreads(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        Product product = productRepo.findById(id).orElse(null);
        if (product == null) return ResponseEntity.notFound().build();

        if (!product.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Ruxsat yo'q"));
        }

        List<ProductMessage> all = messageRepo.findByProductOrderByCreatedAtAsc(product);

        Map<Long, List<ProductMessage>> byBuyer = all.stream()
                .collect(Collectors.groupingBy(m -> m.getBuyer().getId()));

        List<Map<String, Object>> threads = byBuyer.entrySet().stream().map(entry -> {
            List<ProductMessage> msgs = entry.getValue();
            User buyer = msgs.get(0).getBuyer();
            ProductMessage last = msgs.get(msgs.size() - 1);
            long unread = msgs.stream()
                    .filter(m -> m.getSender().getId().equals(buyer.getId())
                            && !Boolean.TRUE.equals(m.getIsRead()))
                    .count();

            return Map.<String, Object>of(
                    "buyerId",     buyer.getId(),
                    "buyerName",   fullName(buyer),
                    "buyerAvatar", buyer.getAvatar() != null ? buyer.getAvatar() : "",
                    "lastMessage", last.getText(),
                    "lastTime",    last.getCreatedAt() != null ? last.getCreatedAt().format(FMT) : "",
                    "unreadCount", unread
            );
        }).sorted((a, b) -> {
            String ta = a.get("lastTime").toString();
            String tb = b.get("lastTime").toString();
            return tb.compareTo(ta);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("threads", threads));
    }

    // ── POST /api/products/{id}/mark-sold ─────────────────────────────────

    @PostMapping("/{id}/mark-sold")
    public ResponseEntity<?> markSold(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        Product product = productRepo.findById(id).orElse(null);
        if (product == null) return ResponseEntity.notFound().build();

        if (!product.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Bu mahsulot sizga tegishli emas"));
        }

        product.setActive(false);
        product.setSold(true);
        productRepo.save(product);

        return ResponseEntity.ok(Map.of("success", true, "message", "Mahsulot ro'yxatdan olib tashlandi"));
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private String fullName(User u) {
        String fn = u.getFirstName() != null ? u.getFirstName() : "";
        String ln = u.getLastName()  != null ? u.getLastName()  : "";
        String full = (fn + " " + ln).trim();
        return full.isEmpty() ? (u.getPhone() != null ? u.getPhone() : "User") : full;
    }
}