package com.shruti.EmpManage.Dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record JoiningMonthReportResponse(
        @Schema(example = "2026") Integer year,
        @Schema(example = "3") Integer month,
        @Schema(description = "How many employees joined in that month") Long count,
        List<EmployeeResponse> employees
) {
}
