package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.request.CreateJournalRequest;
import com.example.demo.dto.request.UpdateJournalRequest;
import com.example.demo.dto.response.JournalResponse;
import com.example.demo.dto.response.JournalStatsResponse;
import com.example.demo.service.JournalService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/journals")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JournalController {

    JournalService journalService;

    @GetMapping
    public ApiResponse<List<JournalResponse>> getJournals(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String mood) {

        log.info("📖 GET /journals - search: {}, mood: {}", search, mood);

        var result = journalService.getJournals(search, mood);
        return ApiResponse.<List<JournalResponse>>builder()
                .result(result)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<JournalResponse> getJournalById(@PathVariable Long id) {
        var result = journalService.getJournalById(id);
        return ApiResponse.<JournalResponse>builder()
                .result(result)
                .build();
    }

    @GetMapping("/v1/stats")
    public ApiResponse<JournalStatsResponse> getJournalStats() {
        var result = journalService.getJournalStats();
        return ApiResponse.<JournalStatsResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping
    public ApiResponse<JournalResponse> createJournal(@RequestBody @Valid CreateJournalRequest request) {
        var result = journalService.createJournal(request);
        return ApiResponse.<JournalResponse>builder()
                .result(result)
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<JournalResponse> updateJournal(
            @PathVariable Long id,
            @RequestBody @Valid UpdateJournalRequest request) {
        var result = journalService.updateJournal(id, request);
        return ApiResponse.<JournalResponse>builder()
                .result(result)
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteJournal(@PathVariable Long id) {
        journalService.deleteJournal(id);
        return ApiResponse.<Void>builder()
                .message("Journal deleted successfully")
                .build();
    }
}
