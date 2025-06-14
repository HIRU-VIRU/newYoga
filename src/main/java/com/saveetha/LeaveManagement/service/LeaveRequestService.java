package com.saveetha.LeaveManagement.service;

import com.saveetha.LeaveManagement.dto.LeaveHistoryDto;
import com.saveetha.LeaveManagement.dto.LeaveRequestDTO;
import com.saveetha.LeaveManagement.dto.LeaveRequestResponseDTO;
import com.saveetha.LeaveManagement.dto.LeaveSearchFilterDTO;
import com.saveetha.LeaveManagement.entity.*;
import com.saveetha.LeaveManagement.enums.AlterationType;
import com.saveetha.LeaveManagement.enums.LeaveStatus;
import com.saveetha.LeaveManagement.enums.NotificationStatus;
import com.saveetha.LeaveManagement.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveAlterationRepository leaveAlterationRepository;
    private final LeaveApprovalService leaveApprovalService;
    private final LeaveValidationService leaveValidationService;
    private final LeaveApprovalRepository leaveApprovalRepository;

    public LeaveRequest createDraftLeaveRequest(LeaveRequestDTO leaveRequestdto) {
        Employee employee = employeeRepository.findByEmpId(leaveRequestdto.getEmpId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        LeaveType leaveType = leaveTypeRepository.findById(leaveRequestdto.getLeaveTypeId())
                .orElseThrow(() -> new RuntimeException("LeaveType not found"));
        // -----CALL APPROPRIATE VALIDATION BASED ON LEAVE TYPE ------------
        String leaveName = leaveType.getTypeName().toLowerCase();
        System.out.println("Leave Type Name: " + leaveName);
        switch (leaveName) {
            case "cl":
                leaveValidationService.validateCasualLeave(leaveRequestdto);
                break;
            case "permission":
                leaveValidationService.validatePermissionLeave(leaveRequestdto);
                break;
            case "ml":
                leaveValidationService.validateMedicalLeave(leaveRequestdto);
                break;
            case "el":
                leaveValidationService.validateEarnedLeave(leaveRequestdto);
                break;
            case "comp off":
                leaveValidationService.validateCompOff(leaveRequestdto);
                break;
            case "lop":
                leaveValidationService.validatelop(leaveRequestdto);
                break;
            case "vacation":
                leaveValidationService.validatevacation(leaveRequestdto);
                break;
            case "late":
                leaveValidationService.validatelate(leaveRequestdto);
                break;
            default:
                throw new RuntimeException("Unknown Leave Type: " + leaveName);
        }

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setStartDate(leaveRequestdto.getStartDate());
        leaveRequest.setEndDate(leaveRequestdto.getEndDate());
        leaveRequest.setStartTime(leaveRequestdto.getStartTime());
        leaveRequest.setEndTime(leaveRequestdto.getEndTime());
        leaveRequest.setReason(leaveRequestdto.getReason());
        leaveRequest.setEarnedDate(leaveRequestdto.getEarnedDate());
        leaveRequest.setFileUpload(leaveRequestdto.getFileUpload());

        // Set the half-day flag to true or false based on the dto
        leaveRequest.setHalfDay(leaveRequestdto.isHalfDay());

        if (leaveRequestdto.isHalfDay()) {
            if (leaveRequestdto.getSession() == null ||
                    (!leaveRequestdto.getSession().equalsIgnoreCase("FN") &&
                            !leaveRequestdto.getSession().equalsIgnoreCase("AN"))) {
                throw new RuntimeException("Session must be 'FN' or 'AN' for half-day leave.");
            }
            leaveRequest.setSession(leaveRequestdto.getSession().toUpperCase());
        } else {
            leaveRequest.setSession(null); // Not applicable
        } // This will pass a boolean value (true or false)

        double numberOfDays =calculateLeaveDays(leaveRequestdto.getStartDate(), leaveRequestdto.getEndDate(), leaveRequestdto.isHalfDay());
        leaveRequest.setNumberOfDays(numberOfDays);

        if (leaveRequestdto.getHasClass() != null && leaveRequestdto.getHasClass()) {
            leaveRequest.setStatus(LeaveStatus.DRAFT);
            return leaveRequestRepository.save(leaveRequest);
        } else {
            leaveRequest.setStatus(LeaveStatus.PENDING);
            LeaveRequest savedLeaveRequest = leaveRequestRepository.save(leaveRequest);
            leaveApprovalService.initiateApprovalFlow(savedLeaveRequest.getRequestId());
            return savedLeaveRequest;
        }

    }
    private double calculateLeaveDays(LocalDate startDate, LocalDate endDate, boolean isHalfDay) {
        if (isHalfDay && !startDate.equals(endDate)) {
            throw new RuntimeException("Half-day leave can only be applied for a single day.");
        }

        if (startDate.equals(endDate)) {
             // If it's a single day leave, return 0.5 for half-day and 1.0 for full-day.
            return isHalfDay ? 0.5 : 1.0;
        }

        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return isHalfDay ? totalDays * 0.5 : totalDays;  // For half-day, return 0.5 of the total days.
    }


    public List<Integer> submitLeaveRequest(Integer requestId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Leave Request not found"));

        if (!leaveRequest.getStatus().equals(LeaveStatus.DRAFT)) {
            throw new RuntimeException("Only DRAFT leave requests can be submitted");
        }

        // Get alterations linked to this leave request
        List<LeaveAlteration> alterations = leaveAlterationRepository.findByLeaveRequest_RequestId(requestId);

        boolean hasAlteration = !alterations.isEmpty();

        if (hasAlteration) {
            for (LeaveAlteration alt : alterations) {
                if (alt.getAlterationType() == AlterationType.STAFF_ALTERATION) {
                    if (alt.getNotificationStatus() != NotificationStatus.APPROVED) {
                        throw new RuntimeException("All staff alterations must be approved before submission.");
                    }
                }
            }
        }

        // If no alteration or all alterations are valid, proceed
        leaveRequest.setStatus(LeaveStatus.PENDING);
        leaveRequestRepository.save(leaveRequest);
        List<LeaveApproval> savedApprovals= leaveApprovalService.initiateApprovalFlow(requestId);
        return  savedApprovals.stream()
                .map(LeaveApproval::getApprovalId)
                .collect(Collectors.toList());
    }


    @Transactional
    public void withdrawLeaveRequestByEmployee(Integer leaveRequestId, String empId) throws IllegalAccessException {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        if (!leaveRequest.getEmployee().getEmpId().equals(empId)) {
            throw new IllegalAccessException("You are not authorized to withdraw this leave request");
        }

        leaveRequest.setStatus(LeaveStatus.WITHDRAWN);
        leaveApprovalRepository.deleteByLeaveRequest(leaveRequest);
        leaveRequestRepository.save(leaveRequest);
    }

    private String getCurrentUserEmpId(){
        Object principal= SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }else{
            throw new RuntimeException("unable to extract user details from security context");
        }
    }
    public List<LeaveHistoryDto> getLeaveHistoryForCurrentUser(){
        String empId =getCurrentUserEmpId();
        List<Object[]> rawData =leaveRequestRepository.getLeaveHistoryForEmployee(empId);

        return rawData.stream().map(row-> new LeaveHistoryDto(
                (Integer) row[0],
                (String) row[1],
                ((java.sql.Date) row[2]).toLocalDate(),
                ((java.sql.Date) row[3]).toLocalDate(),
                (String) row[4],
                (String) row[5],
                ((java.sql.Timestamp) row [6]).toLocalDateTime()
        )).toList();
    }
    public List<LeaveRequestResponseDTO> getAllLeaveRequests() {
        List<LeaveRequest> leaveRequests = leaveRequestRepository.findAll();
        return leaveRequests.stream()
                .map(LeaveRequestResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public LeaveRequest getLeaveRequestById(Integer id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave Request not found with ID: " + id));
    }

    public void deleteLeaveRequest(Integer id) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave Request not found with ID: " + id));
        leaveRequestRepository.delete(leaveRequest);
    }
    public List<LeaveHistoryDto> searchLeaveRequests(LeaveSearchFilterDTO filter) {
        List<Object[]> result = leaveRequestRepository.searchLeaveRequests(
                filter.getEmpId(),
                filter.getEmpName(),
                filter.getLeaveType(),
                filter.getLeaveDate(),
                filter.getStatus(),
                filter.getRequestId()
        );

        return result.stream()
                .map(row -> new LeaveHistoryDto(
                        (Integer) row[0],
                        (String) row[1],
                        ((java.sql.Date) row[2]).toLocalDate(),
                        ((java.sql.Date) row[3]).toLocalDate(),
                        (String) row[4],
                        (String) row[5],
                        ((java.sql.Timestamp) row[6]).toLocalDateTime()
                ))
                .collect(Collectors.toList());
    }

}
