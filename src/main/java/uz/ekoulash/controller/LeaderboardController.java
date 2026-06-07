package uz.ekoulash.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.ekoulash.entity.User;
import uz.ekoulash.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final UserRepository userRepo;

    @GetMapping
    public ResponseEntity<?> getLeaderboard(@AuthenticationPrincipal User currentUser) {
        // 1. Score bo'yicha eng yuqori 50 ta foydalanuvchini bazadan olamiz
        List<User> topUsers = userRepo.findTop50ByOrderByScoreDesc();

        List<Map<String, Object>> usersList = new ArrayList<>();
        int rank = 1;
        for (User u : topUsers) {
            usersList.add(Map.of(
                    "rank", rank++,
                    "userId", u.getId(),
                    "firstName", u.getFirstName() != null ? u.getFirstName() : "",
                    "lastName", u.getLastName() != null ? u.getLastName() : "",
                    "username", u.getUsername() != null ? u.getUsername() : "",
                    "avatarUrl", u.getAvatar() != null ? u.getAvatar() : "",
                    "score", u.getScore() != null ? u.getScore() : 0
            ));
        }

        // 2. So'rov yuborgan foydalanuvchining o'zini o'rnini (myRank) hisoblaymiz
        Map<String, Object> myRankMap;
        if (currentUser != null) {
            // O'zidan yuqori ball to'plagan foydalanuvchilar sonini sanaymiz
            // Masalan: o'zidan baland ballilar 0 ta bo'lsa, joriy user 1-o'rinda bo'ladi
            long higherScoreCount = userRepo.findAll().stream()
                    .filter(u -> u.getScore() != null && u.getScore() > currentUser.getScore())
                    .count();

            int myRank = (int) higherScoreCount + 1;

            myRankMap = Map.of(
                    "rank", myRank,
                    "score", currentUser.getScore() != null ? currentUser.getScore() : 0
            );
        } else {
            // Agar foydalanuvchi tizimdan o'tmagan yoki token xato bo'lsa default qiymat
            myRankMap = Map.of(
                    "rank", 0,
                    "score", 0
            );
        }

        // Flutter ilovasi kutayotgan JSON struktura
        return ResponseEntity.ok(Map.of(
                "users", usersList,
                "myRank", myRankMap
        ));
    }
}