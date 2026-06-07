package uz.ekoulash.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.ekoulash.dto.*;
import uz.ekoulash.entity.OtpLog;
import uz.ekoulash.entity.User;
import uz.ekoulash.repository.OtpLogRepository;
import uz.ekoulash.repository.ProductRepository;
import uz.ekoulash.repository.UserRepository;
import uz.ekoulash.security.JwtUtil;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final OtpLogRepository otpLogRepo;
    private final ProductRepository productRepo;
    private final JwtUtil jwtUtil;
    private final TelegramService telegramService;

    @Value("${app.otp.expiry-seconds:300}")
    private long otpExpiry;

    @Value("${app.dev-mode:false}")
    private boolean devMode;

    // ── Send OTP ───────────────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> sendOtp(String rawPhone) {
        String phone = normalizePhone(rawPhone);
        if (phone.length() < 12) throw new IllegalArgumentException("Noto'g'ri telefon raqam");

        String code = String.format("%06d", new Random().nextInt(1_000_000));
        long now = Instant.now().getEpochSecond();

        User user = userRepo.findByPhone(phone).orElse(null);
        if (user == null) {
            String dummyUname = "u" + now + (new Random().nextInt(900) + 100);
            user = User.builder()
                    .uid(generateUid())
                    .username(dummyUname)
                    .password("otp")
                    .phone(phone)
                    .firstName("User")
                    .verified(false)
                    .build();
        }
        user.setCode(code);
        user.setCodeSentAt(now);
        userRepo.save(user);

        otpLogRepo.save(OtpLog.builder().phone(phone).code(code).sentAt(now).build());

        boolean tgSent = false;
        if (user.getTgId() != null) {
            telegramService.send(user.getTgId(),
                    "🔐 <b>Tasdiqlash kodingiz</b>\n\n" +
                    "<code>" + code + "</code>\n\n" +
                    "⏱ Kod 5 daqiqa amal qiladi.");
            tgSent = true;
        } else {
            // SMS yuborish (ESKIZ yoki boshqa provider)
            // smsService.send(phone, "EkoUlash: " + code);
            log.info("[OTP] {} → {}", phone, code);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("phone", phone);
        result.put("tgSent", tgSent);

        // Developer rejimida OTP ni response da qaytarish
        if (devMode) {
            result.put("devCode", code);
            log.warn("[DEV MODE] OTP for {} is: {}", phone, code);
        }

        return result;
    }

    // ── Verify OTP ─────────────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> verifyOtp(String phone, String code) {
        User user = userRepo.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("Foydalanuvchi topilmadi"));

        long now = Instant.now().getEpochSecond();
        if (user.getCodeSentAt() == null || now - user.getCodeSentAt() > otpExpiry)
            throw new IllegalArgumentException("Kod muddati o'tgan");

        if (!code.equals(user.getCode()))
            throw new IllegalArgumentException("Kod noto'g'ri");

        user.setVerified(true);
        userRepo.save(user);

        boolean needsSetup = user.getFirstName() == null ||
                             user.getFirstName().equals("User") ||
                             user.getRegion() == null;

        if (!needsSetup) {
            return Map.of(
                    "success", true,
                    "needsSetup", false,
                    "userId", user.getId(),
                    "accessToken", jwtUtil.generateToken(user.getId()),
                    "refreshToken", jwtUtil.generateRefreshToken(user.getId()),
                    "user", UserDto.from(user, (int) productRepo.countByUser(user))
            );
        }

        return Map.of("success", true, "needsSetup", true, "userId", user.getId());
    }

    // ── Setup Profile ──────────────────────────────────────────────────────
    @Transactional
    public AuthResponse setupProfile(SetupProfileRequest req) {
        User user = userRepo.findById(req.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Foydalanuvchi topilmadi"));

        if (req.getUsername() != null && !req.getUsername().isBlank()) {
            if (userRepo.existsByUsernameAndIdNot(req.getUsername(), user.getId()))
                throw new IllegalArgumentException("Bu username band");
            user.setUsername(req.getUsername());
        }

        user.setFirstName(req.getFirstName());
        if (req.getLastName() != null) user.setLastName(req.getLastName());
        if (req.getRegion() != null) user.setRegion(req.getRegion());
        user.setVerified(true);
        userRepo.save(user);

        return AuthResponse.builder()
                .accessToken(jwtUtil.generateToken(user.getId()))
                .refreshToken(jwtUtil.generateRefreshToken(user.getId()))
                .user(UserDto.from(user, 0))
                .build();
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private String normalizePhone(String raw) {
        String p = raw.trim().replaceAll("[\\s\\-()]+", "");
        if (p.startsWith("+998")) return "+998" + p.substring(4);
        if (p.startsWith("998"))  return "+998" + p.substring(3);
        if (p.startsWith("0"))    return "+998" + p.substring(1);
        return p.startsWith("+") ? p : "+998" + p;
    }

    private String generateUid() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random rnd = new Random();
        String uid;
        do {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
            uid = sb.toString();
        } while (userRepo.findByUid(uid).isPresent());
        return uid;
    }
}
