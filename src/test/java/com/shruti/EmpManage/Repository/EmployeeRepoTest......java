package com.shruti.EmpManage.Repository;

import com.shruti.EmpManage.Dto.DepartmentSummaryResponse;
import com.shruti.EmpManage.Entity.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class EmployeeRepoTest {

    @Autowired
    private EmployeeRepo employeeRepo;

    private Employee amit;
    private Employee priya;

    @BeforeEach
    void setUp() {
        employeeRepo.deleteAll();
        amit = save("Amit Sharma", "amit@example.com", "IT", "50000");
        priya = save("Priya Kumari", "priya@example.com", "Finance", "75000");
        save("Rahul Verma", "rahul@example.com", "IT", "60000");
        save("Anisha Roy", "anisha@example.com", "Finance", "65000");
        save("Bhutri Devi", "bhutri@example.com", "HR", "100000");
    }

    private Employee save(String name, String email, String dept, String salary) {
        Employee e = new Employee();
        e.setFullName(name);
        e.setEmail(email);
        e.setDepartment(dept);
        e.setSalary(new BigDecimal(salary));
        e.setJoiningDate(LocalDate.of(2024, 1, 15));
        LocalDateTime now = LocalDateTime.now();
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        return employeeRepo.save(e);
    }

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
        duplicate.setDepartment("IT");
        duplicate.setSalary(new BigDecimal("1000"));
        duplicate.setJoiningDate(LocalDate.of(2024, 1, 15));
        duplicate.setCreatedAt(LocalDateTime.now());
        duplicate.setUpdatedAt(LocalDateTime.now());

        assertThrows(DataIntegrityViolationException.class,
                () -> employeeRepo.saveAndFlush(duplicate));
    }

    // ---------- department search ----------

    @Test
    void findByDepartmentIgnoreCase_matchesAnyCase() {
        assertEquals(2, employeeRepo.findByDepartmentIgnoreCase("it").size());
        assertEquals(2, employeeRepo.findByDepartmentIgnoreCase("FINANCE").size());
    }

    @Test
    void findByDepartmentIgnoreCase_unknown_returnsEmptyList() {
        assertTrue(employeeRepo.findByDepartmentIgnoreCase("xyz").isEmpty());
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
    void getDepartmentSummary_returnsOneRowPerDepartmentWithCountAndAverage() {
        List<DepartmentSummaryResponse> summary = employeeRepo.getDepartmentSummary();

        assertEquals(3, summary.size());

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

    // ---------- pagination and sorting ----------

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
}
