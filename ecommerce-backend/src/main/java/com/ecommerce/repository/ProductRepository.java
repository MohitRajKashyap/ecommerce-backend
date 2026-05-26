package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    Page<Product> findByActiveTrue(Pageable pageable);

    Page<Product> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

    Page<Product> findBySellerIdAndActiveTrue(Long sellerId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Product> searchProducts(@Param("query") String query, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND (:minRating IS NULL OR p.averageRating >= :minRating) " +
           "AND (:inStock IS NULL OR (:inStock = true AND p.stockQuantity > 0) OR (:inStock = false))")
    Page<Product> filterProducts(
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minRating") Double minRating,
            @Param("inStock") Boolean inStock,
            Pageable pageable);

    // DSA: Priority Queue simulation — top products by purchase count + rating score
    @Query("SELECT p FROM Product p WHERE p.active = true " +
           "ORDER BY (p.purchaseCount * 0.6 + p.averageRating * p.totalReviews * 0.4) DESC")
    List<Product> findTrendingProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.featured = true AND p.active = true")
    List<Product> findFeaturedProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stockQuantity <= :threshold")
    List<Product> findLowStockProducts(@Param("threshold") int threshold, Pageable pageable);

    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :qty WHERE p.id = :id AND p.stockQuantity >= :qty")
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity + :qty WHERE p.id = :id")
    void incrementStock(@Param("id") Long id, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE Product p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Product p SET p.purchaseCount = p.purchaseCount + :qty WHERE p.id = :id")
    void incrementPurchaseCount(@Param("id") Long id, @Param("qty") long qty);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true")
    long countActiveProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.stockQuantity = 0")
    long countOutOfStockProducts();
}
