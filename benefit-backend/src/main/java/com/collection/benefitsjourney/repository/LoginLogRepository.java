package com.collection.benefitsjourney.repository;

// import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.collection.benefitsjourney.domain.LoginLogBean;

public interface LoginLogRepository extends JpaRepository<LoginLogBean, Integer> {
    // 查詢指定會員的所有登入紀錄，最新的排在前面
//     List<LoginLogBean> findByUser_UsersIdOrderByLoginTimeDesc(
//             Integer usersId);

    // 新增：指定會員的登入紀錄分頁查詢
    Page<LoginLogBean> findByUser_UsersIdOrderByLoginTimeDesc(
            Integer usersId,
            Pageable pageable);

}