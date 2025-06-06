package com.saveetha.LeaveManagement.controller;

import com.saveetha.LeaveManagement.dto.AssignApprovalFlowDTO;
import com.saveetha.LeaveManagement.dto.CreateEmployeeRequestDto;
import com.saveetha.LeaveManagement.dto.EmployeeUpdateDTO;
import com.saveetha.LeaveManagement.entity.Employee;
import com.saveetha.LeaveManagement.repository.EmployeeLeaveBalanceRepository;
import com.saveetha.LeaveManagement.repository.EmployeeRepository;
import com.saveetha.LeaveManagement.service.CloudinaryService;
import com.saveetha.LeaveManagement.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private EmployeeLeaveBalanceRepository employeeLeaveBalanceRepository;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    //  Update Employee API
    @PutMapping("/update/{empId}")
    @PreAuthorize("hasAnyAuthority('EMPLOYEE', 'ADMIN')")  // Restrict access
    public ResponseEntity<String> updateEmployee(
            @PathVariable String empId,
            @RequestBody EmployeeUpdateDTO employeeUpdateDTO) {

        boolean isUpdated = employeeService.updateEmployee(empId, employeeUpdateDTO);
        if (isUpdated) {
            System.out.println("Joining Date: " + employeeUpdateDTO.getJoiningDate());
            return ResponseEntity.ok("Employee updated successfully!");
        } else {
            return ResponseEntity.badRequest().body("Failed to update employee.");
        }
    }
    @PostMapping("/upload-picture/{empId}")
    public ResponseEntity<String> uploadProfilePic(
            @PathVariable String empId,
            @RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = cloudinaryService.uploadImage(file);
            employeeService.updateProfilePic(empId, imageUrl);
            return ResponseEntity.ok(imageUrl);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Image upload failed.");
        }
    }
    // Get all employees
    @GetMapping
    public ResponseEntity<List<Employee>> getAllEmployees() {
        List<Employee> employees = employeeService.getAllEmployees();
        return ResponseEntity.ok(employees);
    }

    // Get employee by ID
    @GetMapping("/{empId}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable String empId) {
        return employeeService.getEmployeeById(empId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    // Soft delete - deactivate employee
    @PatchMapping("/deactivate/{empId}")
    public ResponseEntity<String> deactivateEmployee(@PathVariable String empId) {
        boolean result = employeeService.deactivateEmployee(empId);
        return result ? ResponseEntity.ok("Employee deactivated successfully")
                : ResponseEntity.notFound().build();
    }

    // Reactivate employee
    @PatchMapping("/activate/{empId}")
    public ResponseEntity<String> activateEmployee(@PathVariable String empId) {
        boolean result = employeeService.activateEmployee(empId);
        return result ? ResponseEntity.ok("Employee activated successfully")
                : ResponseEntity.notFound().build();
    }
    @DeleteMapping("/{empId}")
    @Transactional
    public ResponseEntity<String> deleteEmployee(@PathVariable String empId) {
        try {
            // First delete leave balances to satisfy FK constraints
            employeeLeaveBalanceRepository.deleteByEmpId(empId);

            // Then delete the employee
            employeeRepository.deleteById(empId);

            return ResponseEntity.ok("Employee deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete employee: " + e.getMessage());
        }
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> createEmployee(@RequestBody CreateEmployeeRequestDto dto) {
        Employee employee = employeeService.createEmployee(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(employee);
    }
    @PutMapping("/assign-approval-flow")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> assignApprovalFlowToEmployee(@RequestBody AssignApprovalFlowDTO dto) {
        boolean updated = employeeService.updateEmployeeWithApprovalFlow(dto);
        return updated
                ? ResponseEntity.ok("Employee details updated and approval flow assigned successfully.")
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee or Approval Flow not found.");
    }
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<List<Employee>> searchEmployees(@RequestParam String keyword) {
        return ResponseEntity.ok(employeeService.searchEmployees(keyword));
    }

}
