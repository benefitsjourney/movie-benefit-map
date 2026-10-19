package com.collection.benefitsjourney.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collection.benefitsjourney.domain.BenefitBean;
import com.collection.benefitsjourney.domain.CinemaMovieBenefitBean;
import com.collection.benefitsjourney.exception.ExceptionUtils;
import com.collection.benefitsjourney.repository.BenefitRepository;
import com.collection.benefitsjourney.repository.CinemaMovieBenefitRepository;
import com.collection.benefitsjourney.service.BenefitSeedSqlGenerator.SqlFiles;

import lombok.RequiredArgsConstructor;

/** 從資料庫匯出「已確認的特典」SQL，打包成zip（內含benefit.sql、cinema_movie_benefit.sql） */
@Service
@RequiredArgsConstructor
public class BenefitSeedSqlService {

    private final BenefitRepository benefitRepository;
    private final CinemaMovieBenefitRepository linkRepository;

    @Transactional(readOnly = true)
    public byte[] exportZip(boolean includeExpired) {
        Set<String> statuses = includeExpired ? Set.of("APPROVED", "EXPIRED") : Set.of("APPROVED");
        List<BenefitBean> benefits = benefitRepository.findAll().stream()
                .filter(b -> statuses.contains(b.getStatus()))
                .sorted((a, b) -> a.getBenefitId().compareTo(b.getBenefitId()))
                .toList();
        Set<Integer> ids = benefits.stream().map(BenefitBean::getBenefitId).collect(java.util.stream.Collectors.toSet());
        List<CinemaMovieBenefitBean> links = linkRepository.findAll().stream()
                .filter(l -> ids.contains(l.getBenefit().getBenefitId()))
                .toList();

        SqlFiles sql = BenefitSeedSqlGenerator.generate(benefits, links);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            add(zip, "benefit.sql", sql.benefitSql());
            add(zip, "cinema_movie_benefit.sql", sql.cinemaMovieBenefitSql());
            zip.finish();
            return out.toByteArray();
        } catch (IOException e) {
            throw ExceptionUtils.badRequest("產生SQL失敗：" + e.getMessage());
        }
    }

    private void add(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}