package com.collection.benefitsjourney.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.dto.BenefitReportCreateRequest;
import com.collection.benefitsjourney.dto.BenefitReportUserResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.service.BenefitReportService;
import com.collection.benefitsjourney.service.BenefitReportImageService;
import com.collection.benefitsjourney.service.CloudinaryClient.UploadedImage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/benefit-reports")
@RequiredArgsConstructor
@Tag(name = "特典回報（會員）", description = "會員回報缺漏特典、查看自己的回報")
public class BenefitReportController {

    private final BenefitReportService benefitReportService;
    private final BenefitReportImageService benefitReportImageService;

    @PostMapping
    @Operation(summary = "新增缺漏特典回報",
            description = "電影、影城二選一：有id就帶id，資料庫沒有才填自訂名稱。送出後狀態為PENDING，等待管理員審核")
    public ResponseEntity<BenefitReportUserResponse> create(
            @Validated @RequestBody BenefitReportCreateRequest request,
            HttpServletRequest httpRequest) {

        Integer usersId = (Integer) httpRequest.getAttribute("usersId");
        return ResponseEntity.ok(benefitReportService.create(usersId, request));
    }

    @GetMapping("/me")
    @Operation(summary = "查看自己回報的特典", description = "可看到審核狀態與管理員填寫的駁回理由（reviewNote）")
    public ResponseEntity<PageResponse<BenefitReportUserResponse>> findMyReports(
            @Parameter(description = "頁碼") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每頁筆數") @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {

        Integer usersId = (Integer) httpRequest.getAttribute("usersId");
        return ResponseEntity.ok(benefitReportService.findMyReports(usersId, page, size));
    }
    
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上傳特典回報圖片",
            description = "會員回報、管理員審核換圖共用。只接受JPG、PNG、WEBP、GIF，最大10MB。"
                    + "回傳url與publicId，再放進新增回報或修改回報的body")
    public ResponseEntity<UploadedImage> uploadImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {

        Object usersId = httpRequest.getAttribute("usersId");
        if (!(usersId instanceof Integer id)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "尚未取得登入會員資料");
        }
        return ResponseEntity.ok(benefitReportImageService.upload(id, file));
    }
}