package uz.ekoulash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.ekoulash.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);
    Optional<User> findByUsername(String username);
    Optional<User> findByUid(String uid);
    boolean existsByUsername(String username);
    boolean existsByUsernameAndIdNot(String username, Long id);
    long countByRole(User.Role role);
    List<User> findTop50ByOrderByScoreDesc();
}
