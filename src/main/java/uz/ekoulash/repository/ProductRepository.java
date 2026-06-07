package uz.ekoulash.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.ekoulash.entity.Product;
import uz.ekoulash.entity.User;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Faol mahsulotlar
    @Query("SELECT p FROM Product p WHERE p.active = true " +
            "AND (COALESCE(:q, '') = '' OR LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "     OR LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "     OR LOWER(p.tags) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "AND (COALESCE(:cat, '') = '' OR p.category = :cat) " +
            "ORDER BY p.createdAt DESC")
    Page<Product> findActive(@Param("q") String q,
                             @Param("cat") String cat,
                             Pageable pageable);

    List<Product> findByUserOrderByCreatedAtDesc(User user);

    Optional<Product> findByActivationCode(String code);

    boolean existsByActivationCode(String code);

    long countByUser(User user);

    long count();

}