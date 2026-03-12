package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.request.WorkshopRequest;
import com.example.demo.dto.response.WorkshopResponse;
import com.example.demo.service.WorkshopService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workshops")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WorkshopController {
    WorkshopService workshopService;

    @PostMapping("/admin/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WorkshopResponse> createWorkshop(@RequestBody WorkshopRequest request) {
        return ApiResponse.<WorkshopResponse>builder()
                .code(1000)
                .result(workshopService.createWorkshop(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<WorkshopResponse>> getAllWorkshops() {
        return ApiResponse.<List<WorkshopResponse>>builder()
                .code(1000)
                .result(workshopService.getAllWorkshops())
                .build();
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WorkshopResponse> updateWorkshop(
            @PathVariable Long id,
            @RequestBody WorkshopRequest request) {
        return ApiResponse.<WorkshopResponse>builder()
                .code(1000)
                .result(workshopService.updateWorkshop(id, request))
                .build();
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> deleteWorkshop(@PathVariable Long id) {
        workshopService.deleteWorkshop(id);
        return ApiResponse.<String>builder()
                .code(1000)
                .result("Workshop deleted successfully")
                .build();
    }

    @GetMapping("/upcoming")
    public ApiResponse<List<WorkshopResponse>> getUpcomingWorkshops() {
        return ApiResponse.<List<WorkshopResponse>>builder()
                .code(1000)
                .result(workshopService.getUpcomingWorkshops())
                .build();
    }

    @PostMapping("/{id}/book")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<WorkshopResponse> bookWorkshop(@PathVariable Long id) {
        return ApiResponse.<WorkshopResponse>builder()
                .code(1000)
                .result(workshopService.bookWorkshop(id))
                .build();
    }

    @DeleteMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<String> cancelBooking(@PathVariable Long id) {
        workshopService.cancelBooking(id);
        return ApiResponse.<String>builder()
                .code(1000)
                .result("Booking cancelled successfully")
                .build();
    }
}
