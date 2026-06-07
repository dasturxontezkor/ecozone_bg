package uz.ekoulash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import uz.ekoulash.entity.Product;
import uz.ekoulash.entity.ProductLike;
import uz.ekoulash.entity.User;

import java.util.Optional;

public interface ProductLikeRepository extends JpaRepository<ProductLike, Long> {
    Optional<ProductLike> findByProductAndUser(Product product, User user);
    boolean existsByProductAndUser(Product product, User user);

    /** * Mahsulot o'chirilayotganda unga tegishli barcha layklarni tozalash
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM ProductLike pl WHERE pl.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);
}