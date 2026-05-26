package com.ecommerce.repository;

import com.ecommerce.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductId(Long productId);

    Optional<ProductVariant> findByVariantSku(String variantSku);

    boolean existsByVariantSku(String variantSku);

    List<ProductVariant> findByProductIdAndAttributeName(Long productId, String attributeName);
}
