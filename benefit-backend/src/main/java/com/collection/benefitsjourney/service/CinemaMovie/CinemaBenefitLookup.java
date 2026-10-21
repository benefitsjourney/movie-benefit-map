package com.collection.benefitsjourney.service.CinemaMovie;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.collection.benefitsjourney.dto.InventoryStatusResponse;
import com.collection.benefitsjourney.repository.CinemaMovieBenefitRepository;
import com.collection.benefitsjourney.repository.LikeKeywords;

/** 地圖與前台影城列表共用 */
final class CinemaBenefitLookup {

    private CinemaBenefitLookup() {
    }

    static Group load(
            CinemaMovieBenefitRepository cinemaMovieBenefitRepository,
            List<Integer> cinemaIds,
            String movieKeyword,
            String benefitKeyword,
            Boolean hasBenefit) {

        String movieLike = LikeKeywords.toLikeKeyword(movieKeyword);
        String benefitLike = LikeKeywords.toLikeKeyword(benefitKeyword);
        boolean onlyWithBenefits = Boolean.TRUE.equals(hasBenefit)
                || movieLike != null
                || benefitLike != null;

        Map<Integer, List<InventoryStatusResponse>> byCinema = new LinkedHashMap<>();
        if (cinemaIds != null && !cinemaIds.isEmpty()) {
            cinemaMovieBenefitRepository.findApprovedByCinemaIds(cinemaIds, movieLike, benefitLike)
                    .stream()
                    .map(InventoryStatusResponse::from)
                    .forEach(benefit -> byCinema
                            .computeIfAbsent(benefit.cinemaId(), id -> new ArrayList<>())
                            .add(benefit));
        }

        return new Group(byCinema, onlyWithBenefits);
    }

    record Group(
            Map<Integer, List<InventoryStatusResponse>> byCinema,
            boolean onlyWithBenefits) {

        List<InventoryStatusResponse> of(Integer cinemaId) {
            return byCinema.getOrDefault(cinemaId, List.of());
        }

        int count(Integer cinemaId) {
            return of(cinemaId).size();
        }

        /** 有指定特典條件時，沒有符合結果的影城不留下 */
        boolean keep(Integer cinemaId) {
            return !onlyWithBenefits || !of(cinemaId).isEmpty();
        }
    }
}
