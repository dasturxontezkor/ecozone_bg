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

@Service
public class FcmService {
    private static final Logger log = LoggerFactory.getLogger(FcmService.class);

    @Value("${FCM_SERVICE_ACCOUNT_JSON:}")
    private String serviceAccountJson;

    @Value("${fcm.service-account-path:}")
    private String serviceAccountPath;

    private boolean initialized = false;

    @PostConstruct
    public void init() {
        if (!FirebaseApp.getApps().isEmpty()) {
            initialized = true;
            log.info("FCM: Firebase allaqachon initialized");
            return;
        }
        try {
            InputStream serviceAccount = null;
            if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
                serviceAccount = new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8));
                log.info("FCM: Service account JSON env var dan oqildi");
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
            } else {
                log.warn("FCM: FCM_SERVICE_ACCOUNT_JSON env var topilmadi!");
                return;
            }
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount)).build();
            FirebaseApp.initializeApp(options);
            initialized = true;
            log.info("FCM: Firebase Admin SDK initialized");
        } catch (IOException e) {
            log.error("FCM: Firebase init xatosi: {}", e.getMessage());
        }
    }

    public void sendNotification(String fcmToken, String title, String body, Map<String, String> data) {
        if (!initialized) { log.warn("FCM initialized emas"); return; }
        if (fcmToken == null || fcmToken.isBlank()) return;
        try {
            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                            .setSound("default")
                            .setClickAction("FLUTTER_NOTIFICATION_CLICK").build()).build();
            ApnsConfig apnsConfig = ApnsConfig.builder()
                    .setAps(Aps.builder().setSound("default").setContentAvailable(true).build()).build();
            Message.Builder mb = Message.builder().setToken(fcmToken)
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .setAndroidConfig(androidConfig).setApnsConfig(apnsConfig);
            if (data != null && !data.isEmpty()) mb.putAllData(data);
            FirebaseMessaging.getInstance().send(mb.build());
            log.info("FCM notification yuborildi: {}", title);
        } catch (FirebaseMessagingException e) {
            log.error("FCM xato [{}]: {}", e.getMessagingErrorCode(), e.getMessage());
        }
    }

    public void sendChatNotification(String recipientToken, String senderName,
                                     String messageText, Long productId, Long buyerId) {
        Map<String, String> data = Map.of(
                "type", "chat",
                "productId", String.valueOf(productId),
                "buyerId", String.valueOf(buyerId),
                "click_action", "FLUTTER_NOTIFICATION_CLICK");
        String preview = messageText != null && messageText.length() > 60
                ? messageText.substring(0, 60) + "..." : (messageText != null ? messageText : "");
        sendNotification(recipientToken, "💬 " + senderName, preview, data);
    }
}
