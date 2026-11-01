package com.shruti.EmpManage.Dto;

public record DepartmentSummaryResponse(
        String department,
        Long employeeCount,
        Double averageSalary
) {
}
