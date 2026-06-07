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

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GET /api/buyer-chats
 *
 * Xaridorga o'zi xabar yozgan barcha suhbatlarni qaytaradi.
 * Har bir chat:
 * {
 *   "product":     { id, title, image, isFree, price },
 *   "seller":      { id, firstName, lastName, username },
 *   "lastMessage": { text, senderId, createdAt },
 *   "unreadCount": int   ← sotuvchidan kelgan o'qilmagan xabarlar
 * }
 *
 * Farqi MyChatsController (sotuvchi) dan:
 *   - buyer = currentUser (token bilan aniqlangan)
 *   - seller = product.user
 *   - unreadCount = sotuvchi yozgan, lekin buyer ko'rmagan xabarlar soni
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BuyerChatsController {

    private final ProductMessageRepository messageRepo;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @GetMapping("/buyer-chats")
    public ResponseEntity<?> buyerChats(@AuthenticationPrincipal User buyer) {

        // Xaridor ishtirok etgan barcha xabarlar (buyer = currentUser)
        List<ProductMessage> allMsgs = messageRepo.findByBuyerOrderByCreatedAtAsc(buyer);

        if (allMsgs.isEmpty()) {
            return ResponseEntity.ok(Map.of("chats", List.of()));
        }

        // Mahsulot bo'yicha guruhlash
        Map<Long, List<ProductMessage>> byProduct = allMsgs.stream()
                .collect(Collectors.groupingBy(m -> m.getProduct().getId()));

        List<Map<String, Object>> result = new ArrayList<>();

        for (Map.Entry<Long, List<ProductMessage>> entry : byProduct.entrySet()) {
            List<ProductMessage> conversation = entry.getValue();
            if (conversation.isEmpty()) continue;

            Product product = conversation.get(0).getProduct();
            User seller = product.getUser();

            // Oxirgi xabar
            ProductMessage last = conversation.get(conversation.size() - 1);

            // O'qilmagan: sotuvchi yozgan va xaridor hali ko'rmagan xabarlar
            long unread = conversation.stream()
                    .filter(m -> m.getSender().getId().equals(seller.getId())
                            && !Boolean.TRUE.equals(m.getIsRead()))
                    .count();

            Map<String, Object> chat = new LinkedHashMap<>();

            // Mahsulot ma'lumotlari
            Map<String, Object> productMap = new LinkedHashMap<>();
            productMap.put("id",     product.getId());
            productMap.put("title",  product.getTitle() != null ? product.getTitle() : "");
            productMap.put("image",  product.getImage() != null ? product.getImage() : "");
            productMap.put("isFree", product.getIsFree() != null ? product.getIsFree() : false);
            productMap.put("price",  product.getPrice() != null ? product.getPrice() : 0);
            chat.put("product", productMap);

            // Sotuvchi ma'lumotlari
            Map<String, Object> sellerMap = new LinkedHashMap<>();
            sellerMap.put("id",        seller.getId());
            sellerMap.put("firstName", seller.getFirstName() != null ? seller.getFirstName() : "");
            sellerMap.put("lastName",  seller.getLastName()  != null ? seller.getLastName()  : "");
            sellerMap.put("username",  seller.getUsername()  != null ? seller.getUsername()  : "");
            chat.put("seller", sellerMap);

            // Oxirgi xabar
            chat.put("lastMessage", Map.of(
                    "text",      last.getText() != null ? last.getText() : "",
                    "senderId",  last.getSender().getId(),
                    "createdAt", last.getCreatedAt() != null ? last.getCreatedAt().format(FMT) : ""
            ));

            chat.put("unreadCount", unread);
            result.add(chat);
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