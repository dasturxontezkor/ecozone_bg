package uz.ekoulash.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uz.ekoulash.entity.User;
import uz.ekoulash.service.UserService;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** GET /api/me — joriy foydalanuvchi ma'lumotlari */
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("user", userService.getMe(user)));
    }

    /** POST /api/update-profile — profilni yangilash */
    @PostMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal User user,
                                           @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(Map.of("user", userService.updateProfile(user, body)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/upload-avatar — avatar yuklash (fayl yoki Cloudinary URL) */
    @PostMapping("/upload-avatar")
    public ResponseEntity<?> uploadAvatar(@AuthenticationPrincipal User user,
                                          @RequestParam(value = "file", required = false) MultipartFile file,
                                          @RequestParam(value = "cloudinaryUrl", required = false) String cloudinaryUrl) {
        try {
            String url;
            if (cloudinaryUrl != null && !cloudinaryUrl.isBlank()) {
                // Cloudinary URL to'g'ridan saqlanadi
                url = userService.saveAvatarUrl(user, cloudinaryUrl);
            } else if (file != null && !file.isEmpty()) {
                // Eski usul: fayl yuklanadi (backward compatibility)
                url = userService.uploadAvatar(user, file);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "file yoki cloudinaryUrl yuborilishi shart"));
            }
            return ResponseEntity.ok(Map.of("success", true, "url", url));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Fayl saqlashda xatolik"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/rating — top foydalanuvchilar */
    @GetMapping("/rating")
    public ResponseEntity<?> rating() {
        return ResponseEntity.ok(Map.of("users", userService.getRating()));
    }

    /** GET /api/my-stats — foydalanuvchining shaxsiy statistikasi */
    @GetMapping("/my-stats")
    public ResponseEntity<?> myStats(@AuthenticationPrincipal User user) {
        // UserService.getMe() ichida productCount bor
        var me = user != null ? userService.getMe(user) : null;
        return ResponseEntity.ok(Map.of(
                "userStats", me != null ? me : Map.of()
        ));
    }
}