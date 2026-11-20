package com.jj.moca.core.domain.product.entity;

import com.jj.moca.core.common.base.BaseUpdateEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "category", discriminatorType = DiscriminatorType.STRING)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseUpdateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, insertable = false, updatable = false)
    private ProductCategory category; // KeyBoard, Mouse, HeadSet

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private boolean wireless; // 유무선 여부

    @Column(columnDefinition = "TEXT")
    private String description; // LLM 시맨틱 검색 및 상품 상세 설명용

    // 공통 특징 태그 (예: ["블루투스", "텐키리스", "가스켓", "초경량"])
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition =  "jsonb")
    private List<String> features = new ArrayList<>();

    protected Product(String name, String brand, int price, boolean wireless,
                      String description, List<String> features) {
        this.name = name;
        this.brand = brand;
        this.price = price;
        this.wireless = wireless;
        this.description = description;
        if (features != null) {
            this.features = features;
        }
    }
}
