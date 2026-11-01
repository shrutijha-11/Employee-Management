package com.shruti.EmpManage.Mapper;

import com.shruti.EmpManage.Dto.EmployeeRequest;
import com.shruti.EmpManage.Dto.EmployeeResponse;
import com.shruti.EmpManage.Entity.Employee;

public class EmployeeMapper {
    private EmployeeMapper() {
    }

    public static Employee toEntity(EmployeeRequest request) {
        Employee employee = new Employee();
        updateEntity(employee, request);
        return employee;
    }

    public static void updateEntity(Employee employee, EmployeeRequest request) {
        employee.setFullName(request.fullName());
        employee.setEmail(request.email());
        employee.setDepartment(request.department());
        employee.setSalary(request.salary());
        employee.setJoiningDate(request.joiningDate());
    }

    public static EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(employee.getId(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getDepartment(),
                employee.getSalary(),
                employee.getJoiningDate(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}
