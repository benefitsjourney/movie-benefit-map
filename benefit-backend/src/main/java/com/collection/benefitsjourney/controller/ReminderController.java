package com.collection.benefitsjourney.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.dto.CalendarEventResponse;
import com.collection.benefitsjourney.dto.ReminderCreateRequest;
import com.collection.benefitsjourney.dto.ReminderResponse;
import com.collection.benefitsjourney.service.ReminderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * 追蹤與行事曆
 * 路徑有/reminders（要登入）和/benefits/calendar（公開），所以class上不放@RequestMapping
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "追蹤與行事曆", description = "追蹤特典／電影、行事曆查詢")
public class ReminderController {

    private final ReminderService reminderService;

    @PostMapping("/reminders")
    @Operation(summary = "追蹤特典或電影",
            description = "追蹤特典帶{\"reminderType\":\"BENEFIT\",\"benefitId\":1}；"
                    + "追蹤電影帶{\"reminderType\":\"MOVIE\",\"movieId\":1}。重複追蹤回409")
    public ResponseEntity<ReminderResponse> create(
            @Validated @RequestBody ReminderCreateRequest request,
            HttpServletRequest httpRequest) {

        Integer usersId = getUsersId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reminderService.create(usersId, request));
    }

    @DeleteMapping("/reminders/{reminderId}")
    @Operation(summary = "取消追蹤", description = "只能取消自己的（否則403）")
    public ResponseEntity<Void> delete(
            @PathVariable Integer reminderId,
            HttpServletRequest httpRequest) {

        Integer usersId = getUsersId(httpRequest);
        reminderService.delete(usersId, reminderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reminders/me")
    @Operation(summary = "我的追蹤清單", description = "新到舊，含特典與電影資訊")
    public ResponseEntity<List<ReminderResponse>> findMine(HttpServletRequest httpRequest) {
        Integer usersId = getUsersId(httpRequest);
        return ResponseEntity.ok(reminderService.findMine(usersId));
    }

    @GetMapping("/reminders/calendar")
    @Operation(summary = "我的追蹤行事曆",
            description = "追蹤特典的開始日（BENEFIT_START）、截止日（BENEFIT_END），追蹤電影的上映日（MOVIE_RELEASE）")
    public ResponseEntity<List<CalendarEventResponse>> findMyCalendar(
            @Parameter(description = "月份yyyy-MM，例如2026-10，不帶就是這個月")
            @RequestParam(required = false) String month,
            HttpServletRequest httpRequest) {

        Integer usersId = getUsersId(httpRequest);
        return ResponseEntity.ok(reminderService.findMyCalendar(usersId, month));
    }

    @GetMapping("/benefits/calendar")
    @Operation(summary = "公開行事曆（不用登入）",
            description = "這個月開始發放的特典（BENEFIT_START）＋這個月上映的電影（MOVIE_RELEASE）")
    public ResponseEntity<List<CalendarEventResponse>> findPublicCalendar(
            @Parameter(description = "月份yyyy-MM，例如2026-10，不帶就是這個月")
            @RequestParam(required = false) String month) {

        return ResponseEntity.ok(reminderService.findPublicCalendar(month));
    }

    /** 從JWT Filter放進request的usersId取得登入者（寫法同InventoryReportController） */
    private Integer getUsersId(HttpServletRequest request) {
        Object value = request.getAttribute("usersId");
        if (!(value instanceof Integer)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "尚未取得登入會員資料");
        }
        return (Integer) value;
    }
}
