package com.shruti.EmpManage.Repository;

import com.shruti.EmpManage.Dto.DepartmentSummaryResponse;
import com.shruti.EmpManage.Entity.Department;
import com.shruti.EmpManage.Entity.Employee;
import com.shruti.EmpManage.Entity.EmployeeStatus;
import com.shruti.EmpManage.Specs.EmployeeSpecifications;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class EmployeeRepoTest {

    @Autowired
    private EmployeeRepo employeeRepo;

    @Autowired
    private DepartmentRepo departmentRepo;

    @Autowired
    private EntityManager entityManager;

    private Department it;
    private Department finance;
    private Department hr;
    private Department empty;      // a department that has no employees

    private Employee amit;
    private Employee priya;

    /*
     * Test data:
     *   Amit Sharma   IT       50000   2024-01-15
     *   Priya Kumari  Finance  75000   2023-06-10
     *   Rahul Verma   IT       60000   2022-03-01
     *   Anisha Roy    Finance  65000   2023-11-20
     *   Bhutri Devi   HR      100000   2021-09-05   (INACTIVE)
     */
    @BeforeEach
    void setUp() {
        employeeRepo.deleteAll();
        departmentRepo.deleteAll();

        it = saveDepartment("IT");
        finance = saveDepartment("Finance");
        hr = saveDepartment("HR");
        empty = saveDepartment("Empty");

        amit = saveEmployee("Amit Sharma", "amit@example.com", it, "50000", LocalDate.of(2024, 1, 15));
        priya = saveEmployee("Priya Kumari", "priya@example.com", finance, "75000", LocalDate.of(2023, 6, 10));
        saveEmployee("Rahul Verma", "rahul@example.com", it, "60000", LocalDate.of(2022, 3, 1));
        saveEmployee("Anisha Roy", "anisha@example.com", finance, "65000", LocalDate.of(2023, 11, 20));
        Employee bhutri = saveEmployee("Bhutri Devi", "bhutri@example.com", hr, "100000", LocalDate.of(2021, 9, 5));
        bhutri.setStatus(EmployeeStatus.INACTIVE);
        employeeRepo.saveAndFlush(bhutri);
    }

    private Department saveDepartment(String name) {
        Department d = new Department();
        d.setName(name);
        d.setLocation("Kolkata");
        return departmentRepo.save(d);
    }

    private Employee saveEmployee(String name, String email, Department dept, String salary, LocalDate joined) {
        Employee e = new Employee();
        e.setFullName(name);
        e.setEmail(email);
        e.setDepartment(dept);
        e.setSalary(new BigDecimal(salary));
        e.setJoiningDate(joined);
        LocalDateTime now = LocalDateTime.now();
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        e.setPhoneNumber(String.format("%010d", PHONE_COUNTER.incrementAndGet()));
        e.setAddress("12 Park Street");
        return employeeRepo.save(e);
    }
    private static final java.util.concurrent.atomic.AtomicLong PHONE_COUNTER = new java.util.concurrent.atomic.AtomicLong(9000000000L);

    // ---------- email checks ----------

    @Test
    void existsByEmail_existing_returnsTrue() {
        assertTrue(employeeRepo.existsByEmail("amit@example.com"));
    }

    @Test
    void existsByEmail_unknown_returnsFalse() {
        assertFalse(employeeRepo.existsByEmail("nobody@example.com"));
    }

    @Test
    void existsByEmailAndIdNot_ownEmail_returnsFalse() {
        assertFalse(employeeRepo.existsByEmailAndIdNot("amit@example.com", amit.getId()));
    }

    @Test
    void existsByEmailAndIdNot_anotherEmployeesEmail_returnsTrue() {
        assertTrue(employeeRepo.existsByEmailAndIdNot("priya@example.com", amit.getId()));
    }

    @Test
    void duplicateEmail_isRejectedByDatabase() {
        Employee duplicate = new Employee();
        duplicate.setFullName("Another Amit");
        duplicate.setEmail("amit@example.com");
        duplicate.setDepartment(it);
        duplicate.setSalary(new BigDecimal("1000"));
        duplicate.setJoiningDate(LocalDate.of(2024, 1, 15));
        duplicate.setCreatedAt(LocalDateTime.now());
        duplicate.setUpdatedAt(LocalDateTime.now());
        duplicate.setPhoneNumber("9000000555");
        duplicate.setAddress("12 Park Street");

        assertThrows(DataIntegrityViolationException.class,
                () -> employeeRepo.saveAndFlush(duplicate));
    }

    // ---------- defaults and version ----------

    @Test
    void newEmployee_isActiveWithVersionZero() {
        assertEquals(EmployeeStatus.ACTIVE, amit.getStatus());
        assertEquals(0L, amit.getVersion());
    }

    @Test
    void updatingAnEmployee_increasesVersionByOne() {
        amit.setSalary(new BigDecimal("55000"));
        Employee saved = employeeRepo.saveAndFlush(amit);

        assertEquals(1L, saved.getVersion());

        saved.setStatus(EmployeeStatus.INACTIVE);
        assertEquals(2L, employeeRepo.saveAndFlush(saved).getVersion());
    }

    @Test
    void savingWithAnOldVersion_isRejectedAndNothingIsChanged() {
        entityManager.flush();
        entityManager.clear();
        Employee stale = employeeRepo.findById(amit.getId()).orElseThrow();   // version 0
        entityManager.clear();

        Employee fresh = employeeRepo.findById(amit.getId()).orElseThrow();   // also version 0
        fresh.setSalary(new BigDecimal("55000"));
        employeeRepo.saveAndFlush(fresh);                                    // database is now version 1

        stale.setFullName("Old Copy Name");
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> employeeRepo.saveAndFlush(stale));
    }

    // ---------- department link ----------

    @Test
    void existsByDepartmentId_departmentWithEmployees_returnsTrue() {
        assertTrue(employeeRepo.existsByDepartmentId(it.getId()));
    }

    @Test
    void existsByDepartmentId_departmentWithoutEmployees_returnsFalse() {
        assertFalse(employeeRepo.existsByDepartmentId(empty.getId()));
    }

    @Test
    void findByDepartmentName_matchesAnyCase() {
        assertEquals(2, employeeRepo.findByDepartmentNameIgnoreCase("it").size());
        assertEquals(2, employeeRepo.findByDepartmentNameIgnoreCase("FINANCE").size());
    }

    @Test
    void findByDepartmentName_unknown_returnsEmptyList() {
        assertTrue(employeeRepo.findByDepartmentNameIgnoreCase("xyz").isEmpty());
    }

    // ---------- name search ----------

    @Test
    void findByFullName_partialMatch() {
        List<Employee> result = employeeRepo.findByFullNameContainingIgnoreCase("kum");

        assertEquals(1, result.size());
        assertEquals("Priya Kumari", result.get(0).getFullName());
    }

    @Test
    void findByFullName_isCaseInsensitive() {
        List<Employee> result = employeeRepo.findByFullNameContainingIgnoreCase("ANI");

        assertEquals(1, result.size());
        assertEquals("Anisha Roy", result.get(0).getFullName());
    }

    @Test
    void findByFullName_noMatch_returnsEmptyList() {
        assertTrue(employeeRepo.findByFullNameContainingIgnoreCase("zzz").isEmpty());
    }

    // ---------- salary range ----------

    @Test
    void findBySalaryBetween_includesBothLimits() {
        List<Employee> result = employeeRepo.findBySalaryBetween(
                new BigDecimal("50000"), new BigDecimal("75000"));

        // Amit (50000), Rahul (60000), Anisha (65000), Priya (75000)
        assertEquals(4, result.size());
    }

    @Test
    void findBySalaryBetween_sameMinAndMax_returnsExactMatch() {
        List<Employee> result = employeeRepo.findBySalaryBetween(
                new BigDecimal("75000"), new BigDecimal("75000"));

        assertEquals(1, result.size());
        assertEquals(priya.getId(), result.get(0).getId());
    }

    @Test
    void findBySalaryBetween_noMatch_returnsEmptyList() {
        assertTrue(employeeRepo.findBySalaryBetween(
                new BigDecimal("1"), new BigDecimal("100")).isEmpty());
    }

    // ---------- department summary ----------

    @Test
    void getDepartmentSummary_oneRowPerDepartmentWithEmployees_withCountAndAverage() {
        List<DepartmentSummaryResponse> summary = employeeRepo.getDepartmentSummary();

        assertEquals(3, summary.size());   // "Empty" has no employees, so it is not listed

        // ordered by department name: Finance, HR, IT
        assertEquals("Finance", summary.get(0).department());
        assertEquals(2L, summary.get(0).employeeCount());
        assertEquals(70000.0, summary.get(0).averageSalary(), 0.01);   // (75000 + 65000) / 2

        assertEquals("HR", summary.get(1).department());
        assertEquals(1L, summary.get(1).employeeCount());
        assertEquals(100000.0, summary.get(1).averageSalary(), 0.01);

        assertEquals("IT", summary.get(2).department());
        assertEquals(2L, summary.get(2).employeeCount());
        assertEquals(55000.0, summary.get(2).averageSalary(), 0.01);   // (50000 + 60000) / 2
    }

    // ---------- pagination, sorting and the status filter ----------

    @Test
    void findAll_paged_returnsRequestedPageWithTotals() {
        Page<Employee> page = employeeRepo.findAll(
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "salary")));

        assertEquals(2, page.getContent().size());
        assertEquals(5, page.getTotalElements());
        assertEquals(3, page.getTotalPages());
        assertEquals("Bhutri Devi", page.getContent().get(0).getFullName());   // highest salary
        assertEquals("Priya Kumari", page.getContent().get(1).getFullName());
    }

    @Test
    void findAll_lastPage_hasRemainingRecords() {
        Page<Employee> page = employeeRepo.findAll(
                PageRequest.of(2, 2, Sort.by(Sort.Direction.ASC, "fullName")));

        assertEquals(1, page.getContent().size());
        assertEquals("Rahul Verma", page.getContent().get(0).getFullName());   // last alphabetically
    }

    @Test
    void findAll_pageBeyondEnd_returnsEmptyContent() {
        Page<Employee> page = employeeRepo.findAll(PageRequest.of(50, 10));

        assertTrue(page.getContent().isEmpty());
        assertEquals(5, page.getTotalElements());
    }

    @Test
    void findAll_sortedByDepartmentName_putsFinanceFirst() {
        Page<Employee> page = employeeRepo.findAll(
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "department.name")));

        assertEquals("Finance", page.getContent().get(0).getDepartment().getName());
        assertEquals("Finance", page.getContent().get(1).getDepartment().getName());
    }

    @Test
    void findByStatus_active_returnsOnlyActiveEmployees() {
        Page<Employee> page = employeeRepo.findByStatus(EmployeeStatus.ACTIVE, PageRequest.of(0, 10));

        assertEquals(4, page.getTotalElements());
        assertTrue(page.getContent().stream().allMatch(e -> e.getStatus() == EmployeeStatus.ACTIVE));
    }

    @Test
    void findByStatus_inactive_returnsOnlyInactiveEmployees() {
        Page<Employee> page = employeeRepo.findByStatus(EmployeeStatus.INACTIVE, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("Bhutri Devi", page.getContent().get(0).getFullName());
    }

    // ---------- combined filter (Specifications) ----------

    @Test
    void spec_noFilters_returnsEveryone() {
        assertEquals(5, employeeRepo.findAll(Specification.<Employee>unrestricted()).size());
    }

    @Test
    void spec_nameContains_isPartialAndCaseInsensitive() {
        List<Employee> result = employeeRepo.findAll(EmployeeSpecifications.nameContains("ANI"));

        assertEquals(1, result.size());
        assertEquals("Anisha Roy", result.get(0).getFullName());
    }

    @Test
    void spec_nameContains_percentSignIsSearchedLiterally() {
        assertTrue(employeeRepo.findAll(EmployeeSpecifications.nameContains("%")).isEmpty());
    }

    @Test
    void spec_departmentIs_isExactAndCaseInsensitive() {
        assertEquals(2, employeeRepo.findAll(EmployeeSpecifications.departmentIs("it")).size());
        assertTrue(employeeRepo.findAll(EmployeeSpecifications.departmentIs("I")).isEmpty());   // exact, not partial
    }

    @Test
    void spec_salaryAtLeast_includesTheLimit() {
        // 65000 (Anisha), 75000 (Priya), 100000 (Bhutri)
        assertEquals(3, employeeRepo.findAll(EmployeeSpecifications.salaryAtLeast(new BigDecimal("65000"))).size());
    }

    @Test
    void spec_salaryAtMost_includesTheLimit() {
        // 50000 (Amit), 60000 (Rahul)
        assertEquals(2, employeeRepo.findAll(EmployeeSpecifications.salaryAtMost(new BigDecimal("60000"))).size());
    }

    @Test
    void spec_salaryRange_includesBothLimits() {
        Specification<Employee> spec = EmployeeSpecifications.salaryAtLeast(new BigDecimal("60000"))
                .and(EmployeeSpecifications.salaryAtMost(new BigDecimal("75000")));

        // Rahul 60000, Anisha 65000, Priya 75000
        assertEquals(3, employeeRepo.findAll(spec).size());
    }

    @Test
    void spec_joinedOnOrAfter_includesTheDate() {
        // Amit 2024-01-15 and Anisha 2023-11-20 (exactly the date)
        assertEquals(2, employeeRepo.findAll(
                EmployeeSpecifications.joinedOnOrAfter(LocalDate.of(2023, 11, 20))).size());
    }

    @Test
    void spec_joinedOnOrBefore_includesTheDate() {
        // Rahul 2022-03-01 (exactly the date) and Bhutri 2021-09-05
        assertEquals(2, employeeRepo.findAll(
                EmployeeSpecifications.joinedOnOrBefore(LocalDate.of(2022, 3, 1))).size());
    }

    @Test
    void spec_sameDayForBothDates_returnsThatDaysJoiners() {
        Specification<Employee> spec = EmployeeSpecifications.joinedOnOrAfter(LocalDate.of(2022, 3, 1))
                .and(EmployeeSpecifications.joinedOnOrBefore(LocalDate.of(2022, 3, 1)));

        List<Employee> result = employeeRepo.findAll(spec);
        assertEquals(1, result.size());
        assertEquals("Rahul Verma", result.get(0).getFullName());
    }

    @Test
    void spec_statusIs_filtersByStatus() {
        List<Employee> result = employeeRepo.findAll(EmployeeSpecifications.statusIs(EmployeeStatus.INACTIVE));

        assertEquals(1, result.size());
        assertEquals("Bhutri Devi", result.get(0).getFullName());
    }

    @Test
    void spec_departmentAndSalary_applyTogether() {
        Specification<Employee> spec = EmployeeSpecifications.departmentIs("Finance")
                .and(EmployeeSpecifications.salaryAtLeast(new BigDecimal("70000")));

        List<Employee> result = employeeRepo.findAll(spec);
        assertEquals(1, result.size());
        assertEquals("Priya Kumari", result.get(0).getFullName());
    }

    @Test
    void spec_allSixFiltersTogether_returnOnlyTheEmployeeMatchingAll() {
        Specification<Employee> spec = EmployeeSpecifications.nameContains("a")
                .and(EmployeeSpecifications.departmentIs("IT"))
                .and(EmployeeSpecifications.salaryAtLeast(new BigDecimal("40000")))
                .and(EmployeeSpecifications.salaryAtMost(new BigDecimal("55000")))
                .and(EmployeeSpecifications.joinedOnOrAfter(LocalDate.of(2024, 1, 1)))
                .and(EmployeeSpecifications.joinedOnOrBefore(LocalDate.of(2024, 12, 31)));

        List<Employee> result = employeeRepo.findAll(spec);
        assertEquals(1, result.size());
        assertEquals("Amit Sharma", result.get(0).getFullName());
    }

    @Test
    void spec_filtersThatExcludeEachOther_returnNothing() {
        Specification<Employee> spec = EmployeeSpecifications.departmentIs("HR")
                .and(EmployeeSpecifications.salaryAtMost(new BigDecimal("50000")));

        assertTrue(employeeRepo.findAll(spec).isEmpty());
    }

    @Test
    void spec_withPagingAndSorting_totalsDescribeTheFilteredSet() {
        Page<Employee> page = employeeRepo.findAll(
                EmployeeSpecifications.departmentIs("IT"),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "salary")));

        assertEquals(1, page.getContent().size());
        assertEquals(2, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertEquals("Rahul Verma", page.getContent().get(0).getFullName());   // 60000 is the higher IT salary
    }
}