package com.collection.benefitsjourney.repository;
import com.collection.benefitsjourney.domain.UsersBean;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<UsersBean, Integer> {

    Optional<UsersBean> findByAccount(String account);

    Optional<UsersBean> findByEmail(String email);

    Optional<UsersBean> findByGoogleId(String googleId);

    boolean existsByAccount(String account);

    boolean existsByEmail(String email);

    boolean existsByGoogleId(String googleId);

    boolean existsByRole(String role);
}