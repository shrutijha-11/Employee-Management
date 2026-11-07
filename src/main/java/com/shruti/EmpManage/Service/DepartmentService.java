package com.shruti.EmpManage.Service;

import com.shruti.EmpManage.Dto.DepartmentRequest;
import com.shruti.EmpManage.Dto.DepartmentResponse;
import com.shruti.EmpManage.Entity.Department;
import com.shruti.EmpManage.Exception.DepartmentInUseException;
import com.shruti.EmpManage.Exception.DepartmentNotFoundException;
import com.shruti.EmpManage.Exception.DuplicateDepartmentException;
import com.shruti.EmpManage.Mapper.DepartmentMapper;
import com.shruti.EmpManage.Repository.DepartmentRepo;
import com.shruti.EmpManage.Repository.EmployeeRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepo departmentRepo;
    private final EmployeeRepo employeeRepo;

    public DepartmentService(DepartmentRepo departmentRepo, EmployeeRepo employeeRepo) {
        this.departmentRepo = departmentRepo;
        this.employeeRepo = employeeRepo;
    }

    public DepartmentResponse createDepartment(DepartmentRequest request) {
        String name = request.name().trim();
        if (departmentRepo.existsByNameIgnoreCase(name)) {
            throw new DuplicateDepartmentException(name);
        }
        Department saved = departmentRepo.save(DepartmentMapper.toEntity(request));
        return DepartmentMapper.toResponse(saved);
    }

    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepo.findAll().stream()
                .map(DepartmentMapper::toResponse)
                .toList();
    }

    public DepartmentResponse getDepartmentById(Long id) {
        return DepartmentMapper.toResponse(findDepartmentOrThrow(id));
    }

    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department existing = findDepartmentOrThrow(id);
        String name = request.name().trim();
        if (departmentRepo.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateDepartmentException(name);
        }
        DepartmentMapper.updateEntity(existing, request);
        return DepartmentMapper.toResponse(departmentRepo.save(existing));
    }

    public void deleteDepartment(Long id) {
        Department existing = findDepartmentOrThrow(id);
        if (employeeRepo.existsByDepartmentId(id)) {
            throw new DepartmentInUseException(id);
        }
        departmentRepo.delete(existing);
    }

    private Department findDepartmentOrThrow(Long id) {
        return departmentRepo.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));
    }
}
