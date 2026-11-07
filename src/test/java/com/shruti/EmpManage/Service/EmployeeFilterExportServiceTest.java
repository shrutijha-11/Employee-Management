package com.shruti.EmpManage.Service;

import com.shruti.EmpManage.Dto.EmployeeFilterRequest;
import com.shruti.EmpManage.Dto.EmployeeResponse;
import com.shruti.EmpManage.Dto.PageResponse;
import com.shruti.EmpManage.Entity.Department;
import com.shruti.EmpManage.Entity.Employee;
import com.shruti.EmpManage.Entity.EmployeeStatus;
import com.shruti.EmpManage.Exception.InvalidRequestException;
import com.shruti.EmpManage.Repository.DepartmentRepo;
import com.shruti.EmpManage.Repository.EmployeeHistoryRepo;
import com.shruti.EmpManage.Repository.EmployeeRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeFilterExportServiceTest {

    private static final String CSV_HEADER =
            "id,fullName,email,department,salary,joiningDate,status,createdAt,updatedAt";

    @Mock
    private EmployeeRepo employeeRepo;

    @Mock
    private DepartmentRepo departmentRepo;

    @Mock
    private EmployeeHistoryRepo historyRepo;

    @InjectMocks
    private EmployeeService employeeService;

    // ---------- helpers ----------

    private Employee employee(Long id, String name, String email, String deptName, String salary) {
        Department d = new Department();
        d.setId(1L);
        d.setName(deptName);
        d.setLocation("Kolkata");

        Employee e = new Employee();
        e.setId(id);
        e.setFullName(name);
        e.setEmail(email);
        e.setDepartment(d);
        e.setSalary(new BigDecimal(salary));
        e.setJoiningDate(LocalDate.of(2024, 1, 15));
        e.setStatus(EmployeeStatus.ACTIVE);
        e.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
        e.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
        ReflectionTestUtils.setField(e, "version", 0L);
        return e;
    }

    private EmployeeFilterRequest noFilters() {
        return new EmployeeFilterRequest(null, null, null, null, null, null);
    }

    private void stubFilterQuery(List<Employee> result) {
        when(employeeRepo.findAll(ArgumentMatchers.<Specification<Employee>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(result, PageRequest.of(0, 10), result.size()));
    }

    // ---------- combined filter ----------

    @Test
    void filter_noFilters_returnsPageDetails() {
        stubFilterQuery(List.of(
                employee(1L, "Amit Sharma", "amit@example.com", "IT", "50000"),
                employee(2L, "Priya Kumari", "priya@example.com", "Finance", "75000")));

        PageResponse<EmployeeResponse> page = employeeService.filterEmployees(noFilters(), 0, 10, "fullName", "asc");

        assertEquals(2, page.content().size());
        assertEquals(0, page.currentPage());
        assertEquals(2, page.totalRecords());
        assertEquals(1, page.totalPages());
    }

    @Test
    void filter_allFiltersSent_runsOneQueryWithRequestedPagingAndSorting() {
        stubFilterQuery(List.of(employee(1L, "Amit Sharma", "amit@example.com", "IT", "50000")));
        EmployeeFilterRequest filter = new EmployeeFilterRequest("ami", "it",
                new BigDecimal("40000"), new BigDecimal("60000"),
                LocalDate.of(2023, 1, 1), LocalDate.of(2024, 12, 31));

        PageResponse<EmployeeResponse> page = employeeService.filterEmployees(filter, 0, 5, "salary", "desc");

        assertEquals(1, page.content().size());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(employeeRepo).findAll(ArgumentMatchers.<Specification<Employee>>any(), captor.capture());
        assertEquals(5, captor.getValue().getPageSize());
        assertEquals(Sort.Direction.DESC, captor.getValue().getSort().getOrderFor("salary").getDirection());
    }

    @Test
    void filter_blankNameAndDepartmentAreTreatedAsNotSent() {
        stubFilterQuery(List.of());

        employeeService.filterEmployees(
                new EmployeeFilterRequest("  ", "", null, null, null, null), 0, 10, "fullName", "asc");

        verify(employeeRepo).findAll(ArgumentMatchers.<Specification<Employee>>any(), any(Pageable.class));
    }

    @Test
    void filter_equalLimitsAreAllowed() {
        stubFilterQuery(List.of());
        EmployeeFilterRequest filter = new EmployeeFilterRequest(null, null,
                new BigDecimal("50000"), new BigDecimal("50000"),
                LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 15));

        assertDoesNotThrow(() -> employeeService.filterEmployees(filter, 0, 10, "fullName", "asc"));
    }

    @Test
    void filter_negativeMinSalary_throwsBeforeQuerying() {
        EmployeeFilterRequest filter = new EmployeeFilterRequest(null, null,
                new BigDecimal("-1"), null, null, null);

        assertThrows(InvalidRequestException.class,
                () -> employeeService.filterEmployees(filter, 0, 10, "fullName", "asc"));
        verifyNoInteractions(employeeRepo);
    }

    @Test
    void filter_negativeMaxSalary_throwsBeforeQuerying() {
        EmployeeFilterRequest filter = new EmployeeFilterRequest(null, null,
                null, new BigDecimal("-1"), null, null);

        assertThrows(InvalidRequestException.class,
                () -> employeeService.filterEmployees(filter, 0, 10, "fullName", "asc"));
        verifyNoInteractions(employeeRepo);
    }

    @Test
    void filter_minGreaterThanMax_throwsBeforeQuerying() {
        EmployeeFilterRequest filter = new EmployeeFilterRequest(null, null,
                new BigDecimal("90000"), new BigDecimal("50000"), null, null);

        assertThrows(InvalidRequestException.class,
                () -> employeeService.filterEmployees(filter, 0, 10, "fullName", "asc"));
        verifyNoInteractions(employeeRepo);
    }

    @Test
    void filter_joinedFromAfterJoinedTo_throwsBeforeQuerying() {
        EmployeeFilterRequest filter = new EmployeeFilterRequest(null, null, null, null,
                LocalDate.of(2024, 12, 1), LocalDate.of(2024, 1, 1));

        assertThrows(InvalidRequestException.class,
                () -> employeeService.filterEmployees(filter, 0, 10, "fullName", "asc"));
        verifyNoInteractions(employeeRepo);
    }

    @Test
    void filter_invalidPagingOrSorting_throws() {
        assertThrows(InvalidRequestException.class,
                () -> employeeService.filterEmployees(noFilters(), -1, 10, "fullName", "asc"));
        assertThrows(InvalidRequestException.class,
                () -> employeeService.filterEmployees(noFilters(), 0, 0, "fullName", "asc"));
        assertThrows(InvalidRequestException.class,
                () -> employeeService.filterEmployees(noFilters(), 0, 10, "password", "asc"));
        assertThrows(InvalidRequestException.class,
                () -> employeeService.filterEmployees(noFilters(), 0, 10, "fullName", "up"));
    }

    // ---------- CSV export ----------

    private void stubExportQuery(List<Employee> result) {
        when(employeeRepo.findAll(ArgumentMatchers.<Specification<Employee>>any(), any(Sort.class)))
                .thenReturn(result);
    }

    @Test
    void export_noEmployees_returnsOnlyTheHeaderRow() {
        stubExportQuery(List.of());

        String csv = employeeService.exportEmployeesCsv(null, null);

        assertEquals(CSV_HEADER + "\n", csv);
    }

    @Test
    void export_writesHeaderThenOneRowPerEmployeeInTheRequiredColumnOrder() {
        stubExportQuery(List.of(
                employee(1L, "Amit Sharma", "amit@example.com", "IT", "50000"),
                employee(2L, "Priya Kumari", "priya@example.com", "Finance", "75000")));

        String[] lines = employeeService.exportEmployeesCsv(null, null).split("\n");

        assertEquals(3, lines.length);
        assertEquals(CSV_HEADER, lines[0]);
        assertEquals("1,Amit Sharma,amit@example.com,IT,50000,2024-01-15,ACTIVE,2024-01-01T10:00,2024-01-01T10:00",
                lines[1]);
        assertEquals("2,Priya Kumari,priya@example.com,Finance,75000,2024-01-15,ACTIVE,2024-01-01T10:00,2024-01-01T10:00",
                lines[2]);
    }

    @Test
    void export_valueWithCommaOrQuoteIsQuoted() {
        stubExportQuery(List.of(
                employee(1L, "Smith, John", "john@example.com", "IT", "50000"),
                employee(2L, "Ann \"AJ\" Jones", "ann@example.com", "IT", "50000")));

        String[] lines = employeeService.exportEmployeesCsv(null, null).split("\n");

        assertTrue(lines[1].startsWith("1,\"Smith, John\","));
        assertTrue(lines[2].startsWith("2,\"Ann \"\"AJ\"\" Jones\","));
    }

    @Test
    void export_valueStartingWithFormulaCharacterIsNeutralised() {
        stubExportQuery(List.of(employee(1L, "=SUM(A1)", "x@example.com", "IT", "50000")));

        String[] lines = employeeService.exportEmployeesCsv(null, null).split("\n");

        assertTrue(lines[1].startsWith("1,'=SUM(A1),"));
    }

    @Test
    void export_withDepartmentAndStatusFilters_runsOneQuery() {
        stubExportQuery(List.of(employee(1L, "Amit Sharma", "amit@example.com", "IT", "50000")));

        String csv = employeeService.exportEmployeesCsv("it", "active");

        assertEquals(2, csv.split("\n").length);
        verify(employeeRepo).findAll(ArgumentMatchers.<Specification<Employee>>any(), any(Sort.class));
    }

    @Test
    void export_invalidStatus_throwsBeforeQuerying() {
        assertThrows(InvalidRequestException.class, () -> employeeService.exportEmployeesCsv(null, "bogus"));

        verifyNoInteractions(employeeRepo);
    }
}
