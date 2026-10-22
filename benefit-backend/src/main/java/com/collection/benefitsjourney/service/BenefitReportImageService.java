package com.collection.benefitsjourney.service;

import java.io.IOException;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.exception.ExceptionUtils;
import com.collection.benefitsjourney.repository.UsersRepository;
import com.collection.benefitsjourney.service.CloudinaryClient.UploadedImage;

import lombok.RequiredArgsConstructor;

/**
 * 缺漏特典回報的圖片上傳（會員回報、管理員審核換圖共用）。
 * 只負責把圖片傳到Cloudinary並回傳網址＋publicId，
 * 真正寫進回報單是在 POST /benefit-reports（會員）或 PATCH /benefit-reports/{id}（管理員）。
 */
@Service
@RequiredArgsConstructor
public class BenefitReportImageService {

    /** 跟收藏庫照片一樣只收這四種 */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    private final CloudinaryClient cloudinaryClient;
    private final UsersRepository usersRepository;

    public UploadedImage upload(Integer usersId, MultipartFile file) {
        UsersBean user = usersRepository.findById(usersId)
                .orElseThrow(() -> ExceptionUtils.notFound("找不到會員"));

        // 被停權或已停用的帳號不能上傳（跟回報規則一樣）
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "此帳號目前無法上傳圖片");
        }

        if (file == null || file.isEmpty()) {
            throw ExceptionUtils.badRequest("請選擇要上傳的圖片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw ExceptionUtils.badRequest("只能上傳JPG、PNG、WEBP、GIF格式的圖片");
        }
        if (file.getSize() > CloudinaryClient.MAX_BYTES) {
            throw ExceptionUtils.badRequest("圖片不可超過10MB");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw ExceptionUtils.badRequest("無法讀取上傳的圖片");
        }

        return cloudinaryClient.uploadBytes(bytes, contentType)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "圖片上傳失敗，請稍後再試"));
    }
}
