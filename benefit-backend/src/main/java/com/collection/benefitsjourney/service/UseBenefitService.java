package com.collection.benefitsjourney.service;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.domain.UseBenefitBean;
import com.collection.benefitsjourney.domain.UseBenefitPhotoBean;
import com.collection.benefitsjourney.dto.UseBenefitCreateRequest;
import com.collection.benefitsjourney.dto.UseBenefitPhotoResponse;
import com.collection.benefitsjourney.dto.UseBenefitResponse;
import com.collection.benefitsjourney.exception.ExceptionUtils;
import com.collection.benefitsjourney.repository.BenefitRepository;
import com.collection.benefitsjourney.repository.ExchangeListingRepository;
import com.collection.benefitsjourney.repository.UseBenefitRepository;
import com.collection.benefitsjourney.repository.UsersRepository;
import com.collection.benefitsjourney.service.CloudinaryClient.UploadedImage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 持有紀錄（收藏庫）：新增、查自己的清單、刪除 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UseBenefitService {
	
    /** 一筆持有紀錄最多幾張照片（避免Cloudinary用量過大） */
    static final int MAX_PHOTOS = 5;

    private final UseBenefitRepository useBenefitRepository;
    private final BenefitRepository benefitRepository;
    private final UsersRepository usersRepository;
    private final ExchangeListingRepository exchangeListingRepository;
    private final CloudinaryClient cloudinaryClient;

    /**
     * 新增持有紀錄。
     * 已上架、已下架的特典都可以新增。
     * 配合交換設計，同一個特典只能有一筆。
     */
    @Transactional
    public UseBenefitResponse create(Integer usersId, UseBenefitCreateRequest request) {
        BenefitBean benefit = benefitRepository.findById(request.benefitId())
                .orElseThrow(() -> ExceptionUtils.notFound("找不到特典：" + request.benefitId()));

        if (useBenefitRepository.existsByUser_UsersIdAndBenefit_BenefitId(usersId, request.benefitId())) {
            throw ExceptionUtils.conflict("已經收藏過這個特典");
        }

        UseBenefitBean useBenefit = new UseBenefitBean();
        // 只需要會員ID，不用另外查詢會員的完整資料
        useBenefit.setUser(usersRepository.getReferenceById(usersId));
        useBenefit.setBenefit(benefit);

        return UseBenefitResponse.from(useBenefitRepository.save(useBenefit));
    }

    /** 查登入者自己的持有清單（新到舊，不分特典上下架） */
    @Transactional(readOnly = true)
    public List<UseBenefitResponse> findMine(Integer usersId) {
        return useBenefitRepository.findMyUseBenefits(usersId)
                .stream()
                .map(UseBenefitResponse::from)
                .toList();
    }

    /**
     * 刪除持有紀錄。
     * 只能刪自己的，已經拿去建立交換刊登的不能刪（交換刊登綁著這筆持有紀錄）。
     * 照片資料列會跟著一起刪（UseBenefitBean的photos有設orphanRemoval），Cloudinary上的檔案也會刪掉。
     */
    @Transactional
    public void delete(Integer usersId, Integer useBenefitId) {
        UseBenefitBean useBenefit = getMineOrThrow(usersId, useBenefitId);

        if (exchangeListingRepository.findByUseBenefitBean(useBenefit).isPresent()) {
            throw ExceptionUtils.conflict("這筆持有紀錄已建立交換刊登，無法刪除");
        }
        // 先記下要刪的Cloudinary檔案
        List<String> publicIds = useBenefit.getPhotos().stream()
                .map(UseBenefitPhotoBean::getPublicId)
                .toList();

        // 先刪資料庫並立刻送出（flush），確定資料庫刪得掉，才去刪Cloudinary的檔案
        useBenefitRepository.delete(useBenefit);
        useBenefitRepository.flush();

        publicIds.forEach(this::deleteCloudinaryQuietly);
    }
    

    /**
     * 新增一張照片：先上傳到Cloudinary，再存網址跟publicId到資料庫。
     * 只接受圖片、最大10MB、一筆持有紀錄最多5張。
     */
    @Transactional
    public UseBenefitPhotoResponse addPhoto(Integer usersId, Integer useBenefitId, MultipartFile file) {
        UseBenefitBean useBenefit = getMineOrThrow(usersId, useBenefitId);

        if (file == null || file.isEmpty()) {
            throw ExceptionUtils.badRequest("請選擇要上傳的照片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw ExceptionUtils.badRequest("只能上傳圖片檔");
        }
        if (file.getSize() > CloudinaryClient.MAX_BYTES) {
            throw ExceptionUtils.badRequest("照片不可超過10MB");
        }
        if (useBenefit.getPhotos().size() >= MAX_PHOTOS) {
            throw ExceptionUtils.badRequest("每筆收藏最多只能有" + MAX_PHOTOS + "張照片");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw ExceptionUtils.badRequest("無法讀取上傳的照片");
        }

        UploadedImage image = cloudinaryClient.uploadBytes(bytes, contentType)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "照片上傳失敗，請稍後再試"));

        UseBenefitPhotoBean photo = new UseBenefitPhotoBean();
        photo.setUseBenefit(useBenefit);
        photo.setUseBenefitPhotoUrl(image.url());
        photo.setPublicId(image.publicId());
        useBenefit.getPhotos().add(photo);

        try {
            // saveAndFlush：立刻寫進資料庫，photo才會拿到ID和建立時間
            useBenefitRepository.saveAndFlush(useBenefit);
        } catch (RuntimeException e) {
            // 資料庫存失敗，把剛上傳的檔案刪掉，避免Cloudinary留下沒人用的檔案
            deleteCloudinaryQuietly(image.publicId());
            throw e;
        }
        return UseBenefitPhotoResponse.from(photo);
    }

    /** 刪除一張照片：先刪資料庫，再刪Cloudinary的檔案 */
    @Transactional
    public void deletePhoto(Integer usersId, Integer useBenefitId, Integer photoId) {
        UseBenefitBean useBenefit = getMineOrThrow(usersId, useBenefitId);

        // 照片一定要屬於這筆持有紀錄，避免拿別人的照片ID來刪
        UseBenefitPhotoBean photo = useBenefit.getPhotos().stream()
                .filter(p -> p.getUseBenefitPhotoId().equals(photoId))
                .findFirst()
                .orElseThrow(() -> ExceptionUtils.notFound("這筆收藏沒有這張照片：" + photoId));

        String publicId = photo.getPublicId();
        useBenefit.getPhotos().remove(photo);   // orphanRemoval會刪掉這筆照片資料列
        useBenefitRepository.saveAndFlush(useBenefit);

        deleteCloudinaryQuietly(publicId);
    }

    /** 找持有紀錄，並確認是登入者自己的 */
    UseBenefitBean getMineOrThrow(Integer usersId, Integer useBenefitId) {
        UseBenefitBean useBenefit = useBenefitRepository.findById(useBenefitId)
                .orElseThrow(() -> ExceptionUtils.notFound("找不到持有紀錄：" + useBenefitId));

        if (!useBenefit.getUser().getUsersId().equals(usersId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能操作自己的持有紀錄");
        }
        return useBenefit;
    }
    

    /**
     * 刪Cloudinary檔案，失敗只寫log不丟例外。
     * 資料庫已經刪掉了，不能因為Cloudinary連不上就讓使用者刪除失敗。
     */
    private void deleteCloudinaryQuietly(String publicId) {
        if (!cloudinaryClient.delete(publicId)) {
            log.warn("Cloudinary照片沒有刪除成功，publicId={}（可以到Cloudinary後台手動刪）", publicId);
        }
    }
}