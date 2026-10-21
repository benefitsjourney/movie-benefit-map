package com.collection.benefitsjourney.dto.CinemaMovie;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.enums.County;

/** 前台影城探索卡片需要的欄位，含上架特典筆數 */
public record CinemaExploreResponse(
        Integer cinemaId,
        String cinemaName,
        County county,
        String cinemaAddress,
        String officialWebsiteUrl,
        int benefitCount) {

    public static CinemaExploreResponse from(CinemaBean cinema, int benefitCount) {
        return new CinemaExploreResponse(
                cinema.getCinemaId(),
                cinema.getCinemaName(),
                cinema.getCounty(),
                cinema.getCinemaAddress(),
                cinema.getOfficialWebsiteUrl(),
                benefitCount);
    }
}
