package com.collection.benefitsjourney.dto;

import com.collection.benefitsjourney.domain.CinemaMovieBenefitBean;
import com.collection.benefitsjourney.enums.BenefitInventoryStatus;

/** 某影城／電影／特典目前的最新庫存狀態（地圖與特典庫存查詢頁） */
public record InventoryStatusResponse(
        Integer cinemaMovieBenefitId,
        Integer cinemaId,
        String cinemaName,
        Integer movieId,
        String movieName,
        Integer benefitId,
        String benefitName,
        String status) {

    public static InventoryStatusResponse from(CinemaMovieBenefitBean c) {
        return new InventoryStatusResponse(
                c.getCinemaMovieBenefitId(),
                c.getCinema().getCinemaId(),
                c.getCinema().getCinemaName(),
                c.getMovie().getMovieId(),
                c.getMovie().getMovieName(),
                c.getBenefit().getBenefitId(),
                c.getBenefit().getBenefitName(),
                BenefitInventoryStatus.fromCode(c.getBenefitInventoryStatus()).name());
    }
}
