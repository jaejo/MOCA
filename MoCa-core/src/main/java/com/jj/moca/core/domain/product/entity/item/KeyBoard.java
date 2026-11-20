package com.jj.moca.core.domain.product.entity.item;

import com.jj.moca.core.domain.product.entity.Product;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@DiscriminatorValue("KEYBOARD")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KeyBoard extends Product {

    private String switchType;  // 스위치 타입
    private String layout;      // 배열

    public KeyBoard(String name, String brand, int price, boolean wireless,
                    String description, List<String> features,
                    String switchType, String layout) {
        super(name, brand, price, wireless, description, features);
        this.switchType = switchType;
        this.layout = layout;
    }
}
