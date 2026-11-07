package com.shruti.EmpManage.Mapper;

import com.shruti.EmpManage.Dto.EmployeeRequest;
import com.shruti.EmpManage.Dto.EmployeeResponse;
import com.shruti.EmpManage.Dto.EmployeeUpdateRequest;
import com.shruti.EmpManage.Entity.Employee;

public class EmployeeMapper {

    private EmployeeMapper() {
    }

    // the department is set by the service, which looks it up by name
    public static Employee toEntity(EmployeeRequest request) {
        Employee employee = new Employee();
        employee.setFullName(request.fullName());
        employee.setEmail(request.email());
        employee.setSalary(request.salary());
        employee.setJoiningDate(request.joiningDate());
        return employee;
    }

    public static void updateEntity(Employee employee, EmployeeUpdateRequest request) {
        employee.setFullName(request.fullName());
        employee.setEmail(request.email());
        employee.setSalary(request.salary());
        employee.setJoiningDate(request.joiningDate());
    }

    public static EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getDepartment().getName(),
                employee.getSalary(),
                employee.getJoiningDate(),
                employee.getStatus(),
                employee.getVersion(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}