package com.collection.benefitsjourney.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.dto.UpdateProfileRequest;
import com.collection.benefitsjourney.repository.UsersRepository;

@Service
public class UsersService {

    private final UsersRepository usersRepository;

    // 建構子注入 UsersRepository
    public UsersService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    public UsersBean getUserById(Integer usersId) {

    return usersRepository.findById(usersId)
            .orElseThrow(() ->
                    new IllegalArgumentException("找不到會員")
            );
    }

    // 查詢所有會員
    public List<UsersBean> findAll() {
        return usersRepository.findAll();
    }

    // 依帳號查詢
    public Optional<UsersBean> findByAccount(String account) {
        return usersRepository.findByAccount(account);
    }

    // 依 Email 查詢
    public Optional<UsersBean> findByEmail(String email) {
        return usersRepository.findByEmail(email);
    }

    // 依 Google ID 查詢
    public Optional<UsersBean> findByGoogleId(String googleId) {
        return usersRepository.findByGoogleId(googleId);
    }

    // 新增會員
    public UsersBean save(UsersBean usersBean) {
        return usersRepository.save(usersBean);
    }

    // 修改會員
    public UsersBean update(UsersBean usersBean) {
        return usersRepository.save(usersBean);
    }

    public UsersBean updateProfile(
            Integer usersId,
            UpdateProfileRequest request) {

        // 先找到目前登入的會員
        UsersBean usersBean =
                usersRepository.findById(usersId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("找不到會員")
                        );

        // 如果 Email 有修改，確認新的 Email 沒有被其他會員使用
        if (!usersBean.getEmail().equals(request.getEmail())
                && usersRepository.existsByEmail(request.getEmail())) {

            throw new IllegalArgumentException("Email 已存在");
        }

        // 修改允許修改的資料
        usersBean.setName(request.getName());
        usersBean.setEmail(request.getEmail());
        usersBean.setBirthDate(request.getBirthDate());

        // 更新修改時間
        usersBean.setUpdatedAt(LocalDateTime.now());

        // 使用原本的 update() 方法存回資料庫
        return update(usersBean);
    }

    public UsersBean updateStatus(Integer usersId, String status) {

    UsersBean usersBean = usersRepository.findById(usersId)
                    .orElseThrow(() ->
                            new IllegalArgumentException("找不到會員")
                    );
                    
                    if ("BANNED".equals(usersBean.getStatus())) {
                        throw new IllegalArgumentException(
                            "此帳號已被管理員停權"
                        );
                    }

                    // 不允許重複設定相同狀態
                    if (usersBean.getStatus().equals(status)) {
                        throw new IllegalArgumentException(
                            "帳號目前已經是此狀態"
                    );
                    }
                    
                    // 會員只能設定 ACTIVE 或 INACTIVE
                    if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
                        throw new IllegalArgumentException(
                                "會員只能自行停用或啟用帳號"
                        );
                    }
                    
        usersBean.setStatus(status);
        usersBean.setUpdatedAt(LocalDateTime.now());

        return usersRepository.save(usersBean);
        }


    //檢查是否為管理員
    public UsersBean checkAdmin(Integer usersId) {

        UsersBean usersBean =
                usersRepository.findById(usersId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("找不到會員")
                        );

        if (!"ADMIN".equals(usersBean.getRole())) {
            throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "沒有管理員權限"
            );
        }

        return usersBean;
    }

    // 管理員修改會員狀態
    public UsersBean updateAdminStatus(
            Integer usersId,
            String status) {

        UsersBean usersBean =
                usersRepository.findById(usersId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("找不到會員")
                        );

        // 管理員只能設定 ACTIVE 或 BANNED
        if (!"ACTIVE".equals(status)
                && !"BANNED".equals(status)) {

            throw new IllegalArgumentException(
                    "無效的會員狀態"
            );
        }

        usersBean.setStatus(status);
        usersBean.setUpdatedAt(LocalDateTime.now());

        return usersRepository.save(usersBean);
    }

    // 刪除會員
    public void deleteById(Integer usersId) {
        usersRepository.deleteById(usersId);
    }
}

