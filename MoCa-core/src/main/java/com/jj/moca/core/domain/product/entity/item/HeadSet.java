package com.jj.moca.core.domain.product.entity.item;

import com.jj.moca.core.domain.product.entity.Product;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@DiscriminatorValue("HEADSET")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HeadSet extends Product {

    private Integer weight;             // 무게
    private boolean noiseCanceling;     // 액티브 노이즈 캔슬링 지원 여부
    private boolean hasMic;         // 탈부착/외장 붐마이크 여부 (게이밍용 vs 음감용)
    private String soundChannel;        // 채널/음장 스펙 (예: 스테레오, 가상 7.1채널, 돌비 애트모스)

    public HeadSet(String name, String brand, int price, boolean wireless,
                   String description, List<String> features,
                   Integer weight, boolean noiseCanceling, boolean hasMic, String soundChannel) {
        super(name, brand, price, wireless, description, features);
        this.weight = weight;
        this.noiseCanceling = noiseCanceling;
        this.hasMic = hasMic;
        this.soundChannel = soundChannel;
    }
}
