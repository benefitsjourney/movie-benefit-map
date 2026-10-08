package com.collection.benefitsjourney.service;

import com.collection.benefitsjourney.domain.UsersBean;
import com.collection.benefitsjourney.repository.UsersRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsersService {

    private final UsersRepository usersRepository;

    // 建構子注入 UsersRepository
    public UsersService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
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

    // 刪除會員
    public void deleteById(Integer usersId) {
        usersRepository.deleteById(usersId);
    }
}

