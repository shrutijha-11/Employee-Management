package com.shruti.EmpManage.Service;

import com.shruti.EmpManage.Dto.DepartmentRequest;
import com.shruti.EmpManage.Dto.DepartmentResponse;
import com.shruti.EmpManage.Entity.Department;
import com.shruti.EmpManage.Exception.DepartmentInUseException;
import com.shruti.EmpManage.Exception.DepartmentNotFoundException;
import com.shruti.EmpManage.Exception.DuplicateDepartmentException;
import com.shruti.EmpManage.Repository.DepartmentRepo;
import com.shruti.EmpManage.Repository.EmployeeRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepo departmentRepo;

    @Mock
    private EmployeeRepo employeeRepo;

    @InjectMocks
    private DepartmentService departmentService;

    private Department department(Long id, String name, String location) {
        Department d = new Department();
        d.setId(id);
        d.setName(name);
        d.setLocation(location);
        return d;
    }

    // ---------- create ----------

    @Test
    void create_validRequest_savesAndReturnsDepartment() {
        when(departmentRepo.existsByNameIgnoreCase("IT")).thenReturn(false);
        when(departmentRepo.save(any(Department.class))).thenAnswer(inv -> {
            Department d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });

        DepartmentResponse response = departmentService.createDepartment(new DepartmentRequest("IT", "Kolkata"));

        assertEquals(1L, response.id());
        assertEquals("IT", response.name());
        assertEquals("Kolkata", response.location());
    }

    @Test
    void create_nameAndLocationAreTrimmed() {
        when(departmentRepo.existsByNameIgnoreCase("IT")).thenReturn(false);
        when(departmentRepo.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        DepartmentResponse response = departmentService.createDepartment(new DepartmentRequest("  IT ", " Kolkata "));

        assertEquals("IT", response.name());
        assertEquals("Kolkata", response.location());
    }

    @Test
    void create_duplicateName_throwsAndDoesNotSave() {
        when(departmentRepo.existsByNameIgnoreCase("IT")).thenReturn(true);

        assertThrows(DuplicateDepartmentException.class,
                () -> departmentService.createDepartment(new DepartmentRequest("IT", "Delhi")));

        verify(departmentRepo, never()).save(any());
    }

    // ---------- read ----------

    @Test
    void getAll_returnsAllDepartments() {
        when(departmentRepo.findAll()).thenReturn(List.of(
                department(1L, "IT", "Kolkata"), department(2L, "Finance", "Delhi")));

        List<DepartmentResponse> result = departmentService.getAllDepartments();

        assertEquals(2, result.size());
        assertEquals("Finance", result.get(1).name());
    }

    @Test
    void getById_found_returnsDepartment() {
        when(departmentRepo.findById(1L)).thenReturn(Optional.of(department(1L, "IT", "Kolkata")));

        assertEquals("IT", departmentService.getDepartmentById(1L).name());
    }

    @Test
    void getById_notFound_throws() {
        when(departmentRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(DepartmentNotFoundException.class, () -> departmentService.getDepartmentById(99L));
    }

    // ---------- update ----------

    @Test
    void update_changesLocation_keepsName() {
        when(departmentRepo.findById(1L)).thenReturn(Optional.of(department(1L, "IT", "Kolkata")));
        when(departmentRepo.existsByNameIgnoreCaseAndIdNot("IT", 1L)).thenReturn(false);
        when(departmentRepo.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        DepartmentResponse response = departmentService.updateDepartment(1L, new DepartmentRequest("IT", "Delhi"));

        assertEquals("IT", response.name());
        assertEquals("Delhi", response.location());
    }

    @Test
    void update_nameBelongsToAnotherDepartment_throwsAndDoesNotSave() {
        when(departmentRepo.findById(1L)).thenReturn(Optional.of(department(1L, "IT", "Kolkata")));
        when(departmentRepo.existsByNameIgnoreCaseAndIdNot("Finance", 1L)).thenReturn(true);

        assertThrows(DuplicateDepartmentException.class,
                () -> departmentService.updateDepartment(1L, new DepartmentRequest("Finance", "Kolkata")));

        verify(departmentRepo, never()).save(any());
    }

    @Test
    void update_notFound_throws() {
        when(departmentRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(DepartmentNotFoundException.class,
                () -> departmentService.updateDepartment(99L, new DepartmentRequest("IT", "Kolkata")));
    }

    // ---------- delete ----------

    @Test
    void delete_departmentWithNoEmployees_deletesIt() {
        Department existing = department(1L, "IT", "Kolkata");
        when(departmentRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepo.existsByDepartmentId(1L)).thenReturn(false);

        departmentService.deleteDepartment(1L);

        verify(departmentRepo).delete(existing);
    }

    @Test
    void delete_departmentStillHasEmployees_throwsAndKeepsTheDepartment() {
        when(departmentRepo.findById(1L)).thenReturn(Optional.of(department(1L, "IT", "Kolkata")));
        when(employeeRepo.existsByDepartmentId(1L)).thenReturn(true);

        assertThrows(DepartmentInUseException.class, () -> departmentService.deleteDepartment(1L));

        verify(departmentRepo, never()).delete(any());
    }

    @Test
    void delete_notFound_throwsWithoutCheckingEmployees() {
        when(departmentRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(DepartmentNotFoundException.class, () -> departmentService.deleteDepartment(99L));

        verifyNoInteractions(employeeRepo);
        verify(departmentRepo, never()).delete(any());
    }
}
