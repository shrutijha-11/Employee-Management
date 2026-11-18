package com.shruti.EmpManage.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BulkStatusRequest(
        @NotEmpty(message = "ids must contain at least one employee id")
        @Schema(description = "Employee ids to update. Must not repeat an id.", example = "[1, 2, 3]")
        List<@NotNull(message = "ids must not contain null") Long> ids,

        @NotBlank(message = "Status is required")
        @Schema(description = "New status for every listed employee",
                allowableValues = {"ACTIVE", "INACTIVE"}, example = "INACTIVE")
        String status
) {
}
