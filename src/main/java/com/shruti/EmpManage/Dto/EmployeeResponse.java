package com.shruti.EmpManage.Dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeResponse( Long id,
                                String fullName,
                                String email,
                                String department,
                                BigDecimal salary,
                                LocalDate joiningDate,
                                LocalDateTime createdAt,
                                LocalDateTime updatedAt
) {
}
