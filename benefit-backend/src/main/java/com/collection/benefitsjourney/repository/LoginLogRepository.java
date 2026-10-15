package com.collection.benefitsjourney.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.collection.benefitsjourney.domain.LoginLogBean;

public interface LoginLogRepository extends JpaRepository<LoginLogBean, Integer> {

}