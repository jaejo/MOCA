package com.jj.moca.core.domain.productReview.repository;

import com.jj.moca.core.domain.productReview.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {

}
