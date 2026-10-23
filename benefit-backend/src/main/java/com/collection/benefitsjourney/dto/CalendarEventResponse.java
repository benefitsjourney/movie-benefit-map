package com.collection.benefitsjourney.dto;

import java.time.LocalDateTime;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.domain.MovieBean;

/**
 * 行事曆上的一個事件
 * eventType：
 *   BENEFIT_START 特典開始發放（date = 特典startDate）
 *   BENEFIT_END   特典發放截止（只有「我的追蹤」行事曆才有，date = 特典endDate）
 *   MOVIE_RELEASE 電影上映（date = 電影releaseDate）
 * reminderId：「我的追蹤」行事曆才有值，公開行事曆是null
 */
public record CalendarEventResponse(
        String eventType,
        LocalDateTime date,

        // 特典（電影上映事件時是null）
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

        Integer reminderId) {

    /**
     * 特典事件
     * @param movie 這個特典對應的電影 {benefitId, movieId, movieName}，沒有就傳null
     */
    public static CalendarEventResponse ofBenefit(
            String eventType, LocalDateTime date, BenefitBean b, Object[] movie, Integer reminderId) {
        return new CalendarEventResponse(
                eventType,
                date,
                b.getBenefitId(),
                b.getBenefitName(),
                b.getBenefitType(),
                b.getStatus(),
                b.getIsMemberOnly(),
                b.getStartDate(),
                b.getEndDate(),
                b.getOfficialImageUrl(),
                movie == null ? null : (Integer) movie[1],
                movie == null ? null : (String) movie[2],
                null,
                reminderId);
    }

    /** 電影上映事件 */
    public static CalendarEventResponse ofMovie(MovieBean m, Integer reminderId) {
        return new CalendarEventResponse(
                "MOVIE_RELEASE",
                m.getReleaseDate(),
                null, null, null, null, null, null, null, null,
                m.getMovieId(),
                m.getMovieName(),
                m.getImageUrl(),
                reminderId);
    }
}
