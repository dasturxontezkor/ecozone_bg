package uz.ekoulash.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Barcha rasmlarni (avatar + mahsulot rasmi) Cloudinary-ga yuklash uchun
 * yagona servis.
 *
 * Cloudinary sozlamalari application.yml / muhit o'zgaruvchilaridan olinadi:
 *   cloudinary.cloud-name
 *   cloudinary.upload-preset   (unsigned preset)
 *   cloudinary.folder          (ixtiyoriy, default: ekoulash)
 */
@Service
public class CloudinaryService {

    private static final String UPLOAD_URL =
            "https://api.cloudinary.com/v1_1/%s/image/upload";

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.upload-preset}")
    private String uploadPreset;

    @Value("${cloudinary.folder:ekoulash}")
    private String folder;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * MultipartFile-ni Cloudinary-ga yuklaydi va secure_url qaytaradi.
     *
     * @param file     yuklash kerak bo'lgan fayl
     * @param subFolder ichki papka (masalan "avatars" yoki "products")
     * @return Cloudinary secure_url
     */
    public String upload(MultipartFile file, String subFolder) throws IOException {
        String url = String.format(UPLOAD_URL, cloudName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("upload_preset", uploadPreset);
        body.add("folder", folder + "/" + subFolder);

        // MultipartFile -> ByteArrayResource (fayl nomi saqlanadi)
        String originalFilename = file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "upload.jpg";
        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() { return originalFilename; }
        };
        body.add("file", resource);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IOException("Cloudinary xatosi: " + response.getStatusCode());
        }

        Object secureUrl = response.getBody().get("secure_url");
        if (secureUrl == null) {
            Object error = response.getBody().get("error");
            throw new IOException("Cloudinary URL topilmadi: " + error);
        }

        return secureUrl.toString();
    }

    /** Avatar yuklash — /ekoulash/avatars/ papkasiga */
    public String uploadAvatar(MultipartFile file) throws IOException {
        return upload(file, "avatars");
    }

    /** Mahsulot rasmi yuklash — /ekoulash/products/ papkasiga */
    public String uploadProductImage(MultipartFile file) throws IOException {
        return upload(file, "products");
    }
}
