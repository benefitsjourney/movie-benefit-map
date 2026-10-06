package com.collection.benefitsjourney.enums;

// 特典發放類型
public enum BenefitType {
    // 一般場次：固定七天，時間到下架
    REGULAR,
    // 特別場（類型1）：上映日過後下架
    SPECIAL_BY_DATE,
    // 特別場（類型2）：大量存貨發完為止，庫存全沒或下檔才下架
    SPECIAL_UNTIL_OUT
}
