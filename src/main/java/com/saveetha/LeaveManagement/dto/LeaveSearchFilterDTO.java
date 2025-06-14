package com.saveetha.LeaveManagement.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LeaveSearchFilterDTO {
    private String empId;
    private String empName;
    private String leaveType;
    private LocalDate leaveDate; // used to filter by start or end date
    private String status;
    private String approverName;
    private Integer requestId;
}
