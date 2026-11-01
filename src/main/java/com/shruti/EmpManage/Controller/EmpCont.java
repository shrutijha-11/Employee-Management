package com.shruti.EmpManage.Controller;

import com.shruti.EmpManage.Dto.DepartmentSummaryResponse;
import com.shruti.EmpManage.Dto.EmployeeRequest;
import com.shruti.EmpManage.Dto.EmployeeResponse;
import com.shruti.EmpManage.Dto.PageResponse;
import com.shruti.EmpManage.Service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employees", description = "Employee management operations")
public class EmpCont {

    private final EmployeeService employeeService;

    public EmpCont(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @Operation(summary = "Create an employee")
    @ApiResponse(responseCode = "201", description = "Employee created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "409", description = "Email already exists")
    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.createEmployee(request));
    }

    @Operation(summary = "List employees with pagination and sorting",
            description = "sortBy accepts fullName, email, department, salary or joiningDate. "
                    + "direction accepts asc or desc. Page numbers start at 0.")
    @ApiResponse(responseCode = "200", description = "Page of employees")
    @ApiResponse(responseCode = "400", description = "Invalid page, size, sortBy or direction")
    @GetMapping
    public ResponseEntity<PageResponse<EmployeeResponse>> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return ResponseEntity.ok(employeeService.getAllEmployees(page, size, sortBy, direction));
    }

    @Operation(summary = "Get an employee by ID")
    @ApiResponse(responseCode = "200", description = "Employee found")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @Operation(summary = "Update an employee")
    @ApiResponse(responseCode = "200", description = "Employee updated")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    @ApiResponse(responseCode = "409", description = "Email belongs to another employee")
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(@PathVariable Long id,
                                                           @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @Operation(summary = "Delete an employee")
    @ApiResponse(responseCode = "204", description = "Employee deleted")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List employees in a department", description = "Case-insensitive match")
    @ApiResponse(responseCode = "200", description = "Employees in the department (may be empty)")
    @GetMapping("/department/{department}")
    public ResponseEntity<List<EmployeeResponse>> getEmployeesByDepartment(@PathVariable String department) {
        return ResponseEntity.ok(employeeService.getEmployeesByDepartment(department));
    }

    @Operation(summary = "Search employees by name",
            description = "Returns employees whose full name contains the text, ignoring case")
    @ApiResponse(responseCode = "200", description = "Matching employees (may be empty)")
    @ApiResponse(responseCode = "400", description = "name is missing or blank")
    @GetMapping("/search")
    public ResponseEntity<List<EmployeeResponse>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(employeeService.searchByName(name));
    }

    @Operation(summary = "Filter employees by salary range",
            description = "Both min and max are included in the range")
    @ApiResponse(responseCode = "200", description = "Employees in the range (may be empty)")
    @ApiResponse(responseCode = "400", description = "min or max negative, or min greater than max")
    @GetMapping("/salary")
    public ResponseEntity<List<EmployeeResponse>> getBySalaryRange(@RequestParam BigDecimal min,
                                                                   @RequestParam BigDecimal max) {
        return ResponseEntity.ok(employeeService.getEmployeesBySalaryRange(min, max));
    }

    @Operation(summary = "Department summary",
            description = "One row per department with the employee count and average salary")
    @ApiResponse(responseCode = "200", description = "Summary rows")
    @GetMapping("/summary")
    public ResponseEntity<List<DepartmentSummaryResponse>> getDepartmentSummary() {
        return ResponseEntity.ok(employeeService.getDepartmentSummary());
    }
}