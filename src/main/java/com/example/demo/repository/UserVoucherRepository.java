package com.example.demo.repository;

import com.example.demo.entity.UserVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, Long> {
    List<UserVoucher> findAllByUser_Id(Long userId);
    Optional<UserVoucher> findByCode(String code);
}
