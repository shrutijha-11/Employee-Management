package com.shruti.EmpManage.Service;

import com.shruti.EmpManage.Dto.DepartmentSummaryResponse;
import com.shruti.EmpManage.Dto.EmployeeHistoryResponse;
import com.shruti.EmpManage.Dto.EmployeeRequest;
import com.shruti.EmpManage.Dto.EmployeeResponse;
import com.shruti.EmpManage.Dto.EmployeeUpdateRequest;
import com.shruti.EmpManage.Dto.PageResponse;
import com.shruti.EmpManage.Dto.StatusUpdateRequest;
import com.shruti.EmpManage.Entity.Department;
import com.shruti.EmpManage.Entity.Employee;
import com.shruti.EmpManage.Entity.EmployeeHistory;
import com.shruti.EmpManage.Entity.EmployeeStatus;
import com.shruti.EmpManage.Exception.DuplicateEmailException;
import com.shruti.EmpManage.Exception.EmployeeNotFoundException;
import com.shruti.EmpManage.Exception.InvalidRequestException;
import com.shruti.EmpManage.Exception.VersionConflictException;
import com.shruti.EmpManage.Repository.DepartmentRepo;
import com.shruti.EmpManage.Repository.EmployeeHistoryRepo;
import com.shruti.EmpManage.Repository.EmployeeRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepo employeeRepo;

    @Mock
    private DepartmentRepo departmentRepo;

    @Mock
    private EmployeeHistoryRepo historyRepo;

    @InjectMocks
    private EmployeeService employeeService;

    @Captor
    private ArgumentCaptor<List<EmployeeHistory>> historyCaptor;

    // ---------- helpers ----------

    private Department department(Long id, String name) {
        Department d = new Department();
        d.setId(id);
        d.setName(name);
        d.setLocation("Kolkata");
        return d;
    }

    private Employee employee(Long id, String name, String email, Department dept, String salary) {
        Employee e = new Employee();
        e.setId(id);
        e.setFullName(name);
        e.setEmail(email);
        e.setDepartment(dept);
        e.setSalary(new BigDecimal(salary));
        e.setJoiningDate(LocalDate.of(2024, 1, 15));
        e.setStatus(EmployeeStatus.ACTIVE);
        e.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
        e.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
        ReflectionTestUtils.setField(e, "version", 0L);   // version has no setter; Hibernate manages it
        return e;
    }

    private EmployeeRequest createRequest(String email, String dept) {
        return new EmployeeRequest("Amit Sharma", email, dept,
                new BigDecimal("50000"), LocalDate.of(2024, 1, 15));
    }

    private EmployeeUpdateRequest updateRequest(String name, String email, String dept,
                                                String salary, Long version) {
        return new EmployeeUpdateRequest(name, email, dept, new BigDecimal(salary),
                LocalDate.of(2024, 1, 15), version);
    }

    private List<EmployeeHistory> savedHistory() {
        verify(historyRepo).saveAll(historyCaptor.capture());
        return historyCaptor.getValue();
    }

    // ---------- create ----------

    @Test
    void create_validRequest_savesActiveEmployeeWithTimestampsAndNoHistory() {
        Department it = department(1L, "IT");
        when(employeeRepo.existsByEmail("amit@example.com")).thenReturn(false);
        when(departmentRepo.findByNameIgnoreCase("IT")).thenReturn(Optional.of(it));
        when(employeeRepo.save(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        EmployeeResponse response = employeeService.createEmployee(createRequest("amit@example.com", "IT"));

        assertEquals(1L, response.id());
        assertEquals("IT", response.department());
        assertEquals(EmployeeStatus.ACTIVE, response.status());
        assertNotNull(response.createdAt());
        assertEquals(response.createdAt(), response.updatedAt());
        verifyNoInteractions(historyRepo);   // creating an employee writes no history
    }

    @Test
    void create_duplicateEmail_throwsAndDoesNotSave() {
        when(employeeRepo.existsByEmail("amit@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class,
                () -> employeeService.createEmployee(createRequest("amit@example.com", "IT")));

        verify(employeeRepo, never()).save(any());
    }

    @Test
    void create_unknownDepartment_throwsInvalidRequestAndDoesNotSave() {
        when(employeeRepo.existsByEmail("amit@example.com")).thenReturn(false);
        when(departmentRepo.findByNameIgnoreCase("Nope")).thenReturn(Optional.empty());

        assertThrows(InvalidRequestException.class,
                () -> employeeService.createEmployee(createRequest("amit@example.com", "Nope")));

        verify(employeeRepo, never()).save(any());
    }

    @Test
    void create_departmentNameIsTrimmedAndCaseInsensitive_responseShowsStoredName() {
        Department it = department(1L, "IT");
        when(employeeRepo.existsByEmail("amit@example.com")).thenReturn(false);
        when(departmentRepo.findByNameIgnoreCase("it")).thenReturn(Optional.of(it));
        when(employeeRepo.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeResponse response = employeeService.createEmployee(createRequest("amit@example.com", "  it "));

        assertEquals("IT", response.department());
    }

    // ---------- get ----------

    @Test
    void getById_found_returnsEmployee() {
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(
                employee(1L, "Amit Sharma", "amit@example.com", department(1L, "IT"), "50000")));

        EmployeeResponse response = employeeService.getEmployeeById(1L);

        assertEquals(1L, response.id());
        assertEquals("IT", response.department());
        assertEquals(0L, response.version());
    }

    @Test
    void getById_notFound_throws() {
        when(employeeRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.getEmployeeById(99L));
    }

    // ---------- update: success and history ----------

    @Test
    void update_changedFields_writesOneHistoryRowPerChangedField() {
        Department it = department(1L, "IT");
        Department finance = department(2L, "Finance");
        Employee existing = employee(1L, "Amit Sharma", "amit@example.com", it, "50000");
        LocalDateTime originalCreated = existing.getCreatedAt();

        when(employeeRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepo.existsByEmailAndIdNot("amit@example.com", 1L)).thenReturn(false);
        when(departmentRepo.findByNameIgnoreCase("Finance")).thenReturn(Optional.of(finance));
        when(employeeRepo.saveAndFlush(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeResponse response = employeeService.updateEmployee(1L,
                updateRequest("Amit Sharma", "amit@example.com", "Finance", "55000", 0L));

        assertEquals("Finance", response.department());
        assertEquals(0, new BigDecimal("55000").compareTo(response.salary()));
        assertEquals(originalCreated, response.createdAt());               // createdAt never changes
        assertTrue(response.updatedAt().isAfter(originalCreated));         // updatedAt moves

        List<EmployeeHistory> rows = savedHistory();
        assertEquals(2, rows.size());
        EmployeeHistory deptRow = rows.stream().filter(r -> r.getFieldName().equals("department")).findFirst().orElseThrow();
        assertEquals("IT", deptRow.getOldValue());
        assertEquals("Finance", deptRow.getNewValue());
        assertEquals(1L, deptRow.getEmployeeId());
        assertNotNull(deptRow.getChangedAt());
        EmployeeHistory salaryRow = rows.stream().filter(r -> r.getFieldName().equals("salary")).findFirst().orElseThrow();
        assertEquals("50000.00", salaryRow.getOldValue());
        assertEquals("55000.00", salaryRow.getNewValue());
    }

    @Test
    void update_onlyFullNameChanged_writesExactlyOneRow() {
        Department it = department(1L, "IT");
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(
                employee(1L, "Amit Sharma", "amit@example.com", it, "50000")));
        when(employeeRepo.existsByEmailAndIdNot("amit@example.com", 1L)).thenReturn(false);
        when(departmentRepo.findByNameIgnoreCase("IT")).thenReturn(Optional.of(it));
        when(employeeRepo.saveAndFlush(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        employeeService.updateEmployee(1L,
                updateRequest("Amit Kumar Sharma", "amit@example.com", "IT", "50000", 0L));

        List<EmployeeHistory> rows = savedHistory();
        assertEquals(1, rows.size());
        assertEquals("fullName", rows.get(0).getFieldName());
        assertEquals("Amit Sharma", rows.get(0).getOldValue());
        assertEquals("Amit Kumar Sharma", rows.get(0).getNewValue());
    }

    @Test
    void update_sameValuesSent_writesNoHistoryRows() {
        // salary 50000.00 vs 50000 and department "it" vs "IT" are not changes
        Department it = department(1L, "IT");
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(
                employee(1L, "Amit Sharma", "amit@example.com", it, "50000.00")));
        when(employeeRepo.existsByEmailAndIdNot("amit@example.com", 1L)).thenReturn(false);
        when(departmentRepo.findByNameIgnoreCase("it")).thenReturn(Optional.of(it));
        when(employeeRepo.saveAndFlush(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        employeeService.updateEmployee(1L,
                updateRequest("Amit Sharma", "amit@example.com", "it", "50000", 0L));

        assertTrue(savedHistory().isEmpty());
    }

    // ---------- update: version check ----------

    @Test
    void update_staleVersion_throwsAndChangesNothing() {
        Employee existing = employee(1L, "Amit Sharma", "amit@example.com", department(1L, "IT"), "50000");
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(existing));

        VersionConflictException ex = assertThrows(VersionConflictException.class,
                () -> employeeService.updateEmployee(1L,
                        updateRequest("Changed Name", "amit@example.com", "IT", "99999", 5L)));

        assertTrue(ex.getMessage().contains("5"));
        assertEquals("Amit Sharma", existing.getFullName());     // nothing was changed
        verify(employeeRepo, never()).saveAndFlush(any());
        verify(employeeRepo, never()).save(any());
        verifyNoInteractions(historyRepo);                       // and no history written
        verifyNoInteractions(departmentRepo);
    }

    @Test
    void update_missingVersion_isTreatedAsConflictAndChangesNothing() {
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(
                employee(1L, "Amit Sharma", "amit@example.com", department(1L, "IT"), "50000")));

        assertThrows(VersionConflictException.class,
                () -> employeeService.updateEmployee(1L,
                        updateRequest("Amit Sharma", "amit@example.com", "IT", "50000", null)));

        verify(employeeRepo, never()).saveAndFlush(any());
        verifyNoInteractions(historyRepo);
    }

    // ---------- update: other failures ----------

    @Test
    void update_emailBelongsToAnotherEmployee_throwsAndChangesNothing() {
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(
                employee(1L, "Amit Sharma", "amit@example.com", department(1L, "IT"), "50000")));
        when(employeeRepo.existsByEmailAndIdNot("priya@example.com", 1L)).thenReturn(true);

        assertThrows(DuplicateEmailException.class,
                () -> employeeService.updateEmployee(1L,
                        updateRequest("Amit Sharma", "priya@example.com", "IT", "50000", 0L)));

        verify(employeeRepo, never()).saveAndFlush(any());
        verifyNoInteractions(historyRepo);
    }

    @Test
    void update_unknownDepartment_throwsAndChangesNothing() {
        Employee existing = employee(1L, "Amit Sharma", "amit@example.com", department(1L, "IT"), "50000");
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepo.existsByEmailAndIdNot("amit@example.com", 1L)).thenReturn(false);
        when(departmentRepo.findByNameIgnoreCase("Nope")).thenReturn(Optional.empty());

        assertThrows(InvalidRequestException.class,
                () -> employeeService.updateEmployee(1L,
                        updateRequest("Changed Name", "amit@example.com", "Nope", "50000", 0L)));

        assertEquals("Amit Sharma", existing.getFullName());
        verify(employeeRepo, never()).saveAndFlush(any());
        verifyNoInteractions(historyRepo);
    }

    @Test
    void update_notFound_throws() {
        when(employeeRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class,
                () -> employeeService.updateEmployee(99L,
                        updateRequest("Amit Sharma", "amit@example.com", "IT", "50000", 0L)));
    }

    // ---------- status change ----------

    @Test
    void updateStatus_changesStatusAndWritesOneHistoryRow() {
        Employee existing = employee(1L, "Amit Sharma", "amit@example.com", department(1L, "IT"), "50000");
        LocalDateTime originalUpdated = existing.getUpdatedAt();
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepo.saveAndFlush(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeResponse response = employeeService.updateStatus(1L, new StatusUpdateRequest("INACTIVE"));

        assertEquals(EmployeeStatus.INACTIVE, response.status());
        assertTrue(response.updatedAt().isAfter(originalUpdated));
        List<EmployeeHistory> rows = savedHistory();
        assertEquals(1, rows.size());
        assertEquals("status", rows.get(0).getFieldName());
        assertEquals("ACTIVE", rows.get(0).getOldValue());
        assertEquals("INACTIVE", rows.get(0).getNewValue());
    }

    @Test
    void updateStatus_sameStatus_writesNoHistoryRow() {
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(
                employee(1L, "Amit Sharma", "amit@example.com", department(1L, "IT"), "50000")));
        when(employeeRepo.saveAndFlush(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        employeeService.updateStatus(1L, new StatusUpdateRequest("ACTIVE"));

        assertTrue(savedHistory().isEmpty());
    }

    @Test
    void updateStatus_lowercaseValueIsAccepted() {
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(
                employee(1L, "Amit Sharma", "amit@example.com", department(1L, "IT"), "50000")));
        when(employeeRepo.saveAndFlush(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeResponse response = employeeService.updateStatus(1L, new StatusUpdateRequest("inactive"));

        assertEquals(EmployeeStatus.INACTIVE, response.status());
    }

    @Test
    void updateStatus_unknownStatus_throwsAndChangesNothing() {
        Employee existing = employee(1L, "Amit Sharma", "amit@example.com", department(1L, "IT"), "50000");
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(InvalidRequestException.class,
                () -> employeeService.updateStatus(1L, new StatusUpdateRequest("BOGUS")));

        assertEquals(EmployeeStatus.ACTIVE, existing.getStatus());
        verify(employeeRepo, never()).saveAndFlush(any());
        verifyNoInteractions(historyRepo);
    }

    @Test
    void updateStatus_notFound_throws() {
        when(employeeRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class,
                () -> employeeService.updateStatus(99L, new StatusUpdateRequest("INACTIVE")));
    }

    // ---------- history ----------

    @Test
    void getHistory_returnsRowsFromRepository() {
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(
                employee(1L, "Amit Sharma", "amit@example.com", department(1L, "IT"), "50000")));
        LocalDateTime now = LocalDateTime.of(2024, 2, 1, 9, 0);
        when(historyRepo.findByEmployeeIdOrderByChangedAtDescIdAsc(1L)).thenReturn(List.of(
                new EmployeeHistory(1L, "status", "ACTIVE", "INACTIVE", now),
                new EmployeeHistory(1L, "salary", "50000.00", "55000.00", now.minusDays(1))));

        List<EmployeeHistoryResponse> result = employeeService.getEmployeeHistory(1L);

        assertEquals(2, result.size());
        assertEquals("status", result.get(0).fieldName());
        assertEquals("ACTIVE", result.get(0).oldValue());
        assertEquals("INACTIVE", result.get(0).newValue());
        assertEquals(now, result.get(0).changedAt());
    }

    @Test
    void getHistory_employeeNotFound_throwsWithoutReadingHistory() {
        when(employeeRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.getEmployeeHistory(99L));

        verifyNoInteractions(historyRepo);
    }

    // ---------- delete ----------

    @Test
    void delete_existing_deletesIt() {
        Employee existing = employee(1L, "Amit Sharma", "amit@example.com", department(1L, "IT"), "50000");
        when(employeeRepo.findById(1L)).thenReturn(Optional.of(existing));

        employeeService.deleteEmployee(1L);

        verify(employeeRepo).delete(existing);
    }

    @Test
    void delete_notFound_throws() {
        when(employeeRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.deleteEmployee(99L));
        verify(employeeRepo, never()).delete(any(Employee.class));
    }

    // ---------- list: paging, sorting, status filter ----------

    @Test
    void list_noStatus_returnsPageDetailsFromFindAll() {
        Department it = department(1L, "IT");
        List<Employee> content = List.of(
                employee(3L, "A", "a@example.com", it, "10000"),
                employee(4L, "B", "b@example.com", it, "20000"));
        when(employeeRepo.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(content, PageRequest.of(1, 2), 5));

        PageResponse<EmployeeResponse> page = employeeService.getAllEmployees(1, 2, "salary", "desc", null);

        assertEquals(2, page.content().size());
        assertEquals(1, page.currentPage());
        assertEquals(2, page.pageSize());
        assertEquals(5, page.totalRecords());
        assertEquals(3, page.totalPages());
        verify(employeeRepo, never()).findByStatus(any(), any());
    }

    @Test
    void list_passesRequestedSortToRepository() {
        when(employeeRepo.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        employeeService.getAllEmployees(0, 10, "salary", "desc", null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(employeeRepo).findAll(captor.capture());
        assertEquals(Sort.Direction.DESC, captor.getValue().getSort().getOrderFor("salary").getDirection());
    }

    @Test
    void list_sortByDepartment_sortsByDepartmentName() {
        when(employeeRepo.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        employeeService.getAllEmployees(0, 10, "department", "asc", null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(employeeRepo).findAll(captor.capture());
        assertNotNull(captor.getValue().getSort().getOrderFor("department.name"));
    }

    @Test
    void list_withStatus_usesStatusFilterOnly() {
        Employee inactive = employee(5L, "Gone", "gone@example.com", department(1L, "IT"), "30000");
        inactive.setStatus(EmployeeStatus.INACTIVE);
        when(employeeRepo.findByStatus(eq(EmployeeStatus.INACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(inactive), PageRequest.of(0, 10), 1));

        PageResponse<EmployeeResponse> page = employeeService.getAllEmployees(0, 10, "fullName", "asc", "inactive");

        assertEquals(1, page.content().size());
        assertEquals(EmployeeStatus.INACTIVE, page.content().get(0).status());
        verify(employeeRepo, never()).findAll(any(Pageable.class));
    }

    @Test
    void list_invalidStatus_throws() {
        assertThrows(InvalidRequestException.class,
                () -> employeeService.getAllEmployees(0, 10, "fullName", "asc", "bogus"));
    }

    @Test
    void list_invalidSortBy_throws() {
        assertThrows(InvalidRequestException.class,
                () -> employeeService.getAllEmployees(0, 10, "password", "asc", null));
    }

    @Test
    void list_invalidDirection_throws() {
        assertThrows(InvalidRequestException.class,
                () -> employeeService.getAllEmployees(0, 10, "salary", "up", null));
    }

    @Test
    void list_negativePage_throws() {
        assertThrows(InvalidRequestException.class,
                () -> employeeService.getAllEmployees(-1, 10, "salary", "asc", null));
    }

    @Test
    void list_sizeZero_throws() {
        assertThrows(InvalidRequestException.class,
                () -> employeeService.getAllEmployees(0, 0, "salary", "asc", null));
    }

    @Test
    void list_sizeAbove100_throws() {
        assertThrows(InvalidRequestException.class,
                () -> employeeService.getAllEmployees(0, 101, "salary", "asc", null));
    }

    // ---------- name search ----------

    @Test
    void searchByName_returnsMatches() {
        when(employeeRepo.findByFullNameContainingIgnoreCase("priya")).thenReturn(List.of(
                employee(2L, "Priya Kumari", "priya@example.com", department(2L, "Finance"), "75000")));

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
    void salaryRange_valid_returnsMatches() {
        BigDecimal min = new BigDecimal("50000");
        BigDecimal max = new BigDecimal("80000");
        when(employeeRepo.findBySalaryBetween(min, max)).thenReturn(List.of(
                employee(2L, "Priya Kumari", "priya@example.com", department(2L, "Finance"), "75000")));

        assertEquals(1, employeeService.getEmployeesBySalaryRange(min, max).size());
    }

    @Test
    void salaryRange_negativeMin_throws() {
        assertThrows(InvalidRequestException.class,
                () -> employeeService.getEmployeesBySalaryRange(new BigDecimal("-1"), new BigDecimal("5000")));
    }

    @Test
    void salaryRange_negativeMax_throws() {
        assertThrows(InvalidRequestException.class,
                () -> employeeService.getEmployeesBySalaryRange(new BigDecimal("0"), new BigDecimal("-5")));
    }

    @Test
    void salaryRange_minGreaterThanMax_throws() {
        assertThrows(InvalidRequestException.class,
                () -> employeeService.getEmployeesBySalaryRange(new BigDecimal("90000"), new BigDecimal("50000")));
    }

    // ---------- by department ----------

    @Test
    void byDepartment_returnsEmployeesOfThatDepartment() {
        when(employeeRepo.findByDepartmentNameIgnoreCase("finance")).thenReturn(List.of(
                employee(2L, "Priya Kumari", "priya@example.com", department(2L, "Finance"), "75000")));

        List<EmployeeResponse> result = employeeService.getEmployeesByDepartment("finance");

        assertEquals(1, result.size());
        assertEquals("Finance", result.get(0).department());
    }

    // ---------- department summary ----------

    @Test
    void summary_roundsAverageToTwoDecimals() {
        when(employeeRepo.getDepartmentSummary())
                .thenReturn(List.of(new DepartmentSummaryResponse("IT", 3L, 70000.3333)));

        List<DepartmentSummaryResponse> result = employeeService.getDepartmentSummary();

        assertEquals(1, result.size());
        assertEquals("IT", result.get(0).department());
        assertEquals(3L, result.get(0).employeeCount());
        assertEquals(70000.33, result.get(0).averageSalary(), 0.001);
    }
}