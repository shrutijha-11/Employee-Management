package com.shruti.EmpManage.Dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TopDepartmentResponse(
        @Schema(example = "Engineering") String name,
        @Schema(example = "12") Long employeeCount
) {
}
