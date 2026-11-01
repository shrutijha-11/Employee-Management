package com.shruti.EmpManage.Service;

import com.shruti.EmpManage.Dto.DepartmentSummaryResponse;
import com.shruti.EmpManage.Dto.EmployeeRequest;
import com.shruti.EmpManage.Dto.EmployeeResponse;
import com.shruti.EmpManage.Dto.PageResponse;
import com.shruti.EmpManage.Entity.Employee;
import com.shruti.EmpManage.Exception.DuplicateEmailException;
import com.shruti.EmpManage.Exception.EmployeeNotFoundException;
import com.shruti.EmpManage.Exception.InvalidRequestException;
import com.shruti.EmpManage.Repository.EmployeeRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepo employeeRepo;

    @InjectMocks
    private EmployeeService employeeService;

    // ---------- helpers ----------

    private EmployeeRequest request(String email) {
        return new EmployeeRequest("Amit Sharma", email, "IT",
                new BigDecimal("50000"), LocalDate.of(2024, 1, 15));
    }

    private Employee employee(Long id, String name, String email, String dept, String salary) {
        Employee e = new Employee();
        e.setId(id);
        e.setFullName(name);
        e.setEmail(email);
        e.setDepartment(dept);
        e.setSalary(new BigDecimal(salary));
        e.setJoiningDate(LocalDate.of(2024, 1, 15));
        e.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
        e.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
        return e;
    }

    // ---------- create ----------

    @Test
    void createEmployee_savesEmployeeAndSetsTimestamps() {
        when(employeeRepo.existsByEmail("amit@example.com")).thenReturn(false);
        when(employeeRepo.save(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        EmployeeResponse response = employeeService.createEmployee(request("amit@example.com"));

        assertEquals(1L, response.id());
        assertEquals("Amit Sharma", response.fullName());
        assertNotNull(response.createdAt());
        assertEquals(response.createdAt(), response.updatedAt());
    }

    @Test
    void createEmployee_duplicateEmail_throwsAndDoesNotSave() {
        when(employeeRepo.existsByEmail("amit@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class,
                () -> employeeService.createEmployee(request("amit@example.com")));

        verify(employeeRepo, never()).save(any());
    }

    // ---------- get ----------

    @Test
    void getEmployeeById_found_returnsEmployee() {
        when(employeeRepo.findById(1L))
                .thenReturn(Optional.of(employee(1L, "Amit Sharma", "amit@example.com", "IT", "50000")));

        EmployeeResponse response = employeeService.getEmployeeById(1L);

        assertEquals(1L, response.id());
        assertEquals("amit@example.com", response.email());
    }

    @Test
    void getEmployeeById_notFound_throws() {
        when(employeeRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.getEmployeeById(99L));
    }

    // ---------- update ----------

    @Test
    void updateEmployee_changesFields_keepsCreatedAt_movesUpdatedAt() {
        Employee existing = employee(1L, "Amit Sharma", "amit@example.com", "IT", "50000");
        LocalDateTime originalCreated = existing.getCreatedAt();

        when(employeeRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepo.existsByEmailAndIdNot("amit@example.com", 1L)).thenReturn(false);
        when(employeeRepo.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeRequest update = new EmployeeRequest("Amit Sharma", "amit@example.com",
                "Finance", new BigDecimal("65000"), LocalDate.of(2024, 1, 15));

        EmployeeResponse response = employeeService.updateEmployee(1L, update);

        assertEquals("Finance", response.department());
        assertEquals(0, new BigDecimal("65000").compareTo(response.salary()));
        assertEquals(originalCreated, response.createdAt());
        assertTrue(response.updatedAt().isAfter(originalCreated));
    }

    @Test
    void updateEmployee_notFound_throws() {
        when(employeeRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class,
                () -> employeeService.updateEmployee(99L, request("amit@example.com")));
    }

    @Test
    void updateEmployee_emailBelongsToAnotherEmployee_throws() {
        when(employeeRepo.findById(1L))
                .thenReturn(Optional.of(employee(1L, "Amit Sharma", "amit@example.com", "IT", "50000")));
        when(employeeRepo.existsByEmailAndIdNot("priya@example.com", 1L)).thenReturn(true);

        assertThrows(DuplicateEmailException.class,
                () -> employeeService.updateEmployee(1L, request("priya@example.com")));

        verify(employeeRepo, never()).save(any());
    }

    // ---------- delete ----------

    @Test
    void deleteEmployee_existing_deletesIt() {
        Employee existing = employee(1L, "Amit Sharma", "amit@example.com", "IT", "50000");
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(existing));

        employeeService.deleteEmployee(1L);

        verify(employeeRepo).delete(existing);
    }

    @Test
    void deleteEmployee_notFound_throws() {
        when(employeeRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.deleteEmployee(99L));
        verify(employeeRepo, never()).delete(any());
    }

    // ---------- pagination and sorting ----------

    @Test
    void getAllEmployees_returnsPageDetails() {
        List<Employee> content = List.of(
                employee(3L, "A", "a@example.com", "IT", "10000"),
                employee(4L, "B", "b@example.com", "IT", "20000"));
        when(employeeRepo.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(content, PageRequest.of(1, 2), 5));

        PageResponse<EmployeeResponse> page =
                employeeService.getAllEmployees(1, 2, "salary", "desc");

        assertEquals(2, page.content().size());
        assertEquals(1, page.currentPage());
        assertEquals(2, page.pageSize());
        assertEquals(5, page.totalRecords());
        assertEquals(3, page.totalPages());
    }

    @Test
    void getAllEmployees_passesRequestedSortToRepository() {
        when(employeeRepo.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        employeeService.getAllEmployees(0, 10, "salary", "desc");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(employeeRepo).findAll(captor.capture());
        Pageable used = captor.getValue();
        assertEquals(0, used.getPageNumber());
        assertEquals(10, used.getPageSize());
        assertEquals(Sort.Direction.DESC, used.getSort().getOrderFor("salary").getDirection());
    }

    @Test
    void getAllEmployees_invalidSortBy_throws() {
        assertThrows(InvalidRequestException.class,
                () -> employeeService.getAllEmployees(0, 10, "password", "asc"));
    }

    @Test
    void getAllEmployees_invalidDirection_throws() {
        assertThrows(InvalidRequestException.class,
                () -> employeeService.getAllEmployees(0, 10, "salary", "up"));
    }

    @Test
    void getAllEmployees_negativePage_throws() {
        assertThrows(InvalidRequestException.class,
                () -> employeeService.getAllEmployees(-1, 10, "salary", "asc"));
    }

    @Test
    void getAllEmployees_sizeZero_throws() {
        assertThrows(InvalidRequestException.class,
                () -> employeeService.getAllEmployees(0, 0, "salary", "asc"));
    }

    // ---------- name search ----------

    @Test
    void searchByName_returnsMatches() {
        when(employeeRepo.findByFullNameContainingIgnoreCase("priya"))
                .thenReturn(List.of(employee(2L, "Priya Kumari", "priya@example.com", "Finance", "75000")));

        List<EmployeeResponse> result = employeeService.searchByName("priya");

        assertEquals(1, result.size());
        assertEquals("Priya Kumari", result.get(0).fullName());
    }

    @Test
    void searchByName_blank_throws() {
        assertThrows(InvalidRequestException.class, () -> employeeService.searchByName("  "));
    }

    // ---------- salary range ----------

    @Test
    void getEmployeesBySalaryRange_validRange_returnsMatches() {
        BigDecimal min = new BigDecimal("50000");
        BigDecimal max = new BigDecimal("80000");
        when(employeeRepo.findBySalaryBetween(min, max))
                .thenReturn(List.of(employee(2L, "Priya Kumari", "priya@example.com", "Finance", "75000")));

        List<EmployeeResponse> result = employeeService.getEmployeesBySalaryRange(min, max);

        assertEquals(1, result.size());
    }

    @Test
    void getEmployeesBySalaryRange_negativeMin_throws() {
        assertThrows(InvalidRequestException.class,
                () -> employeeService.getEmployeesBySalaryRange(new BigDecimal("-1"), new BigDecimal("5000")));
    }

    @Test
    void getEmployeesBySalaryRange_negativeMax_throws() {
        assertThrows(InvalidRequestException.class,
                () -> employeeService.getEmployeesBySalaryRange(new BigDecimal("0"), new BigDecimal("-5")));
    }

    @Test
    void getEmployeesBySalaryRange_minGreaterThanMax_throws() {
        assertThrows(InvalidRequestException.class,
                () -> employeeService.getEmployeesBySalaryRange(new BigDecimal("90000"), new BigDecimal("50000")));
    }

    // ---------- department summary ----------

    @Test
    void getDepartmentSummary_roundsAverageToTwoDecimals() {
        when(employeeRepo.getDepartmentSummary())
                .thenReturn(List.of(new DepartmentSummaryResponse("IT", 3L, 70000.3333)));

        List<DepartmentSummaryResponse> result = employeeService.getDepartmentSummary();

        assertEquals(1, result.size());
        assertEquals("IT", result.get(0).department());
        assertEquals(3L, result.get(0).employeeCount());
        assertEquals(70000.33, result.get(0).averageSalary(), 0.001);
    }
}