package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.UseBenefitPhotoBean;

/** 持有紀錄的單張照片（不回傳publicId） */
public record UseBenefitPhotoResponse(
        Integer useBenefitPhotoId,
        String photoUrl,
        LocalDateTime createdAt) {

    public static UseBenefitPhotoResponse from(UseBenefitPhotoBean bean) {
        return new UseBenefitPhotoResponse(
                bean.getUseBenefitPhotoId(),
                bean.getUseBenefitPhotoUrl(),
                bean.getCreatedAt());
    }
}