package com.collection.benefitsjourney.service;

import static com.collection.benefitsjourney.exception.ExceptionUtils.badRequest;
import static com.collection.benefitsjourney.exception.ExceptionUtils.conflict;
import static com.collection.benefitsjourney.exception.ExceptionUtils.notFound;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.domain.MovieBean;
import com.collection.benefitsjourney.domain.ReminderBean;
import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.dto.CalendarEventResponse;
import com.collection.benefitsjourney.dto.ReminderCreateRequest;
import com.collection.benefitsjourney.dto.ReminderResponse;
import com.collection.benefitsjourney.enums.ReminderType;
import com.collection.benefitsjourney.repository.BenefitRepository;
import com.collection.benefitsjourney.repository.CinemaMovieBenefitRepository;
import com.collection.benefitsjourney.repository.MovieRepository;
import com.collection.benefitsjourney.repository.ReminderRepository;
import com.collection.benefitsjourney.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

/** 追蹤特典／電影，以及行事曆查詢 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReminderService {

    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");

    private final ReminderRepository reminderRepository;
    private final UsersRepository usersRepository;
    private final BenefitRepository benefitRepository;
    private final MovieRepository movieRepository;
    private final CinemaMovieBenefitRepository cinemaMovieBenefitRepository;

    // ===================== 追蹤 =====================

    /** 新增追蹤：重複追蹤回409 */
    @Transactional
    public ReminderResponse create(Integer userId, ReminderCreateRequest request) {
        UsersBean user = usersRepository.findById(userId)
                .orElseThrow(() -> notFound("找不到會員"));

        ReminderBean reminder = new ReminderBean();
        reminder.setUser(user);
        reminder.setReminderType(request.reminderType().name());

        if (request.reminderType() == ReminderType.BENEFIT) {
            if (request.benefitId() == null) {
                throw badRequest("請選擇要追蹤的特典");
            }
            BenefitBean benefit = benefitRepository.findById(request.benefitId())
                    .orElseThrow(() -> notFound("找不到特典ID：" + request.benefitId()));
            if (reminderRepository.existsByUser_UsersIdAndBenefit_BenefitId(userId, benefit.getBenefitId())) {
                throw conflict("已經追蹤過這個特典");
            }
            reminder.setBenefit(benefit);
        } else {
            if (request.movieId() == null) {
                throw badRequest("請選擇要追蹤的電影");
            }
            MovieBean movie = movieRepository.findById(request.movieId())
                    .orElseThrow(() -> notFound("找不到電影ID：" + request.movieId()));
            if (reminderRepository.existsByUser_UsersIdAndMovie_MovieId(userId, movie.getMovieId())) {
                throw conflict("已經追蹤過這部電影");
            }
            reminder.setMovie(movie);
        }

        ReminderBean saved;
        try {
            saved = reminderRepository.saveAndFlush(reminder);
        } catch (DataIntegrityViolationException e) {
            // 同時按兩次時，資料庫的UNIQUE會擋下第二筆
            throw conflict("已經追蹤過了");
        }

        Map<Integer, Object[]> movies = saved.getBenefit() == null
                ? Map.of()
                : movieMap(List.of(saved.getBenefit().getBenefitId()));
        return ReminderResponse.from(saved,
                saved.getBenefit() == null ? null : movies.get(saved.getBenefit().getBenefitId()));
    }

    /** 取消追蹤：只能刪自己的（否則403） */
    @Transactional
    public void delete(Integer userId, Integer reminderId) {
        ReminderBean reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> notFound("找不到追蹤紀錄"));
        if (!reminder.getUser().getUsersId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能取消自己的追蹤");
        }
        reminderRepository.delete(reminder);
    }

    /** 我的追蹤清單（新到舊） */
    public List<ReminderResponse> findMine(Integer userId) {
        List<ReminderBean> reminders = reminderRepository.findMine(userId);
        Map<Integer, Object[]> movies = movieMap(benefitIdsOf(reminders));

        return reminders.stream()
                .map(r -> ReminderResponse.from(r,
                        r.getBenefit() == null ? null : movies.get(r.getBenefit().getBenefitId())))
                .toList();
    }

    // ===================== 行事曆 =====================

    /** 公開行事曆：這個月開始發放的特典＋這個月上映的電影 */
    public List<CalendarEventResponse> findPublicCalendar(String month) {
        YearMonth ym = parseMonth(month);
        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to = ym.plusMonths(1).atDay(1).atStartOfDay();

        List<BenefitBean> benefits = reminderRepository.findBenefitsStartingBetween(from, to);
        Map<Integer, Object[]> movies = movieMap(benefits.stream().map(BenefitBean::getBenefitId).toList());

        List<CalendarEventResponse> events = new ArrayList<>();
        for (BenefitBean b : benefits) {
            events.add(CalendarEventResponse.ofBenefit(
                    "BENEFIT_START", b.getStartDate(), b, movies.get(b.getBenefitId()), null));
        }
        for (MovieBean m : reminderRepository.findMoviesReleasedBetween(from, to)) {
            events.add(CalendarEventResponse.ofMovie(m, null));
        }

        events.sort(Comparator.comparing(CalendarEventResponse::date));
        return events;
    }

    /** 我的追蹤行事曆：追蹤的特典（開始日＋截止日）、追蹤的電影（上映日） */
    public List<CalendarEventResponse> findMyCalendar(Integer userId, String month) {
        YearMonth ym = parseMonth(month);
        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to = ym.plusMonths(1).atDay(1).atStartOfDay();

        List<ReminderBean> reminders = reminderRepository.findMine(userId);
        Map<Integer, Object[]> movies = movieMap(benefitIdsOf(reminders));

        List<CalendarEventResponse> events = new ArrayList<>();
        for (ReminderBean r : reminders) {
            BenefitBean b = r.getBenefit();
            MovieBean m = r.getMovie();

            if (b != null) {
                Object[] movie = movies.get(b.getBenefitId());
                if (inRange(b.getStartDate(), from, to)) {
                    events.add(CalendarEventResponse.ofBenefit(
                            "BENEFIT_START", b.getStartDate(), b, movie, r.getReminderId()));
                }
                if (inRange(b.getEndDate(), from, to)) {
                    events.add(CalendarEventResponse.ofBenefit(
                            "BENEFIT_END", b.getEndDate(), b, movie, r.getReminderId()));
                }
            } else if (m != null && inRange(m.getReleaseDate(), from, to)) {
                events.add(CalendarEventResponse.ofMovie(m, r.getReminderId()));
            }
        }

        events.sort(Comparator.comparing(CalendarEventResponse::date));
        return events;
    }

    // ===================== 小工具 =====================

    /** month格式yyyy-MM（例如2026-10），沒帶就是這個月 */
    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now(TAIPEI);
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException e) {
            throw badRequest("month格式要是yyyy-MM，例如2026-10");
        }
    }

    private boolean inRange(LocalDateTime date, LocalDateTime from, LocalDateTime to) {
        return date != null && !date.isBefore(from) && date.isBefore(to);
    }

    private List<Integer> benefitIdsOf(List<ReminderBean> reminders) {
        return reminders.stream()
                .filter(r -> r.getBenefit() != null)
                .map(r -> r.getBenefit().getBenefitId())
                .toList();
    }

    /** 特典ID → {benefitId, movieId, movieName}（一個特典通常只對應一部電影，取第一筆） */
    private Map<Integer, Object[]> movieMap(List<Integer> benefitIds) {
        Map<Integer, Object[]> map = new HashMap<>();
        if (benefitIds.isEmpty()) {
            return map;
        }
        for (Object[] row : cinemaMovieBenefitRepository.findMoviesByBenefitIds(benefitIds)) {
            map.putIfAbsent((Integer) row[0], row);
        }
        return map;
    }
}
