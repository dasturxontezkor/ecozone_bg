package uz.ekoulash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import uz.ekoulash.entity.Product;
import uz.ekoulash.entity.ProductMessage;
import uz.ekoulash.entity.User;

import java.util.List;

public interface ProductMessageRepository extends JpaRepository<ProductMessage, Long> {

    List<ProductMessage> findByProductOrderByCreatedAtAsc(Product product);

    List<ProductMessage> findByBuyerOrderByCreatedAtAsc(User buyer);

    @Query("""
        SELECT m FROM ProductMessage m
        WHERE m.product = :product
          AND (m.sender.id = :buyerId OR m.buyer.id = :buyerId)
        ORDER BY m.createdAt ASC
    """)
    List<ProductMessage> findByProductAndBuyer(
            @Param("product") Product product,
            @Param("buyerId") Long buyerId);

    @Query("""
        SELECT DISTINCT m.buyer FROM ProductMessage m
        WHERE m.product = :product AND m.buyer IS NOT NULL
    """)
    List<User> findDistinctBuyersByProduct(@Param("product") Product product);

    /**
     * Mahsulot o'chirilayotganda unga tegishli barcha chat xabarlarini tozalash
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM ProductMessage pm WHERE pm.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);
}