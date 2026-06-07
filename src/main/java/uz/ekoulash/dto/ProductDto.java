package uz.ekoulash.dto;

import lombok.Builder;
import lombok.Data;
import uz.ekoulash.entity.Product;

import java.time.LocalDateTime;

@Data @Builder
public class ProductDto {
    private Long   id;
    private Long   userId;
    private String title;
    private String description;
    private String category;
    private String condition;
    private Integer price;
    private Boolean isFree;
    private String  location;
    private String  tags;
    private String  image;      // ← Cloudinary secure_url (to'liq URL) yoki null
    private Integer views;
    private Integer likes;
    private Boolean liked;
    private Boolean active;
    private Boolean sold;
    private String  activationCode;
    private Long    branchId;
    private String  branchName;
    private Double  lat;
    private Double  lng;
    private LocalDateTime createdAt;
    private OwnerDto owner;

    @Data @Builder
    public static class OwnerDto {
        private String name;
        private String username;
        private String avatar;       // ← Cloudinary URL yoki null
        private String phone;
        private String tgUsername;
    }

    public static ProductDto from(Product p, boolean liked) {
        var    owner      = p.getUser();
        String branchName = null;
        Double lat        = p.getLat();
        Double lng        = p.getLng();
        Long   branchId   = null;

        if (p.getBranch() != null) {
            branchName = p.getBranch().getName();
            lat        = p.getBranch().getLat();
            lng        = p.getBranch().getLng();
            branchId   = p.getBranch().getId();
        }

        return ProductDto.builder()
                .id(p.getId())
                .userId(owner.getId())
                .title(p.getTitle())
                .description(p.getDescription())
                .category(p.getCategory())
                .condition(p.getCondition())
                .price(p.getPrice())
                .isFree(p.getIsFree())
                .location(p.getLocation())
                .tags(p.getTags())
                // Cloudinary URL to'g'ridan-to'g'ri saqlanadi — /uploads/ prefiks kerak emas
                .image(p.getImage())
                .views(p.getViews())
                .likes(p.getLikes())
                .liked(liked)
                .active(p.getActive())
                .sold(p.getSold() != null ? p.getSold() : false)
                .activationCode(p.getActivationCode())
                .branchId(branchId)
                .branchName(branchName)
                .lat(lat)
                .lng(lng)
                .createdAt(p.getCreatedAt())
                .owner(OwnerDto.builder()
                        .name((owner.getFirstName() + " " +
                                (owner.getLastName() != null ? owner.getLastName() : "")).trim())
                        .username(owner.getUsername())
                        // Avatar ham Cloudinary URL — prefiks kerak emas
                        .avatar(owner.getAvatar())
                        .phone(owner.getPhone())
                        .tgUsername(owner.getTgUsername())
                        .build())
                .build();
    }
}