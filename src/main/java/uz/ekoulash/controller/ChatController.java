package uz.ekoulash.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.ekoulash.entity.Message;
import uz.ekoulash.entity.User;
import uz.ekoulash.repository.MessageRepository;
import uz.ekoulash.service.TelegramService;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Foydalanuvchi ↔ Admin 1-ga-1 chat.
 *
 * Har bir user o'z alohida chatiga ega — boshqalarning xabarlarini ko'rmaydi.
 *
 * GET  /api/chat/messages          → o'z xabarlarini olish
 * POST /api/chat/send              → adminga xabar yuborish
 *
 * Admin tomonidan javob berish: POST /api/admin/reply  (AdminController ichida)
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final MessageRepository messageRepo;
    private final TelegramService   telegramService;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // ── GET /api/chat/messages ────────────────────────────────────────────
    /**
     * Joriy userning faqat o'z xabarlarini qaytaradi.
     * isAdmin=false → user yozgan, isAdmin=true → admin javob bergan.
     *
     * Qaytaradi:
     * {
     *   "messages": [
     *     { "id": 1, "text": "...", "isAdmin": false, "createdAt": "..." }
     *   ]
     * }
     */
    @GetMapping("/messages")
    public ResponseEntity<?> getMessages(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") Long afterId) {

        var messages = afterId > 0
                ? messageRepo.findByUserAndIdGreaterThanOrderByCreatedAtAsc(currentUser, afterId)
                : messageRepo.findByUserOrderByCreatedAtAsc(currentUser);

        var result = messages.stream().map(m -> Map.<String, Object>of(
                "id",        m.getId(),
                "text",      m.getText(),
                "isAdmin",   m.getIsAdmin(),
                "createdAt", m.getCreatedAt() != null ? m.getCreatedAt().format(FMT) : ""
        )).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("messages", result));
    }

    // ── POST /api/chat/send ───────────────────────────────────────────────
    /**
     * User adminga xabar yuboradi.
     * Body: { "text": "Salom, yordam kerak..." }
     *
     * Qaytaradi:
     * { "message": { "id": 1, "text": "...", "isAdmin": false, "createdAt": "..." } }
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, Object> body) {

        String text = body.getOrDefault("text", "").toString().trim();
        if (text.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Xabar bo'sh bo'lishi mumkin emas"));
        }

        Message saved = messageRepo.save(
                Message.builder()
                        .user(currentUser)
                        .text(text)
                        .isAdmin(false)
                        .build()
        );

        // Telegram orqali adminga bildirishnoma (agar tgId bo'lsa)
        // Admin Telegram bot orqali ko'ra oladi yoki /api/admin/messages dan
        try {
            telegramService.send(
                    // Admin tgId ni environment variable dan olish mumkin
                    // Hozircha faqat log qilamiz
                    null,
                    String.format("💬 <b>%s %s</b> (@%s):\n%s",
                            safe(currentUser.getFirstName()),
                            safe(currentUser.getLastName()),
                            safe(currentUser.getUsername()),
                            text)
            );
        } catch (Exception ignored) {}

        return ResponseEntity.ok(Map.of(
                "message", Map.of(
                        "id",        saved.getId(),
                        "text",      saved.getText(),
                        "isAdmin",   false,
                        "createdAt", saved.getCreatedAt() != null
                                ? saved.getCreatedAt().format(FMT) : ""
                )
        ));
    }

    private String safe(String s) {
        return s != null ? s : "";
    }
}