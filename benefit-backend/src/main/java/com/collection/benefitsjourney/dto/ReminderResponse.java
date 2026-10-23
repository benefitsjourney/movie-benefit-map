package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.domain.MovieBean;
import com.collection.benefitsjourney.domain.ReminderBean;

/**
 * 我的追蹤清單的一筆資料
 * 追蹤特典時：特典欄位有值，電影欄位是這個特典對應的電影（可能是null）
 * 追蹤電影時：特典欄位都是null
 */
public record ReminderResponse(
        Integer reminderId,
        String reminderType,
        LocalDateTime createdAt,

        // 特典
        Integer benefitId,
        String benefitName,
        String benefitType,
        String benefitStatus,
        Boolean isMemberOnly,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String officialImageUrl,

        // 電影
        Integer movieId,
        String movieName,
        String movieImageUrl,
        LocalDateTime releaseDate) {

    /**
     * @param benefitMovie 追蹤特典時，這個特典對應的電影 {benefitId, movieId, movieName}，沒有就傳null
     */
    public static ReminderResponse from(ReminderBean reminder, Object[] benefitMovie) {
        BenefitBean b = reminder.getBenefit();
        MovieBean m = reminder.getMovie();

        Integer movieId = null;
        String movieName = null;
        String movieImageUrl = null;
        LocalDateTime releaseDate = null;

        if (m != null) {
            movieId = m.getMovieId();
            movieName = m.getMovieName();
            movieImageUrl = m.getImageUrl();
            releaseDate = m.getReleaseDate();
        } else if (benefitMovie != null) {
            movieId = (Integer) benefitMovie[1];
            movieName = (String) benefitMovie[2];
        }

        return new ReminderResponse(
                reminder.getReminderId(),
                reminder.getReminderType(),
                reminder.getCreatedAt(),
                b == null ? null : b.getBenefitId(),
                b == null ? null : b.getBenefitName(),
                b == null ? null : b.getBenefitType(),
                b == null ? null : b.getStatus(),
                b == null ? null : b.getIsMemberOnly(),
                b == null ? null : b.getStartDate(),
                b == null ? null : b.getEndDate(),
                b == null ? null : b.getOfficialImageUrl(),
                movieId,
                movieName,
                movieImageUrl,
                releaseDate);
    }
}
