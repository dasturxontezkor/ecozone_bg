package uz.ekoulash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.ekoulash.entity.Message;
import uz.ekoulash.entity.User;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByUserOrderByCreatedAtAsc(User user);
    List<Message> findByUserAndIdGreaterThanOrderByCreatedAtAsc(User user, Long lastId);
    boolean existsByUser(User user);

}
