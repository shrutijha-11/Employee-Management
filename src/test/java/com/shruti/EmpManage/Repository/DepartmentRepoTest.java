package com.shruti.EmpManage.Repository;

import com.shruti.EmpManage.Entity.Department;
import com.shruti.EmpManage.Entity.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class DepartmentRepoTest {

    @Autowired
    private DepartmentRepo departmentRepo;

    @Autowired
    private EmployeeRepo employeeRepo;

    private Department it;
    private Department finance;

    @BeforeEach
    void setUp() {
        employeeRepo.deleteAll();
        departmentRepo.deleteAll();
        it = save("IT", "Kolkata");
        finance = save("Finance", "Delhi");
    }

    private Department save(String name, String location) {
        Department d = new Department();
        d.setName(name);
        d.setLocation(location);
        return departmentRepo.save(d);
    }

    private void addEmployeeTo(Department department) {
        Employee e = new Employee();
        e.setFullName("Amit Sharma");
        e.setEmail("amit@example.com");
        e.setDepartment(department);
        e.setSalary(new BigDecimal("50000"));
        e.setJoiningDate(LocalDate.of(2024, 1, 15));
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        employeeRepo.saveAndFlush(e);
    }

    // ---------- create ----------

    @Test
    void save_generatesAnId() {
        assertNotNull(it.getId());
        assertNotEquals(it.getId(), finance.getId());
    }

    @Test
    void duplicateName_isRejectedByTheDatabase() {
        Department duplicate = new Department();
        duplicate.setName("IT");
        duplicate.setLocation("Mumbai");

        assertThrows(DataIntegrityViolationException.class, () -> departmentRepo.saveAndFlush(duplicate));
    }

    // ---------- name checks ----------

    @Test
    void existsByNameIgnoreCase_matchesAnyCase() {
        assertTrue(departmentRepo.existsByNameIgnoreCase("it"));
        assertTrue(departmentRepo.existsByNameIgnoreCase("FINANCE"));
    }

    @Test
    void existsByNameIgnoreCase_unknownName_returnsFalse() {
        assertFalse(departmentRepo.existsByNameIgnoreCase("HR"));
    }

    @Test
    void existsByNameIgnoreCaseAndIdNot_ownName_returnsFalse() {
        assertFalse(departmentRepo.existsByNameIgnoreCaseAndIdNot("it", it.getId()));
    }

    @Test
    void existsByNameIgnoreCaseAndIdNot_anotherDepartmentsName_returnsTrue() {
        assertTrue(departmentRepo.existsByNameIgnoreCaseAndIdNot("finance", it.getId()));
    }

    @Test
    void findByNameIgnoreCase_found_returnsTheDepartment() {
        Optional<Department> found = departmentRepo.findByNameIgnoreCase("fInAnCe");

        assertTrue(found.isPresent());
        assertEquals("Finance", found.get().getName());
        assertEquals("Delhi", found.get().getLocation());
    }

    @Test
    void findByNameIgnoreCase_unknownName_returnsEmpty() {
        assertTrue(departmentRepo.findByNameIgnoreCase("Nope").isEmpty());
    }

    // ---------- update ----------

    @Test
    void updatingTheLocation_isSaved() {
        it.setLocation("Pune");
        departmentRepo.saveAndFlush(it);

        assertEquals("Pune", departmentRepo.findById(it.getId()).orElseThrow().getLocation());
    }

    // ---------- delete ----------

    @Test
    void delete_departmentWithoutEmployees_removesIt() {
        departmentRepo.delete(finance);
        departmentRepo.flush();

        assertFalse(departmentRepo.existsById(finance.getId()));
        assertTrue(departmentRepo.existsById(it.getId()));
    }

    @Test
    void delete_departmentWithEmployees_isBlockedByTheDatabase() {
        addEmployeeTo(it);

        departmentRepo.delete(it);

        // the foreign key from employees stops the delete, even without the service's own check
        assertThrows(DataIntegrityViolationException.class, () -> departmentRepo.flush());
    }
}
