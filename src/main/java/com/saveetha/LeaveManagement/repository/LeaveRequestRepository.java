package com.saveetha.LeaveManagement.repository;

import com.saveetha.LeaveManagement.entity.LeaveRequest;
import com.saveetha.LeaveManagement.entity.Employee;
import com.saveetha.LeaveManagement.entity.LeaveType;
import com.saveetha.LeaveManagement.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Integer> {



    // Get all leave requests by employee
    List<LeaveRequest> findByEmployeeEmpId(String empId);

    // Optional: Get all active requests
    List<LeaveRequest> findByActiveTrue();


    @Query("SELECT COALESCE(SUM(DATEDIFF(lr.endDate, lr.startDate) + 1), 0) FROM LeaveRequest lr " +
            "WHERE lr.employee.empId = :empId " +
            "AND LOWER(lr.leaveType.typeName) = LOWER(:leaveTypeName) " +
            "AND lr.status IN ('PENDING', 'APPROVED')")
    int sumTotalDaysOfPendingAndApprovedML(
            @Param("empId") String empId,
            @Param("leaveTypeName") String leaveTypeName
    );

    @Query("SELECT lr FROM LeaveRequest lr " +
            "WHERE lr.employee.empId = :empId " +
            "AND LOWER(lr.leaveType.typeName) = LOWER(:leaveType) " +
            "AND lr.status IN :statuses " +
            "AND lr.startDate <= :endDate " +
            "AND lr.endDate >= :startDate")
    List<LeaveRequest> findOverlappingLeaveRequests(
            @Param("empId") String empId,
            @Param("leaveType") String leaveType,
            @Param("statuses") List<LeaveStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT lr FROM LeaveRequest lr WHERE " +
            "lr.employee.empId = :empId AND " +
            "lr.leaveType.leaveTypeId = :leaveTypeId AND " +
            "lr.status IN :statuses AND " +
            "((lr.startDate BETWEEN :startDate AND :endDate) OR " +
            "(lr.endDate BETWEEN :startDate AND :endDate))")
    List<LeaveRequest> findOverlappingLeaveRequestsByTypeId(
            @Param("empId") String empId,
            @Param("leaveTypeId") Integer leaveTypeId,
            @Param("statuses") List<LeaveStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT lr FROM LeaveRequest lr WHERE " +
            "lr.employee.empId = :empId AND " +
            "lr.leaveType.leaveTypeId = :leaveTypeId AND " +
            "lr.status IN :statuses AND " +
            "lr.earnedDate = :earnedDate")
    List<LeaveRequest> findCompOffRequestsByEarnedDate(
            @Param("empId") String empId,
            @Param("leaveTypeId") Integer leaveTypeId,
            @Param("statuses") List<LeaveStatus> statuses,
            @Param("earnedDate") LocalDate earnedDate);

    @Query("SELECT COALESCE(SUM(lr.numberOfDays), 0) FROM LeaveRequest lr " +
            "WHERE lr.employee.empId = :empId " +
            "AND lr.leaveType.leaveTypeId = :leaveTypeId " +
            "AND lr.status IN :statuses " +
            "AND lr.startDate >= :academicStart " +
            "AND lr.startDate <= :currentDate")
    double countTotalCLsUsed(
            @Param("empId") String empId,
            @Param("leaveTypeId") Integer leaveTypeId,
            @Param("statuses") List<LeaveStatus> statuses,
            @Param("academicStart") LocalDate academicStart,
            @Param("currentDate") LocalDate currentDate
    );
    @Query("SELECT COUNT(lr) FROM LeaveRequest lr " +
            "WHERE lr.employee.empId = :empId " +
            "AND lr.leaveType.leaveTypeId = :leaveTypeId " +
            "AND lr.status IN :statuses " +
            "AND lr.startDate BETWEEN :startDate AND :endDate")
    int countPermissionLeavesInMonth(
            @Param("empId") String empId,
            @Param("leaveTypeId") Integer leaveTypeId,
            @Param("statuses") List<LeaveStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT CASE WHEN COUNT(lr) > 0 THEN true ELSE false END FROM LeaveRequest lr " +
            "WHERE lr.employee.empId = :empId " +
            "AND lr.leaveType.leaveTypeId = :leaveTypeId " +
            "AND lr.status IN :statuses " +
            "AND lr.startDate = :date " +
            "AND ((lr.startTime <= :endTime AND lr.endTime >= :startTime))")
    boolean existsOverlappingPermission(
            @Param("empId") String empId,
            @Param("leaveTypeId") Integer leaveTypeId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statuses") List<LeaveStatus> statuses
    );
    @Query("""
      SELECT CASE WHEN COUNT(lr) > 0 THEN TRUE ELSE FALSE END
      FROM LeaveRequest lr
      WHERE lr.employee.empId = :empId
        AND lr.startDate = :date
        AND lr.status IN ('PENDING','APPROVED')
    """)
    boolean existsByEmployeeAndDate(
            @Param("empId") String empId,
            @Param("date")  LocalDate date
    );

    /**
     * Count how many PERMISSION leaves (PENDING or APPROVED) the employee has taken
     * between two dates (inclusive).
     */
    @Query("""
      SELECT COUNT(lr)
      FROM LeaveRequest lr
      WHERE lr.employee.empId = :empId
        AND lr.leaveType.typeName = 'PERMISSION'
        AND lr.status IN ('PENDING','APPROVED')
        AND lr.startDate BETWEEN :startDate AND :endDate
    """)
    int countPermissionLeavesInRange(
            @Param("empId")     String    empId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate
    );
    /**
     * Count how many PERMISSION leaves (PENDING or APPROVED) the employee has taken
     * between two dates (inclusive).
     */
    @Query("""
      SELECT COUNT(lr)
      FROM LeaveRequest lr
      WHERE lr.employee.empId = :empId
        AND lr.leaveType.typeName = 'LATE'
        AND lr.status IN ('PENDING','APPROVED')
        AND lr.startDate BETWEEN :startDate AND :endDate
    """)
    int countlateInRange(
            @Param("empId")     String    empId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate
    );
    @Query(value = """
SELECT
    lr.request_id,
    lt.type_name AS leave_type,
    lr.start_date,
    lr.end_date,
    lr.status,
    lr.reason,
    lr.created_at
FROM
    leave_request lr
JOIN
    leave_type lt ON lr.leave_type_id = lt.leave_type_id
WHERE
    lr.emp_id = ? AND lr.active = true
ORDER BY
    lr.created_at DESC

          """,nativeQuery=true)
    List<Object[]>getLeaveHistoryForEmployee(@Param("empId") String empId);



    @Query(value = """
    SELECT lr.request_id, lt.type_name, lr.start_date, lr.end_date, lr.status, lr.reason, lr.created_at
    FROM leave_request lr
    JOIN leave_type lt ON lr.leave_type_id = lt.leave_type_id
    JOIN employee e ON lr.emp_id = e.emp_id
    WHERE (:empId IS NULL OR lr.emp_id = :empId)
      AND (:empName IS NULL OR LOWER(e.emp_name) LIKE LOWER(CONCAT('%', :empName, '%')))
      AND (:leaveType IS NULL OR LOWER(lt.type_name) LIKE LOWER(CONCAT('%', :leaveType, '%')))
      AND (:leaveDate IS NULL OR (:leaveDate BETWEEN lr.start_date AND lr.end_date))
      AND (:status IS NULL OR LOWER(lr.status) = LOWER(:status))
      AND (:requestId IS NULL OR lr.request_id = :requestId)
      AND lr.active = true
    ORDER BY lr.created_at DESC
""", nativeQuery = true)
    List<Object[]> searchLeaveRequests(
            @Param("empId") String empId,
            @Param("empName") String empName,
            @Param("leaveType") String leaveType,
            @Param("leaveDate") LocalDate leaveDate,
            @Param("status") String status,
            @Param("requestId") Integer requestId
    );


}

