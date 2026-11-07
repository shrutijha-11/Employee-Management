package com.shruti.EmpManage.Dto;

import java.time.LocalDateTime;

public record EmployeeHistoryResponse(
        Long id,
        Long employeeId,
        String fieldName,
        String oldValue,
        String newValue,
        LocalDateTime changedAt
) {
}
