package com.shruti.EmpManage.Controller;

import com.shruti.EmpManage.Dto.DepartmentRequest;
import com.shruti.EmpManage.Dto.DepartmentResponse;
import com.shruti.EmpManage.Service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@Tag(name = "Departments", description = "Department management operations")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @Operation(summary = "Create a department")
    @ApiResponse(responseCode = "201", description = "Department created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "409", description = "Department name already exists")
    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.createDepartment(request));
    }

    @Operation(summary = "List all departments")
    @ApiResponse(responseCode = "200", description = "All departments")
    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @Operation(summary = "Get a department by ID")
    @ApiResponse(responseCode = "200", description = "Department found")
    @ApiResponse(responseCode = "404", description = "Department not found")
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @Operation(summary = "Update a department")
    @ApiResponse(responseCode = "200", description = "Department updated")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Department not found")
    @ApiResponse(responseCode = "409", description = "Department name already exists")
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> updateDepartment(@PathVariable Long id,
                                                               @Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request));
    }

    @Operation(summary = "Delete a department",
            description = "Fails with 409 if employees still belong to the department")
    @ApiResponse(responseCode = "204", description = "Department deleted")
    @ApiResponse(responseCode = "404", description = "Department not found")
    @ApiResponse(responseCode = "409", description = "Department still has employees")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
}
