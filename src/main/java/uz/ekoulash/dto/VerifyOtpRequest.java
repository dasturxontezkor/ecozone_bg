package uz.ekoulash.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** POST /api/auth/verify-otp */
@Data
public class VerifyOtpRequest {
    @NotBlank private String phone;
    @NotBlank private String code;
}
