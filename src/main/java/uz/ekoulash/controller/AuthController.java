package uz.ekoulash.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.ekoulash.dto.SendOtpRequest;
import uz.ekoulash.dto.SetupProfileRequest;
import uz.ekoulash.dto.VerifyOtpRequest;
import uz.ekoulash.service.AuthService;

import java.util.Map;

/**
 * POST /api/auth/send-otp      → telefon raqamga OTP jo'natish
 * POST /api/auth/verify-otp    → OTP tasdiqlash
 * POST /api/auth/setup-profile → yangi foydalanuvchi profil to'ldirish
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 1-qadam: telefon raqam kiritiladi, SMS OTP jo'natiladi */
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody SendOtpRequest req) {
        try {
            return ResponseEntity.ok(authService.sendOtp(req.getPhone()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** 2-qadam: OTP kodi kiritiladi va tekshiriladi */
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        try {
            return ResponseEntity.ok(authService.verifyOtp(req.getPhone(), req.getCode()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 3-qadam (faqat yangi foydalanuvchilar uchun):
     * Ism, familiya, username, viloyat kiritiladi
     */
    @PostMapping("/setup-profile")
    public ResponseEntity<?> setupProfile(@Valid @RequestBody SetupProfileRequest req) {
        try {
            return ResponseEntity.ok(authService.setupProfile(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
