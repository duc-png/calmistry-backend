package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.request.SubmitFuiedsRequest;
import com.example.demo.dto.response.FuiedsScoreResponse;
import com.example.demo.service.FuiedsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.List;

@RestController
@RequestMapping("/fuieds")
@RequiredArgsConstructor
@Slf4j
public class FuiedsController {

        private final FuiedsService fuiedsService;

        @PostConstruct
        public void init() {
                log.info("🎯 FuiedsController initialized - Endpoints ready:");
                log.info("   POST /fuieds/submit");
                log.info("   GET  /fuieds/today");
                log.info("   GET  /fuieds/history");
        }

        /**
         * Submit daily FUIEDS quiz response
         */
        @PostMapping("/submit")
        public ApiResponse<FuiedsScoreResponse> submitResponse(
                        @RequestBody SubmitFuiedsRequest request) {
                log.info("📊 Submitting FUIEDS response: {}", request);
                try {
                        FuiedsScoreResponse response = fuiedsService.submitResponse(request);
                        return ApiResponse.<FuiedsScoreResponse>builder()
                                        .code(1000)
                                        .result(response)
                                        .build();
                } catch (Exception e) {
                        log.error("❌ FUIEDS Controller Error: {} - {}", e.getClass().getSimpleName(), e.getMessage());
                        throw e;
                }
        }

        /**
         * Get today's FUIEDS score
         */
        @GetMapping("/today")
        public ResponseEntity<ApiResponse<FuiedsScoreResponse>> getTodayScore() {
                log.info("📊 Getting today's FUIEDS score");
                FuiedsScoreResponse response = fuiedsService.getTodayScore();

                if (response == null) {
                        return ResponseEntity.ok(
                                        ApiResponse.<FuiedsScoreResponse>builder()
                                                        .code(404)
                                                        .message("Chưa có đánh giá hôm nay")
                                                        .build());
                }

                return ResponseEntity.ok(
                                ApiResponse.<FuiedsScoreResponse>builder()
                                                .code(1000)
                                                .result(response)
                                                .build());
        }

        /**
         * Get FUIEDS score history
         * 
         * @param days Number of days to retrieve (default 7)
         */
        @GetMapping("/history")
        public ResponseEntity<ApiResponse<List<FuiedsScoreResponse>>> getHistory(
                        @RequestParam(defaultValue = "7") int days) {
                log.info("📊 Getting FUIEDS history for last {} days", days);
                List<FuiedsScoreResponse> history = fuiedsService.getHistory(days);
                return ResponseEntity.ok(
                                ApiResponse.<List<FuiedsScoreResponse>>builder()
                                                .code(1000)
                                                .result(history)
                                                .build());
        }
}
