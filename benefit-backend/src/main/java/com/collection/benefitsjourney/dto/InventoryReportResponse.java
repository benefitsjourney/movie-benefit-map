package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.CinemaMovieBenefitBean;
import com.collection.benefitsjourney.domain.InventoryReportBean;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.enums.BenefitInventoryStatus;

/** 庫存回報紀錄（新增後回傳、我的回報列表、管理員列表共用） */
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
        LocalDateTime reportedAt,
        Integer reporterId,
        String reporterName) {

    public static InventoryReportResponse from(InventoryReportBean report) {
        CinemaMovieBenefitBean c = report.getCinemaMovieBenefit();
        UsersBean user = report.getUser();
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
                report.getReportedAt(),
                user == null ? null : user.getUsersId(),
                reporterName(user));
    }

    /** 顯示用名稱：暱稱→帳號→Email（Google登入的可能沒帳號，Email必填所以一定有值） */
    private static String reporterName(UsersBean user) {
        if (user == null) {
            return null;
        }
        if (user.getAccount() != null && !user.getAccount().isBlank()) {
            return user.getAccount();
        }
        return user.getEmail();
    }
}