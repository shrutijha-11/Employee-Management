package com.shruti.EmpManage.Dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeFilterRequest(
        String name,
        String department,
        BigDecimal minSalary,
        BigDecimal maxSalary,
        LocalDate joinedFrom,
        LocalDate joinedTo
) {
}
