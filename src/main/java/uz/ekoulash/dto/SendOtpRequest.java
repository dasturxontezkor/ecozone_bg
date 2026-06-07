package uz.ekoulash.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** POST /api/auth/send-otp */
@Data
public class SendOtpRequest {
    @NotBlank
    private String phone;
}
