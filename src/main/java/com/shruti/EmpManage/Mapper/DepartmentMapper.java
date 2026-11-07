package com.shruti.EmpManage.Mapper;

import com.shruti.EmpManage.Dto.DepartmentRequest;
import com.shruti.EmpManage.Dto.DepartmentResponse;
import com.shruti.EmpManage.Entity.Department;

public class DepartmentMapper {

    private DepartmentMapper() {
    }

    public static Department toEntity(DepartmentRequest request) {
        Department department = new Department();
        updateEntity(department, request);
        return department;
    }

    public static void updateEntity(Department department, DepartmentRequest request) {
        department.setName(request.name().trim());
        department.setLocation(request.location().trim());
    }

    public static DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(department.getId(), department.getName(), department.getLocation());
    }
}
