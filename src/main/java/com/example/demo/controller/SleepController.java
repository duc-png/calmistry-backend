package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.request.SubmitSleepQuizRequest;
import com.example.demo.dto.response.SleepHistoryResponse;
import com.example.demo.dto.response.SleepSessionResponse;
import com.example.demo.service.SleepService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sleep")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SleepController {

    SleepService sleepService;

    @PostMapping("/submit")
    public ApiResponse<SleepSessionResponse> submitSleepQuiz(@RequestBody @Valid SubmitSleepQuizRequest request) {
        var result = sleepService.submitSleepQuiz(request);
        return ApiResponse.<SleepSessionResponse>builder()
                .result(result)
                .build();
    }

    @GetMapping("/history")
    public ApiResponse<SleepHistoryResponse> getSleepHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        var result = sleepService.getSleepHistory(page, size);
        return ApiResponse.<SleepHistoryResponse>builder()
                .result(result)
                .build();
    }

    @GetMapping("/latest")
    public ApiResponse<SleepSessionResponse> getLatestSleepSession() {
        var result = sleepService.getLatestSleepSession();
        return ApiResponse.<SleepSessionResponse>builder()
                .result(result)
                .build();
    }
}
