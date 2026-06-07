package uz.ekoulash.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Firebase Cloud Messaging (FCM) V1 API — Firebase Admin SDK orqali.
 *
 * application.yml da quyidagini qo'shing:
 *   fcm:
 *     service-account-path: /path/to/ecoulash-6862d356bdff.json
 *     project-id: ecoulash
 */
@Service
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);

    @Value("${fcm.service-account-path:}")
    private String serviceAccountPath;

    @Value("${fcm.project-id:ecoulash}")
    private String projectId;

    private boolean initialized = false;

    @PostConstruct
    public void init() {
        if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
            log.warn("FCM: fcm.service-account-path yo'q! Notification ishlamaydi.");
            return;
        }

        // Agar allaqachon init bo'lgan bo'lsa qayta qilmaymiz
        if (!FirebaseApp.getApps().isEmpty()) {
            initialized = true;
            log.info("FCM: Firebase allaqachon initialized");
            return;
        }

        try {
            InputStream serviceAccount;
            // Classpath dan yoki fayl tizimidan o'qish
            if (serviceAccountPath.startsWith("classpath:")) {
                String path = serviceAccountPath.replace("classpath:", "");
                serviceAccount = getClass().getClassLoader().getResourceAsStream(path);
                if (serviceAccount == null) {
                    log.error("FCM: classpath resource topilmadi: {}", path);
                    return;
                }
            } else {
                serviceAccount = new FileInputStream(serviceAccountPath);
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            initialized = true;
            log.info("FCM: Firebase Admin SDK muvaffaqiyatli initialized ✅");

        } catch (IOException e) {
            log.error("FCM: Firebase init xatosi: {}", e.getMessage());
        }
    }

    /**
     * Bitta qurilmaga notification yuborish.
     *
     * @param fcmToken  Qabul qiluvchi qurilma FCM token
     * @param title     Notification sarlavhasi
     * @param body      Notification matni
     * @param data      Qo'shimcha ma'lumotlar (ekranni ochish uchun)
     */
    public void sendNotification(String fcmToken, String title, String body,
                                 Map<String, String> data) {
        if (!initialized) {
            log.warn("FCM initialized emas, notification yuborilmadi");
            return;
        }
        if (fcmToken == null || fcmToken.isBlank()) {
            log.debug("FCM token yo'q, notification yuborilmadi");
            return;
        }

        try {
            // Android uchun sozlamalar
            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                            .setSound("default")
                            .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                            .build())
                    .build();

            // iOS uchun sozlamalar
            ApnsConfig apnsConfig = ApnsConfig.builder()
                    .setAps(Aps.builder()
                            .setSound("default")
                            .setContentAvailable(true)
                            .build())
                    .build();

            // Xabar yasash
            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setAndroidConfig(androidConfig)
                    .setApnsConfig(apnsConfig);

            // Qo'shimcha data qo'shamiz
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.info("FCM notification yuborildi ✅: {} → {}...", title,
                    fcmToken.substring(0, Math.min(10, fcmToken.length())));

        } catch (FirebaseMessagingException e) {
            // Token eskirgan yoki noto'g'ri bo'lsa
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                    || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                log.warn("FCM token yaroqsiz, tozalash kerak: {}", fcmToken.substring(0, 10) + "...");
            } else {
                log.error("FCM yuborishda xato [{}]: {}", e.getMessagingErrorCode(), e.getMessage());
            }
        } catch (Exception e) {
            log.error("FCM umumiy xato: {}", e.getMessage());
        }
    }

    /**
     * Chat notification — yangi xabar keldi.
     *
     * @param recipientToken  Qabul qiluvchi FCM token
     * @param senderName      Kim yubordi
     * @param messageText     Xabar matni
     * @param productId       Mahsulot ID (ekranni ochish uchun)
     * @param buyerId         Xaridor ID (chat aniqlashtirish uchun)
     */
    public void sendChatNotification(String recipientToken, String senderName,
                                     String messageText, Long productId, Long buyerId) {
        Map<String, String> data = Map.of(
                "type",          "chat",
                "productId",     String.valueOf(productId),
                "buyerId",       String.valueOf(buyerId),
                "click_action",  "FLUTTER_NOTIFICATION_CLICK"
        );

        String preview = messageText != null && messageText.length() > 60
                ? messageText.substring(0, 60) + "..."
                : (messageText != null ? messageText : "");

        sendNotification(
                recipientToken,
                "💬 " + senderName,
                preview,
                data
        );
    }
}