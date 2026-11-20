package com.jj.moca.core.domain.product.entity.item;

import com.jj.moca.core.domain.product.entity.Product;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@DiscriminatorValue("MOUSE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mouse extends Product {

    private Integer weight; // 무게 ( 54 -> 54g)
    private Integer maxPollingRate; // 최대 폴링레이트 (예: 1000, 4000, 8000Hz)
    private Integer maxDpi; // 최대 감도 (예: 26000 dpi)
    private String gripStyle; // 추천 그립 (예: 팜그립, 클로그립, 핑거팁, 대칭형, 비대칭형)

    public Mouse(String name, String brand, int price, boolean wireless,
                 String description, List<String> features,
                 Integer weight, Integer maxPollingRate, Integer maxDpi, String gripStyle) {
        super(name, brand, price, wireless, description, features);
        this.weight = weight;
        this.maxPollingRate = maxPollingRate;
        this.maxDpi = maxDpi;
        this.gripStyle = gripStyle;
    }
}
