package com.shruti.EmpManage.Controller;

import com.shruti.EmpManage.Dto.DepartmentSummaryResponse;
import com.shruti.EmpManage.Dto.EmployeeFilterRequest;
import com.shruti.EmpManage.Dto.EmployeeHistoryResponse;
import com.shruti.EmpManage.Dto.EmployeeRequest;
import com.shruti.EmpManage.Dto.EmployeeResponse;
import com.shruti.EmpManage.Dto.EmployeeUpdateRequest;
import com.shruti.EmpManage.Dto.PageResponse;
import com.shruti.EmpManage.Dto.StatusUpdateRequest;
import com.shruti.EmpManage.Service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employees", description = "Employee management operations")
public class EmpCont {

    private final EmployeeService employeeService;

    public EmpCont(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // ---------- create ----------

    @Operation(summary = "Create an employee",
            description = "The department is sent by name and must already exist. New employees are ACTIVE with version 0.")
    @ApiResponse(responseCode = "201", description = "Employee created")
    @ApiResponse(responseCode = "400", description = "Validation failed or department does not exist")
    @ApiResponse(responseCode = "409", description = "Email already exists")
    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.createEmployee(request));
    }

    // ---------- list ----------

    @Operation(summary = "List employees with pagination, sorting and optional status filter",
            description = "sortBy accepts fullName, email, department, salary or joiningDate. "
                    + "direction accepts asc or desc. Page numbers start at 0. "
                    + "status (ACTIVE or INACTIVE) is optional; without it every employee is returned.")
    @ApiResponse(responseCode = "200", description = "Page of employees")
    @ApiResponse(responseCode = "400", description = "Invalid page, size, sortBy, direction or status")
    @GetMapping
    public ResponseEntity<PageResponse<EmployeeResponse>> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(employeeService.getAllEmployees(page, size, sortBy, direction, status));
    }

    // ---------- read one ----------

    @Operation(summary = "Get an employee by ID")
    @ApiResponse(responseCode = "200", description = "Employee found")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    // ---------- update ----------

    @Operation(summary = "Update an employee",
            description = "The request must include the employee's current version. "
                    + "If it does not match the saved version, nothing is changed and 409 is returned.")
    @ApiResponse(responseCode = "200", description = "Employee updated; version increased by 1")
    @ApiResponse(responseCode = "400", description = "Validation failed, version missing, or department does not exist")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    @ApiResponse(responseCode = "409", description = "Version mismatch, or email belongs to another employee")
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(@PathVariable Long id,
                                                           @Valid @RequestBody EmployeeUpdateRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @Operation(summary = "Change an employee's status",
            description = "Allowed values: ACTIVE, INACTIVE. Increases the version by 1.")
    @ApiResponse(responseCode = "200", description = "Status changed")
    @ApiResponse(responseCode = "400", description = "Unknown or missing status")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    @PatchMapping("/{id}/status")
    public ResponseEntity<EmployeeResponse> updateStatus(@PathVariable Long id,
                                                         @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(employeeService.updateStatus(id, request));
    }

    // ---------- delete ----------

    @Operation(summary = "Delete an employee")
    @ApiResponse(responseCode = "204", description = "Employee deleted")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- history ----------

    @Operation(summary = "Get an employee's change history",
            description = "One row per field that changed on an update or status change, newest first")
    @ApiResponse(responseCode = "200", description = "History rows (empty if nothing has changed)")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    @GetMapping("/{id}/history")
    public ResponseEntity<List<EmployeeHistoryResponse>> getEmployeeHistory(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeHistory(id));
    }

    // ---------- searches ----------

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

    @Operation(summary = "Combined filter",
            description = "Every filter is optional, and all the ones sent apply together. "
                    + "name is a case-insensitive partial match on the full name; department is a "
                    + "case-insensitive exact match; minSalary/maxSalary and joinedFrom/joinedTo include "
                    + "both limits. Dates use yyyy-MM-dd. Paging and sorting work as in the employee list.")
    @ApiResponse(responseCode = "200", description = "Page of matching employees (may be empty)")
    @ApiResponse(responseCode = "400",
            description = "Negative salary, minSalary greater than maxSalary, joinedFrom after joinedTo, "
                    + "bad date format, or invalid paging/sorting values")
    @GetMapping("/filter")
    public ResponseEntity<PageResponse<EmployeeResponse>> filterEmployees(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) BigDecimal minSalary,
            @RequestParam(required = false) BigDecimal maxSalary,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        EmployeeFilterRequest filter = new EmployeeFilterRequest(
                name, department, minSalary, maxSalary, joinedFrom, joinedTo);
        return ResponseEntity.ok(employeeService.filterEmployees(filter, page, size, sortBy, direction));
    }

    // ---------- summary ----------

    @Operation(summary = "Department summary",
            description = "One row per department with the employee count and average salary")
    @ApiResponse(responseCode = "200", description = "Summary rows")
    @GetMapping("/summary")
    public ResponseEntity<List<DepartmentSummaryResponse>> getDepartmentSummary() {
        return ResponseEntity.ok(employeeService.getDepartmentSummary());
    }

    // ---------- CSV export ----------

    @Operation(summary = "Export employees as CSV",
            description = "Downloads a CSV file. department and status are optional and apply together when sent. "
                    + "Columns: id, fullName, email, department, salary, joiningDate, status, createdAt, updatedAt.")
    @ApiResponse(responseCode = "200", description = "CSV file (Content-Type: text/csv)",
            content = @Content(mediaType = "text/csv"))
    @ApiResponse(responseCode = "400", description = "Unknown status")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportEmployeesCsv(@RequestParam(required = false) String department,
                                                     @RequestParam(required = false) String status) {
        String csv = employeeService.exportEmployeesCsv(department, status);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employees.csv\"")
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}