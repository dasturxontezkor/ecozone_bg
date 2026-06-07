package uz.ekoulash.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uz.ekoulash.dto.UserDto;
import uz.ekoulash.entity.User;
import uz.ekoulash.repository.ProductRepository;
import uz.ekoulash.repository.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private final ProductRepository productRepo;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    public UserDto getMe(User user) {
        int count = (int) productRepo.countByUser(user);
        return UserDto.from(user, count);
    }

    @Transactional
    public UserDto updateProfile(User user, Map<String, String> body) {
        String firstName = body.get("firstName");
        String lastName  = body.get("lastName");
        String username  = body.get("username");
        String email     = body.get("email");
        String gender    = body.get("gender");
        String region    = body.get("region");
        String tgUsername = body.get("tgUsername");
        String lang      = body.get("lang");

        if (firstName != null && !firstName.isBlank()) user.setFirstName(firstName.trim());
        if (lastName  != null) user.setLastName(lastName.trim());
        if (username  != null && !username.isBlank()) {
            if (userRepo.existsByUsernameAndIdNot(username.trim(), user.getId()))
                throw new IllegalArgumentException("Bu username band");
            user.setUsername(username.trim());
        }
        if (email != null)      user.setEmail(email.trim().isEmpty() ? null : email.trim());
        if (gender != null)     user.setGender(gender.trim().isEmpty() ? null : gender.trim());
        if (region != null)     user.setRegion(region.trim().isEmpty() ? null : region.trim());
        if (tgUsername != null) user.setTgUsername(tgUsername.trim().replaceFirst("^@", "").isEmpty() ? null : tgUsername.trim().replaceFirst("^@",""));
        if (lang != null)       user.setLang(lang);

        userRepo.save(user);
        return getMe(user);
    }

    @Transactional
    public String uploadAvatar(User user, MultipartFile file) throws IOException {
        String ext  = getExt(file.getOriginalFilename());
        String name = "avatar_" + user.getId() + ext;
        Path path = Paths.get(uploadDir, name);
        Files.createDirectories(path.getParent());
        Files.write(path, file.getBytes());
        user.setAvatar(name);
        userRepo.save(user);
        return "/uploads/" + name;
    }

    /** Cloudinary URL ni to'g'ridan DB ga saqlash (fayl yuklamasdan) */
    @Transactional
    public String saveAvatarUrl(User user, String cloudinaryUrl) {
        if (cloudinaryUrl == null || cloudinaryUrl.isBlank())
            throw new IllegalArgumentException("URL bo'sh bo'lmasligi kerak");
        user.setAvatar(cloudinaryUrl);
        userRepo.save(user);
        return cloudinaryUrl;
    }

    public List<UserDto> getRating() {
        return userRepo.findAll().stream()
                .sorted((a, b) -> Integer.compare(b.getScore(), a.getScore()))
                .limit(100)
                .map(u -> UserDto.from(u, (int) productRepo.countByUser(u)))
                .toList();
    }

    private String getExt(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.')).toLowerCase();
    }
}