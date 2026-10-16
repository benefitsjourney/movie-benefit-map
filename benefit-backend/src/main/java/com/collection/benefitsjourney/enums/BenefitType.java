package com.collection.benefitsjourney.enums;

// 特典發放類型
public enum BenefitType {
    // 一般場次：固定七天，時間到下架
    REGULAR,
    // 特別場（入場贈送型）：依特定活動日期管理，活動日結束後即下架
    SPECIAL_BY_DATE,
    // 特別場（限量兌換型）：指定日期開放兌換，送完為止（無明確截止日，若一直沒有送完的資訊，最晚在開始日期後20週自動下架）
    SPECIAL_UNTIL_OUT
}
