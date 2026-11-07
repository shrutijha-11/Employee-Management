package com.shruti.EmpManage.Dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

public record StatusUpdateRequest(

        @NotBlank(message = "Status is required")
        @Schema(description = "New status", allowableValues = {"ACTIVE", "INACTIVE"}, example = "INACTIVE")
        String status
) {
}
