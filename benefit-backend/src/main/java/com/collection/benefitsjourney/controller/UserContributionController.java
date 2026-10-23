package com.collection.benefitsjourney.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.collection.benefitsjourney.dto.UserContributionRankingResponse;
import com.collection.benefitsjourney.service.UserContributionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 貢獻排行榜（不用登入）
 * 路徑在 /users 底下，但為了不動到 UserController（共用檔），另外開一個 Controller。
 * /users/report-ranking 是固定字，Spring 會優先對到這裡，不會被 /users/{id} 吃掉。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "貢獻排行", description = "使用者貢獻排行榜（不用登入）")
public class UserContributionController {

    private final UserContributionService userContributionService;

    @GetMapping("/users/report-ranking")
    @Operation(summary = "貢獻排行榜",
            description = "貢獻＝庫存回報＋審核通過的缺漏特典回報，每筆 +1。"
                    + "period：weekly（本週，週一起算）／monthly（本月）／all（全部），預設 weekly。"
                    + "次數一樣同名次。帶 userId 會在 me 欄位回傳該會員的名次（沒貢獻是 null）")
    public ResponseEntity<UserContributionRankingResponse> getRanking(
            @Parameter(description = "weekly／monthly／all")
            @RequestParam(defaultValue = "weekly") String period,
            @Parameter(description = "回傳前幾名（1～50，預設10）")
            @RequestParam(defaultValue = "10") Integer limit,
            @Parameter(description = "（可選）要查名次的會員ID，前端用來標出「我的名次」")
            @RequestParam(required = false) Integer userId) {

        return ResponseEntity.ok(userContributionService.getRanking(period, limit, userId));
    }
}
