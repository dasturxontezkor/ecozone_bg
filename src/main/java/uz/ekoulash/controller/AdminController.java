package uz.ekoulash.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.ekoulash.entity.Branch;
import uz.ekoulash.entity.Product;
import uz.ekoulash.entity.User;
import uz.ekoulash.repository.*;
import uz.ekoulash.service.TelegramService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final MessageRepository messageRepo;
    private final BranchRepository branchRepo;
    private final TelegramService telegramService;

    // ── Stats ──────────────────────────────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(Map.of(
                "users",    userRepo.count(),
                "products", productRepo.count()
        ));
    }

    // ── Users ──────────────────────────────────────────────────────────────
    @GetMapping("/users")
    public ResponseEntity<?> users(@RequestParam(defaultValue = "") String q,
                                    @RequestParam(defaultValue = "1") int page) {
        var all = userRepo.findAll();
        var filtered = all.stream().filter(u ->
                q.isBlank() ||
                safe(u.getFirstName()).contains(q) ||
                safe(u.getLastName()).contains(q) ||
                safe(u.getUsername()).contains(q) ||
                safe(u.getPhone()).contains(q)
        ).toList();
        int limit = 20, offset = (page - 1) * limit;
        var paged = filtered.stream().skip(offset).limit(limit).toList();
        return ResponseEntity.ok(Map.of(
                "users", paged.stream().map(u -> toUserMap(u)).toList(),
                "total", filtered.size(),
                "page", page,
                "pages", (filtered.size() + limit - 1) / limit
        ));
    }

    @PostMapping("/user/update")
    public ResponseEntity<?> updateUser(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Topilmadi"));
        if (body.containsKey("firstName")) user.setFirstName((String) body.get("firstName"));
        if (body.containsKey("lastName"))  user.setLastName((String)  body.get("lastName"));
        if (body.containsKey("region"))    user.setRegion((String)    body.get("region"));
        if (body.containsKey("score"))     user.setScore(toInt(body.get("score")));
        userRepo.save(user);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/user/delete")
    public ResponseEntity<?> deleteUser(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        userRepo.deleteById(userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── Products ───────────────────────────────────────────────────────────
    @GetMapping("/products")
    public ResponseEntity<?> products() {
        return ResponseEntity.ok(Map.of("products", productRepo.findAll().stream()
                .map(p -> Map.of(
                        "id", p.getId(),
                        "title", p.getTitle(),
                        "active", p.getActive(),
                        "activationCode", safe(p.getActivationCode()),
                        "owner", p.getUser().getFirstName() + " " + safe(p.getUser().getLastName()),
                        "image", p.getImage() != null ? "/uploads/" + p.getImage() : ""
                )).toList()
        ));
    }

    @PostMapping("/product/activate")
    public ResponseEntity<?> activateProduct(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        Product p = productRepo.findByActivationCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Mahsulot topilmadi"));
        p.setActive(true);
        productRepo.save(p);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/product/delete")
    public ResponseEntity<?> deleteProduct(@RequestBody Map<String, Object> body) {
        Long pid = toLong(body.get("productId"));
        productRepo.deleteById(pid);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── Subscription ──────────────────────────────────────────────────────
    @PostMapping("/subscription")
    public ResponseEntity<?> grantSub(@RequestBody Map<String, Object> body) {
        Long userId  = toLong(body.get("userId"));
        String type  = (String) body.getOrDefault("type", "monthly");
        User user    = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Topilmadi"));
        LocalDateTime end = LocalDateTime.now().plusDays("yearly".equals(type) ? 365 : 30);
        user.setSubType(type);
        user.setSubEnd(end);
        userRepo.save(user);
        if (user.getTgId() != null) {
            telegramService.send(user.getTgId(),
                    "✅ <b>Obuna faollashtirildi!</b>\n📅 Muddati: <b>" + end.toLocalDate() + "</b>");
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── Messages ───────────────────────────────────────────────────────────
    @GetMapping("/messages")
    public ResponseEntity<?> messages() {
        var usersWithMessages = userRepo.findAll().stream()
                .filter(u -> messageRepo.existsByUser(u))
                .map(u -> {
                    var msgs = messageRepo.findByUserOrderByCreatedAtAsc(u);
                    String last = msgs.isEmpty() ? "" : msgs.get(msgs.size() - 1).getText();
                    return Map.of("userId", u.getId(), "name",
                            u.getFirstName() + " " + safe(u.getLastName()),
                            "username", safe(u.getUsername()), "lastMsg", last);
                }).toList();
        return ResponseEntity.ok(Map.of("chats", usersWithMessages));
    }

    @GetMapping("/messages/{uid}")
    public ResponseEntity<?> userMessages(@PathVariable Long uid) {
        User user = userRepo.findById(uid).orElseThrow();
        return ResponseEntity.ok(Map.of("messages",
                messageRepo.findByUserOrderByCreatedAtAsc(user).stream()
                        .map(m -> Map.of("id", m.getId(), "text", m.getText(),
                                "isAdmin", m.getIsAdmin(), "createdAt", m.getCreatedAt()))
                        .toList()));
    }

    @PostMapping("/reply")
    public ResponseEntity<?> reply(@RequestBody Map<String, Object> body) {
        Long uid  = toLong(body.get("userId"));
        String text = (String) body.get("text");
        User user = userRepo.findById(uid).orElseThrow();
        messageRepo.save(uz.ekoulash.entity.Message.builder()
                .user(user).text(text).isAdmin(true).build());
        if (user.getTgId() != null)
            telegramService.send(user.getTgId(), "💬 <b>Admin:</b> " + text);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── Branches ───────────────────────────────────────────────────────────
    @GetMapping("/branches")
    public ResponseEntity<?> getBranches() {
        return ResponseEntity.ok(Map.of("branches", branchRepo.findAllByOrderByCreatedAtDesc()));
    }

    @PostMapping("/branches")
    public ResponseEntity<?> addBranch(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        if (name == null || name.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Nom kiritilmagan"));
        Branch b = branchRepo.save(Branch.builder()
                .name(name)
                .address(safe((String) body.get("address")))
                .phone(safe((String) body.get("phone")))
                .workHours(safe((String) body.get("workHours")))
                .build());
        return ResponseEntity.ok(Map.of("success", true, "branch", b));
    }

    @DeleteMapping("/branches/{id}")
    public ResponseEntity<?> deleteBranch(@PathVariable Long id) {
        branchRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private Map<String, Object> toUserMap(User u) {
        return Map.of(
                "id",        u.getId(),
                "uid",       safe(u.getUid()),
                "firstName", safe(u.getFirstName()),
                "lastName",  safe(u.getLastName()),
                "username",  safe(u.getUsername()),
                "phone",     safe(u.getPhone()),
                "region",    safe(u.getRegion()),
                "score",     u.getScore(),
                "certificate", safe(u.getCertificate()),
                "createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
        );
    }

    private String safe(String s) { return s != null ? s : ""; }
    private Long toLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(o));
    }
    private int toInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        return Integer.parseInt(String.valueOf(o));
    }
}
