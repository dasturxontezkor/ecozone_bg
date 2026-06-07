package uz.ekoulash.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.ekoulash.entity.Product;
import uz.ekoulash.entity.ProductMessage;
import uz.ekoulash.entity.User;
import uz.ekoulash.repository.ProductMessageRepository;
import uz.ekoulash.repository.ProductRepository;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GET /api/my-chats
 *
 * Sotuvchiga o'z mahsulotlariga kelgan chat ro'yxatini qaytaradi.
 * Har bir chat:
 * {
 *   "product":     { id, title, image },
 *   "buyer":       { id, firstName, lastName, username, phone },  ← phone qo'shildi
 *   "lastMessage": { text, senderId, createdAt },
 *   "unreadCount": int
 * }
 *
 * Bir mahsulotda N ta xaridor bo'lishi mumkin — har biri alohida chat.
 *
 * MUHIM: buyer.phone — Flutter ChatScreen'da sotuvchi xaridorga
 * qo'ng'iroq qilish uchun ishlatiladi.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MyChatsController {

    private final ProductRepository        productRepo;
    private final ProductMessageRepository messageRepo;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @GetMapping("/my-chats")
    public ResponseEntity<?> myChats(@AuthenticationPrincipal User seller) {

        // 1. Sotuvchining barcha mahsulotlari
        List<Product> myProducts = productRepo.findByUserOrderByCreatedAtDesc(seller);
        if (myProducts.isEmpty()) {
            return ResponseEntity.ok(Map.of("chats", List.of()));
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Product product : myProducts) {
            // 2. Har mahsulot uchun barcha xabarlar
            List<ProductMessage> allMsgs =
                    messageRepo.findByProductOrderByCreatedAtAsc(product);

            if (allMsgs.isEmpty()) continue;

            // 3. "buyer" field orqali unique xaridorlarni topamiz.
            //    buyer — har bir xabarda kim bilan gaplashilayotganini ko'rsatadi.
            Map<Long, List<ProductMessage>> byBuyer = allMsgs.stream()
                    .collect(Collectors.groupingBy(m -> m.getBuyer().getId()));

            for (Map.Entry<Long, List<ProductMessage>> entry : byBuyer.entrySet()) {
                List<ProductMessage> conversation = entry.getValue();
                if (conversation.isEmpty()) continue;

                User buyer = conversation.get(0).getBuyer();

                // Oxirgi xabar
                ProductMessage last = conversation.get(conversation.size() - 1);

                // O'qilmagan: xaridor yuborgan va sotuvchi hali ko'rmagan xabarlar
                long unread = conversation.stream()
                        .filter(m -> m.getSender().getId().equals(buyer.getId())
                                && !Boolean.TRUE.equals(m.getIsRead()))
                        .count();

                Map<String, Object> chat = new LinkedHashMap<>();

                // Mahsulot
                chat.put("product", Map.of(
                        "id",    product.getId(),
                        "title", product.getTitle() != null ? product.getTitle() : "",
                        "image", product.getImage() != null ? product.getImage() : ""
                ));

                // Xaridor — phone MAJBURIY (qo'ng'iroq uchun)
                Map<String, Object> buyerMap = new LinkedHashMap<>();
                buyerMap.put("id",        buyer.getId());
                buyerMap.put("firstName", buyer.getFirstName() != null ? buyer.getFirstName() : "");
                buyerMap.put("lastName",  buyer.getLastName()  != null ? buyer.getLastName()  : "");
                buyerMap.put("username",  buyer.getUsername()  != null ? buyer.getUsername()  : "");
                // ↓ ASOSIY: sotuvchi xaridorga qo'ng'iroq qilishi uchun
                buyerMap.put("phone",     buyer.getPhone()     != null ? buyer.getPhone()     : "");
                chat.put("buyer", buyerMap);

                // Oxirgi xabar
                chat.put("lastMessage", Map.of(
                        "text",      last.getText(),
                        "senderId",  last.getSender().getId(),
                        "createdAt", last.getCreatedAt() != null
                                ? last.getCreatedAt().format(FMT) : ""
                ));

                chat.put("unreadCount", unread);
                result.add(chat);
            }
        }

        // Eng yangi chat birinchi
        result.sort((a, b) -> {
            String ta = ((Map<?, ?>) a.get("lastMessage")).get("createdAt").toString();
            String tb = ((Map<?, ?>) b.get("lastMessage")).get("createdAt").toString();
            return tb.compareTo(ta);
        });

        return ResponseEntity.ok(Map.of("chats", result));
    }
}