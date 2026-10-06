package com.collection.benefitsjourney.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.collection.benefitsjourney.domain.CinemaBean;

public interface CinemaRepository extends JpaRepository<CinemaBean, Integer> {
    Optional<CinemaBean> findBySourceId(String sourceId);
}
