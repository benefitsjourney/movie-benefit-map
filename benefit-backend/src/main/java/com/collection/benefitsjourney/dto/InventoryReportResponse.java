package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.CinemaMovieBenefitBean;
import com.collection.benefitsjourney.domain.InventoryReportBean;
import com.collection.benefitsjourney.enums.BenefitInventoryStatus;

/** 庫存回報紀錄（新增後回傳、我的回報列表共用） */
public record InventoryReportResponse(
        Integer reportId,
        Integer cinemaMovieBenefitId,
        Integer cinemaId,
        String cinemaName,
        Integer movieId,
        String movieName,
        Integer benefitId,
        String benefitName,
        String status,
        LocalDateTime reportedAt) {

    public static InventoryReportResponse from(InventoryReportBean report) {
        CinemaMovieBenefitBean c = report.getCinemaMovieBenefit();
        return new InventoryReportResponse(
                report.getReportId(),
                c.getCinemaMovieBenefitId(),
                c.getCinema().getCinemaId(),
                c.getCinema().getCinemaName(),
                c.getMovie().getMovieId(),
                c.getMovie().getMovieName(),
                c.getBenefit().getBenefitId(),
                c.getBenefit().getBenefitName(),
                BenefitInventoryStatus.fromCode(report.getStatus()).name(),
                report.getReportedAt());
    }
}
