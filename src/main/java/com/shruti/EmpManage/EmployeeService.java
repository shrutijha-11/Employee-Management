package com.shruti.EmpManage.Service;

import com.shruti.EmpManage.Dto.DepartmentSummaryResponse;
import com.shruti.EmpManage.Dto.EmployeeRequest;
import com.shruti.EmpManage.Dto.EmployeeResponse;
import com.shruti.EmpManage.Entity.Employee;
import com.shruti.EmpManage.Exception.DuplicateEmailException;
import com.shruti.EmpManage.Exception.EmployeeNotFoundException;
import com.shruti.EmpManage.Mapper.EmployeeMapper;
import com.shruti.EmpManage.Repository.EmployeeRepo;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

import com.shruti.EmpManage.Dto.PageResponse;
import com.shruti.EmpManage.Exception.InvalidRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;

import com.shruti.EmpManage.Dto.DepartmentSummaryResponse;
import java.util.List;
import java.util.Set;

@Service
public class EmployeeService {
    private final EmployeeRepo employeeRepository;
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("fullName", "email", "department", "salary", "joiningDate");

    public EmployeeService(EmployeeRepo employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public EmployeeResponse createEmployee(EmployeeRequest request) {
        if (employeeRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        Employee employee = EmployeeMapper.toEntity(request);
        LocalDateTime now = LocalDateTime.now();
        employee.setCreatedAt(now);
        employee.setUpdatedAt(now);
        return EmployeeMapper.toResponse(employeeRepository.save(employee));
    }

    public PageResponse<EmployeeResponse> getAllEmployees(int page, int size, String sortBy, String direction) {
            if (page < 0) {
                throw new InvalidRequestException("page must be 0 or greater");
            }
            if (size < 1 || size > 100) {
                throw new InvalidRequestException("size must be between 1 and 100");
            }
            if (!SORTABLE_FIELDS.contains(sortBy)) {
                throw new InvalidRequestException(
                        "sortBy must be one of: fullName, email, department, salary, joiningDate");
            }
            if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
                throw new InvalidRequestException("direction must be 'asc' or 'desc'");
            }

            Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Employee> result = employeeRepository.findAll(pageable);

            return new PageResponse<>(
                    result.getContent().stream().map(EmployeeMapper::toResponse).toList(),
                    result.getNumber(),
                    result.getSize(),
                    result.getTotalElements(),
                    result.getTotalPages()
            );
    }

    public EmployeeResponse getEmployeeById(Long id) {
        return EmployeeMapper.toResponse(findEmployeeOrThrow(id));
    }

    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee existing = findEmployeeOrThrow(id);

        if (employeeRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new DuplicateEmailException(request.email());
        }

        EmployeeMapper.updateEntity(existing, request);
        existing.setUpdatedAt(LocalDateTime.now());
        return EmployeeMapper.toResponse(employeeRepository.save(existing));
    }

    public void deleteEmployee(Long id) {
        employeeRepository.delete(findEmployeeOrThrow(id));
    }

    public List<EmployeeResponse> getEmployeesByDepartment(String department) {
        return employeeRepository.findByDepartmentIgnoreCase(department).stream()
                .map(EmployeeMapper::toResponse)
                .toList();
    }

    private Employee findEmployeeOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }
    public List<EmployeeResponse> searchByName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidRequestException("name must not be blank");
        }
        return employeeRepository.findByFullNameContainingIgnoreCase(name.trim()).stream()
                .map(EmployeeMapper::toResponse)
                .toList();
    }

    public List<EmployeeResponse> getEmployeesBySalaryRange(BigDecimal min, BigDecimal max) {
        if (min.signum() < 0 || max.signum() < 0) {
            throw new InvalidRequestException("min and max must not be negative");
        }
        if (min.compareTo(max) > 0) {
            throw new InvalidRequestException("min must not be greater than max");
        }
        return employeeRepository.findBySalaryBetween(min, max).stream()
                .map(EmployeeMapper::toResponse)
                .toList();
    }
    public List<DepartmentSummaryResponse> getDepartmentSummary() {
        return employeeRepository.getDepartmentSummary().stream()
                .map(s -> new DepartmentSummaryResponse(
                        s.department(),
                        s.employeeCount(),
                        Math.round(s.averageSalary() * 100.0) / 100.0))
                .toList();
    }
}
