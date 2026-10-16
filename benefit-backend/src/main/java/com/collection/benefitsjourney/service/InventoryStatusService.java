package com.collection.benefitsjourney.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.dto.InventoryStatusResponse;
import com.collection.benefitsjourney.exception.ExceptionUtils;
import com.collection.benefitsjourney.repository.BenefitRepository;
import com.collection.benefitsjourney.repository.CinemaMovieBenefitRepository;
import com.collection.benefitsjourney.repository.CinemaRepository;

import lombok.RequiredArgsConstructor;

/** 查詢目前庫存狀態（讀中介表） */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryStatusService {

    private final CinemaMovieBenefitRepository cinemaMovieBenefitRepository;
    private final BenefitRepository benefitRepository;
    private final CinemaRepository cinemaRepository;

    /** 某特典在哪些影城的庫存狀態 */
    public List<InventoryStatusResponse> findByBenefit(Integer benefitId) {
        if (!benefitRepository.existsById(benefitId)) {
            throw ExceptionUtils.notFound("找不到特典：" + benefitId);
        }
        return cinemaMovieBenefitRepository.findStatusByBenefit(benefitId).stream()
                .map(InventoryStatusResponse::from)
                .toList();
    }

    /** 某影城目前上架中特典的庫存狀態 */
    public List<InventoryStatusResponse> findByCinema(Integer cinemaId) {
        if (!cinemaRepository.existsById(cinemaId)) {
            throw ExceptionUtils.notFound("找不到影城ID：" + cinemaId);
        }
        return cinemaMovieBenefitRepository.findApprovedStatusByCinema(cinemaId).stream()
                .map(InventoryStatusResponse::from)
                .toList();
    }
}