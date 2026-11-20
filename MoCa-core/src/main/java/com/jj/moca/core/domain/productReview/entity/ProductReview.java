package com.jj.moca.core.domain.productReview.entity;

import com.jj.moca.core.common.base.BaseUpdateEntity;
import com.jj.moca.core.domain.product.entity.Product;
import com.jj.moca.core.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "product_reviews",
        indexes = @Index(name = "idx_review_product_rating", columnList = "product_id, rating DESC")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductReview extends BaseUpdateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Double rating;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    public ProductReview(Product product, User user, Double rating, String content) {
        this.product = product;
        this.user = user;
        this.rating = rating;
        this.content = content;
    }
}
