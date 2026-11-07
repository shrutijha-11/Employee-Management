package com.shruti.EmpManage.Dto;

import com.shruti.EmpManage.Entity.EmployeeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeResponse(
        Long id,
        String fullName,
        String email,
        String department,
        BigDecimal salary,
        LocalDate joiningDate,
        EmployeeStatus status,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
