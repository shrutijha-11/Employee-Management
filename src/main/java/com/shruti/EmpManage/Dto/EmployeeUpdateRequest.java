package com.shruti.EmpManage.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeUpdateRequest(

        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Department is required")
        String department,

        @NotNull(message = "Salary is required")
        @DecimalMin(value = "0.0", message = "Salary cannot be negative")
        BigDecimal salary,

        @NotNull(message = "Joining date is required")
        LocalDate joiningDate,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "[0-9]{10}", message = "Phone number must be exactly 10 digits")
        String phoneNumber,
        @Schema(description = "Exactly 10 digits", example = "9876543210")

        @NotBlank(message = "Address is required")
        @Size(max = 255, message = "Address must be at most 255 characters")
        String address,

        @Schema(description = "Id of this employee's manager. Leave empty for no manager.", example = "3")
        Long managerId,

        @NotNull(message = "Version is required")
        @Schema(description = "Current version of the employee, taken from the latest GET", example = "0")
        Long version

) {
}
