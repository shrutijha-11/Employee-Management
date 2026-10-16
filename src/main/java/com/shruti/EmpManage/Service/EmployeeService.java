package com.shruti.EmpManage.Service;

import com.shruti.EmpManage.Entity.Employee;
import com.shruti.EmpManage.Exception.DuplicateEmailException;
import com.shruti.EmpManage.Exception.EmployeeNotFoundException;
import com.shruti.EmpManage.Repository.EmployeeRepo;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class EmployeeService {
    private final EmployeeRepo employeeRepository;

    public EmployeeService(EmployeeRepo employeeRepository) {
        this.employeeRepository = employeeRepository;
    }
    public Employee createEmployee(Employee employee) {
        if (employeeRepository.existsByEmail(employee.getEmail())) {
            throw new DuplicateEmailException(employee.getEmail());
        }
        employee.setId(null); // make sure the DB generates the ID
        return employeeRepository.save(employee);
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    public Employee updateEmployee(Long id, Employee updatedEmployee) {
        Employee existing = getEmployeeById(id);

        if (employeeRepository.existsByEmailAndIdNot(updatedEmployee.getEmail(), id)) {
            throw new DuplicateEmailException(updatedEmployee.getEmail());
        }

        existing.setFullName(updatedEmployee.getFullName());
        existing.setEmail(updatedEmployee.getEmail());
        existing.setDepartment(updatedEmployee.getDepartment());
        existing.setSalary(updatedEmployee.getSalary());
        existing.setJoiningDate(updatedEmployee.getJoiningDate());

        return employeeRepository.save(existing);
    }

    public void deleteEmployee(Long id) {
        Employee existing = getEmployeeById(id);
        employeeRepository.delete(existing);
    }

    public List<Employee> getEmployeesByDepartment(String department) {
        return employeeRepository.findByDepartmentIgnoreCase(department);
    }
}
