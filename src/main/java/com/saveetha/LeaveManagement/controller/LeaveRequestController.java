package com.saveetha.LeaveManagement.controller;

import com.saveetha.LeaveManagement.dto.LeaveHistoryDto;
import com.saveetha.LeaveManagement.dto.LeaveRequestDTO;
import com.saveetha.LeaveManagement.dto.LeaveRequestResponseDTO;
import com.saveetha.LeaveManagement.entity.LeaveRequest;
import com.saveetha.LeaveManagement.security.JwtUtil;
import com.saveetha.LeaveManagement.service.CloudinaryService;
import com.saveetha.LeaveManagement.service.LeaveRequestService;
import com.saveetha.LeaveManagement.dto.LeaveSearchFilterDTO;

import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leave-request")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;
    @Autowired
    private CloudinaryService cloudinaryService;
    @Autowired
    private JwtUtil jwtUtil;
    @PostMapping("/create-draft")
    public ResponseEntity<?> createDraft(@RequestBody LeaveRequestDTO leaveRequestdto) {
        LeaveRequest saved = leaveRequestService.createDraftLeaveRequest(leaveRequestdto);
        return ResponseEntity.ok("Draft Leave Request created with ID: " + saved.getRequestId());
    }
    @PostMapping("/submit/{id}")
    public ResponseEntity<?> submitLeaveRequest(@PathVariable("id") Integer requestId) {
        List<Integer> approvalIds = leaveRequestService.submitLeaveRequest(requestId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Leave request submitted successfully.");
        response.put("approvalIds", approvalIds);

        return ResponseEntity.ok(response);
    }
    // PATCH endpoint to withdraw a leave request
    @PostMapping("/withdraw/{leaveRequestId}")
    public ResponseEntity<String> withdrawLeaveRequest(
            @PathVariable Integer leaveRequestId,
            @RequestHeader("Authorization") String token) {
        try {
            String empId = jwtUtil.extractEmpId(token.substring(7)); // Remove "Bearer "
            leaveRequestService.withdrawLeaveRequestByEmployee(leaveRequestId, empId);
            return ResponseEntity.ok("Leave request withdrawn successfully");
        } catch (IllegalAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    @PostMapping("/upload-file")
    public ResponseEntity<String> uploadMedicalCertificate(@RequestParam("file") MultipartFile file) {
        try {
            String fileUrl = cloudinaryService.uploadDocument(file); // Uploads to Cloudinary
            return ResponseEntity.ok(fileUrl); // Return just the URL to frontend
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File upload failed.");
        }
    }
    @GetMapping("/leave-history")
    public ResponseEntity<List<LeaveHistoryDto>> getLeaveHistory() {
        return ResponseEntity.ok(leaveRequestService.getLeaveHistoryForCurrentUser());
    }


    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<LeaveRequestResponseDTO>> getAllLeaveRequests() {
        return ResponseEntity.ok(leaveRequestService.getAllLeaveRequests());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/search")
    public ResponseEntity<List<LeaveHistoryDto>> searchLeaveHistory(@RequestBody LeaveSearchFilterDTO filter) {
        List<LeaveHistoryDto> results = leaveRequestService.searchLeaveRequests(filter);
        return ResponseEntity.ok(results);
    }



}
