package com.collection.benefitsjourney.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.collection.benefitsjourney.domain.PasswordResetTokenBean;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetTokenBean, Integer> {

    Optional<PasswordResetTokenBean> findByTokenHash(String tokenHash);

    List<PasswordResetTokenBean> findAllByUsersIdAndUsedFalse(Integer usersId);
}