package uz.ekoulash.dto;

import lombok.Builder;
import lombok.Data;
import uz.ekoulash.entity.User;

import java.time.LocalDateTime;

@Data @Builder
public class UserDto {
    private Long id;
    private String uid;
    private String firstName;
    private String lastName;
    private String username;
    private String phone;
    private String avatar;
    private String tgUsername;
    private String lang;
    private Integer score;
    private String certificate;
    private boolean subValid;
    private String subType;
    private LocalDateTime subEnd;
    private String region;
    private String email;
    private String gender;
    private int productCount;
    private LocalDateTime createdAt;

    public static UserDto from(User u, int productCount) {
        boolean sv = u.getSubEnd() != null && u.getSubEnd().isAfter(LocalDateTime.now());
        return UserDto.builder()
                .id(u.getId())
                .uid(u.getUid())
                .firstName(u.getFirstName())
                .lastName(u.getLastName() != null ? u.getLastName() : "")
                .username(u.getUsername())
                .phone(u.getPhone())
                .avatar(u.getAvatar() != null ? "/uploads/" + u.getAvatar() : null)
                .tgUsername(u.getTgUsername())
                .lang(u.getLang())
                .score(u.getScore())
                .certificate(u.getCertificate())
                .subValid(sv)
                .subType(u.getSubType())
                .subEnd(u.getSubEnd())
                .region(u.getRegion())
                .email(u.getEmail())
                .gender(u.getGender())
                .productCount(productCount)
                .createdAt(u.getCreatedAt())
                .build();
    }
}
