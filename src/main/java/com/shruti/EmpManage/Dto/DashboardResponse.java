package com.shruti.EmpManage.Dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record DashboardResponse(
        Long totalEmployees,
        Long activeEmployees,
        Long inactiveEmployees,
        @Schema(description = "Average salary across all employees; 0 when there are none")
        Double averageSalary,
        @Schema(description = "Department with the most employees; null when there are no employees")
        TopDepartmentResponse topDepartment
) {
}
