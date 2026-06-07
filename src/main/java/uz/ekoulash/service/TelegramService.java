package uz.ekoulash.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class TelegramService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.telegram.bot-token}")
    private String botToken;

    public void send(Long chatId, String text) {
        if (botToken == null || botToken.isBlank() || botToken.equals("YOUR_BOT_TOKEN_HERE")) {
            log.warn("[TG] Bot token not configured. Message to {}: {}", chatId, text);
            return;
        }
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = Map.of(
                    "chat_id", chatId,
                    "text", text,
                    "parse_mode", "HTML"
            );
            restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
        } catch (Exception e) {
            log.error("[TG] send error: {}", e.getMessage());
        }
    }
}
