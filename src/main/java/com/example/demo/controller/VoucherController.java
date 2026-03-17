package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.User;
import com.example.demo.entity.UserVoucher;
import com.example.demo.entity.Voucher;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserVoucherRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/vouchers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VoucherController {
    UserVoucherRepository userVoucherRepository;
    UserRepository userRepository;

    @GetMapping("/validate")
    public ApiResponse<Map<String, Object>> validateVoucher(@RequestParam String code) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        UserVoucher uv = userVoucherRepository.findByCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Voucher không tồn tại"));

        if (!uv.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Voucher không thuộc về bạn");
        }

        if (uv.getStatus() != UserVoucher.VoucherStatus.UNUSED) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Voucher đã được sử dụng");
        }

        Voucher v = uv.getVoucher();
        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);
        result.put("discountValue", v.getDiscountValue());
        result.put("discountType", v.getDiscountType().name());
        result.put("title", v.getTitle());

        return ApiResponse.<Map<String, Object>>builder()
                .result(result)
                .build();
    }

    @GetMapping("/my-vouchers")
    public ApiResponse<java.util.List<Map<String, Object>>> getMyVouchers() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        java.util.List<UserVoucher> userVouchers = userVoucherRepository.findAllByUser_Id(user.getId());
        
        java.util.List<Map<String, Object>> result = userVouchers.stream().map(uv -> {
            Map<String, Object> map = new HashMap<>();
            map.put("code", uv.getCode());
            map.put("status", uv.getStatus().name());
            map.put("title", uv.getVoucher().getTitle());
            map.put("discountValue", uv.getVoucher().getDiscountValue());
            map.put("discountType", uv.getVoucher().getDiscountType().name());
            map.put("expiryDate", uv.getExpiryDate());
            return map;
        }).collect(java.util.stream.Collectors.toList());

        return ApiResponse.<java.util.List<Map<String, Object>>>builder()
                .result(result)
                .build();
    }
}
