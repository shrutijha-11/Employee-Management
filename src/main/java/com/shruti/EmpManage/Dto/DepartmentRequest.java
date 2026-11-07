package com.shruti.EmpManage.Dto;

import jakarta.validation.constraints.NotBlank;

public record DepartmentRequest(

        @NotBlank(message = "Department name is required")
        String name,

        @NotBlank(message = "Location is required")
        String location
) {
}