package uz.ekoulash.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.ekoulash.entity.User;
import uz.ekoulash.repository.UserRepository;

import java.util.Map;

/**
 * POST /api/save-fcm-token
 *
 * Flutter app ishga tushganda FirebaseMessaging.instance.getToken() dan
 * olingan FCM tokenni serverga saqlaydi.
 *
 * Body: { "fcmToken": "eXYZ123..." }
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FcmTokenController {

    private final UserRepository userRepo;

    @PostMapping("/save-fcm-token")
    public ResponseEntity<?> saveFcmToken(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, String> body) {

        String token = body.get("fcmToken");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "fcmToken bo'sh bo'lishi mumkin emas"));
        }

        currentUser.setFcmToken(token);
        userRepo.save(currentUser);

        return ResponseEntity.ok(Map.of("success", true));
    }
}
