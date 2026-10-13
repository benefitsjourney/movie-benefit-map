
package com.collection.benefitsjourney.service;

import static com.collection.benefitsjourney.exception.ExceptionUtils.badRequest;
import static com.collection.benefitsjourney.exception.ExceptionUtils.conflict;
import static com.collection.benefitsjourney.exception.ExceptionUtils.notFound;

import java.util.Optional;

import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;

import com.collection.benefitsjourney.domain.CinemaBean;
import com.collection.benefitsjourney.dto.CinemaRequest;
import com.collection.benefitsjourney.dto.CinemaResponse;
import com.collection.benefitsjourney.dto.PageResponse;
import com.collection.benefitsjourney.enums.CinemaSort;
import com.collection.benefitsjourney.enums.County;
import com.collection.benefitsjourney.enums.SortOrder;
import com.collection.benefitsjourney.repository.CinemaRepository;
import com.collection.benefitsjourney.repository.LikeKeywords;

@Service
public class CinemaService {
    private final CinemaRepository cinemaRepository;

    public CinemaService(CinemaRepository cinemaRepository) {
        this.cinemaRepository = cinemaRepository;
    }

    /** 取得所有影城資料 */
    public PageResponse<CinemaResponse> findAll(
            String keyword,
            County county,
            Boolean isActive,
            Boolean isVisible,
            int page,
            int size,
            CinemaSort.SortBy orderBy,
            SortOrder sort) {

        PageRequest pageable = PageRequest.of(
                page - 1,
                size,
                cinemaSort(orderBy, sort));

        Page<CinemaResponse> result = cinemaRepository.findAllByFilters(
                LikeKeywords.toLikeKeyword(keyword),
                county,
                isActive,
                isVisible,
                pageable)
                .map(CinemaResponse::from);

        return PageResponse.from(result);
    }

    /** 縣市依由北到南的順序，其餘欄位依實體屬性排序 */
    private Sort cinemaSort(CinemaSort.SortBy orderBy, SortOrder sort) {
        Sort.Direction direction = sort == SortOrder.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        if (orderBy == CinemaSort.SortBy.COUNTY) {
            // 整個 CASE 必須包在括號裡。Spring Data 看到不含括號的排序欄位，會再把查詢別名接在最前面。
            String northToSouth = "(" + County.northToSouthExpression("c.county") + ")";
            return JpaSort.unsafe(direction, northToSouth)
                    .and(cinemaIdAsc());
        }

        Sort primary = Sort.by(direction, orderBy.getProperty());
        if (orderBy == CinemaSort.SortBy.CINEMA_ID) {
            return primary;
        }
        return primary.and(cinemaIdAsc());
    }

    /** 次排序固定依影城 ID 升冪，避免分頁結果順序不穩定 */
    private static Sort cinemaIdAsc() {
        return Sort.by(
                Sort.Direction.ASC,
                TypedPropertyPath.path((CinemaBean cinema) -> cinema.getCinemaId()));
    }

    /** 取得指定id影城資料 */
    public CinemaResponse findById(Integer id) {
        CinemaBean cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> notFound("找不到影城 ID：" + id));

        return CinemaResponse.from(cinema);
    }

    /** 新增影城 */
    public CinemaResponse createCinema(CinemaRequest request) {
        if (request == null) {
            throw badRequest("請求內容不可為空");
        }

        if (request.cinemaName() == null
                || request.cinemaName().isBlank()) {
            throw badRequest("影城名稱不可為空");
        }

        if (request.county() == null) {
            throw badRequest("縣市不可為空");
        }

        if (request.sourceId() != null
                && cinemaRepository.findBySourceId(request.sourceId()).isPresent()) {
            throw conflict("來源 ID 已存在：" + request.sourceId());
        }

        CinemaBean cinema = new CinemaBean();

        cinema.setSourceId(request.sourceId());
        cinema.setCinemaName(request.cinemaName());
        cinema.setCounty(request.county());
        cinema.setCinemaAddress(request.cinemaAddress());
        cinema.setLatitude(request.latitude());
        cinema.setLongitude(request.longitude());
        cinema.setOfficialWebsiteUrl(request.officialWebsiteUrl());

        cinema.setIsActive(
                request.isActive() != null
                        ? request.isActive()
                        : true);

        cinema.setIsVisible(
                request.isVisible() != null
                        ? request.isVisible()
                        : true);

        CinemaBean savedCinema = cinemaRepository.save(cinema);

        return CinemaResponse.from(savedCinema);
    }

    /** 修改影城 */
    public CinemaResponse updateCinema(Integer id, CinemaRequest request) {
        if (request == null) {
            throw badRequest("請求內容不可為空");
        }

        if (request.cinemaName() != null
                && request.cinemaName().isBlank()) {
            throw badRequest("影城名稱不可為空");
        }

        CinemaBean cinema = cinemaRepository.findById(id)
                .orElseThrow(() -> notFound("找不到影城ID：" + id));

        if (request.sourceId() != null) {
            // sourceId有修改時檢查是否與其他影城重複
            cinemaRepository.findBySourceId(request.sourceId())
                    .filter(existing -> !existing.getCinemaId().equals(id))
                    .ifPresent(existing -> {
                        throw conflict("來源ID已存在：" + request.sourceId());
                    });

            cinema.setSourceId(request.sourceId());
        }

        if (request.cinemaName() != null) {
            cinema.setCinemaName(request.cinemaName());
        }

        if (request.cinemaAddress() != null) {
            cinema.setCinemaAddress(request.cinemaAddress());
        }

        if (request.latitude() != null) {
            cinema.setLatitude(request.latitude());
        }

        if (request.longitude() != null) {
            cinema.setLongitude(request.longitude());
        }

        if (request.officialWebsiteUrl() != null) {
            cinema.setOfficialWebsiteUrl(request.officialWebsiteUrl());
        }

        if (request.county() != null) {
            cinema.setCounty(request.county());
        }

        if (request.isActive() != null) {
            cinema.setIsActive(request.isActive());
        }

        if (request.isVisible() != null) {
            cinema.setIsVisible(request.isVisible());
        }

        CinemaBean savedCinema = cinemaRepository.save(cinema);

        return CinemaResponse.from(savedCinema);
    }

    /** 根據來源ID取得影城 */
    public Optional<CinemaResponse> findBySourceId(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            return Optional.empty();
        }

        return cinemaRepository.findBySourceId(sourceId)
                .map(CinemaResponse::from);
    }
}