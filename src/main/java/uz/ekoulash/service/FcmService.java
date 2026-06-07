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
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Firebase Cloud Messaging (FCM) V1 API — Firebase Admin SDK orqali.
 *
 * Railway environment variables:
 *   FCM_SERVICE_ACCOUNT_JSON  → Firebase service account JSON ning to'liq matni
 *   FCM_PROJECT_ID            → ecoulash
 *
 * YOKI fayl orqali:
 *   FCM_SERVICE_ACCOUNT_PATH  → classpath:ecoulash-service-account.json
 */
@Service
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);

    // ✅ Variant 1: JSON matnini to'g'ridan Railway env var ga qo'yish
    @Value("${FCM_SERVICE_ACCOUNT_JSON:}")
    private String serviceAccountJson;

    // Variant 2: Fayl yo'li orqali (classpath yoki absolute path)
    @Value("${fcm.service-account-path:}")
    private String serviceAccountPath;

    @Value("${fcm.project-id:ecoulash}")
    private String projectId;

    private boolean initialized = false;

    @PostConstruct
    public void init() {
        // Agar allaqachon init bo'lgan bo'lsa qayta qilmaymiz
        if (!FirebaseApp.getApps().isEmpty()) {
            initialized = true;
            log.info("FCM: Firebase allaqachon initialized ✅");
            return;
        }

        try {
            InputStream serviceAccount = null;

            // ✅ Birinchi: JSON matnini env var dan o'qish (Railway uchun eng qulay)
            if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
                serviceAccount = new ByteArrayInputStream(
                        serviceAccountJson.getBytes(StandardCharsets.UTF_8));
                log.info("FCM: Service account JSON env var dan o'qildi");

                // Ikkinchi: fayl yo'li orqali
            } else if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
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
                log.info("FCM: Service account fayl dan o'qildi: {}", serviceAccountPath);

            } else {
                log.warn("FCM: Service account topilmadi! " +
                        "FCM_SERVICE_ACCOUNT_JSON yoki fcm.service-account-path kerak.");
                return;
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
            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                            .setSound("default")
                            .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                            .build())
                    .build();

            ApnsConfig apnsConfig = ApnsConfig.builder()
                    .setAps(Aps.builder()
                            .setSound("default")
                            .setContentAvailable(true)
                            .build())
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setAndroidConfig(androidConfig)
                    .setApnsConfig(apnsConfig);

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.info("FCM notification yuborildi ✅: {} → {}...", title,
                    fcmToken.substring(0, Math.min(10, fcmToken.length())));

        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                    || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                log.warn("FCM token yaroqsiz: {}...", fcmToken.substring(0, 10));
            } else {
                log.error("FCM yuborishda xato [{}]: {}", e.getMessagingErrorCode(), e.getMessage());
            }
        } catch (Exception e) {
            log.error("FCM umumiy xato: {}", e.getMessage());
        }
    }

    /**
     * Chat notification — yangi xabar keldi.
     */
    public void sendChatNotification(String recipientToken, String senderName,
                                     String messageText, Long productId, Long buyerId) {
        Map<String, String> data = Map.of(
                "type",         "chat",
                "productId",    String.valueOf(productId),
                "buyerId",      String.valueOf(buyerId),
                "click_action", "FLUTTER_NOTIFICATION_CLICK"
        );

        String preview = messageText != null && messageText.length() > 60
                ? messageText.substring(0, 60) + "..."
                : (messageText != null ? messageText : "");

        sendNotification(recipientToken, "💬 " + senderName, preview, data);
    }
}