package uz.ekoulash.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** POST /api/auth/setup-profile — yangi foydalanuvchi profil to'ldirish */
@Data
public class SetupProfileRequest {
    @NotNull  private Long userId;
    @NotBlank private String firstName;
    private String lastName;
    @NotBlank private String username;
    private String region;
}
