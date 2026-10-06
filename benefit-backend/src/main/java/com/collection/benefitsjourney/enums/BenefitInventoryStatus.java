package com.collection.benefitsjourney.enums;

import java.util.Arrays;

// 庫存和地圖數字編號要統一
public enum BenefitInventoryStatus {
    UNKNOWN(0), // 未知
    IN_STOCK(1), // 有庫存
    LOW_STOCK(2), // 即將售罄
    EXHAUSTED(3); // 已售罄

    private final int code;

    BenefitInventoryStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static BenefitInventoryStatus fromCode(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .findFirst()
                .orElse(UNKNOWN);
    }
}

