package uz.ekoulash.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.ekoulash.repository.ProductRepository;
import uz.ekoulash.repository.UserRepository;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final UserRepository userRepo;
    private final ProductRepository productRepo;

    @GetMapping
    public ResponseEntity<?> stats() {
        long users    = userRepo.count();
        long products = productRepo.count();
        return ResponseEntity.ok(Map.of(
                "users",    users,
                "products", products,
                "co2",      products * 2
        ));
    }
}
