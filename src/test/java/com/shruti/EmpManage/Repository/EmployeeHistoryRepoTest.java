package com.shruti.EmpManage.Repository;

import com.shruti.EmpManage.Entity.EmployeeHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class EmployeeHistoryRepoTest {

    @Autowired
    private EmployeeHistoryRepo historyRepo;

    private static final LocalDateTime DAY_1 = LocalDateTime.of(2024, 2, 1, 9, 0);
    private static final LocalDateTime DAY_2 = LocalDateTime.of(2024, 2, 2, 9, 0);
    private static final LocalDateTime DAY_3 = LocalDateTime.of(2024, 2, 3, 9, 0);

    @BeforeEach
    void setUp() {
        historyRepo.deleteAll();
    }

    private void saveRow(Long employeeId, String field, String oldValue, String newValue, LocalDateTime at) {
        historyRepo.saveAndFlush(new EmployeeHistory(employeeId, field, oldValue, newValue, at));
    }

    @Test
    void savedRow_keepsAllItsFields() {
        saveRow(1L, "salary", "50000.00", "55000.00", DAY_1);

        EmployeeHistory row = historyRepo.findByEmployeeIdOrderByChangedAtDescIdAsc(1L).get(0);

        assertNotNull(row.getId());
        assertEquals(1L, row.getEmployeeId());
        assertEquals("salary", row.getFieldName());
        assertEquals("50000.00", row.getOldValue());
        assertEquals("55000.00", row.getNewValue());
        assertEquals(DAY_1, row.getChangedAt());
    }

    @Test
    void findByEmployeeId_returnsNewestFirst() {
        saveRow(1L, "fullName", "A", "B", DAY_1);
        saveRow(1L, "salary", "50000.00", "55000.00", DAY_3);
        saveRow(1L, "email", "a@x.com", "b@x.com", DAY_2);

        List<EmployeeHistory> rows = historyRepo.findByEmployeeIdOrderByChangedAtDescIdAsc(1L);

        assertEquals(3, rows.size());
        assertEquals("salary", rows.get(0).getFieldName());     // DAY_3
        assertEquals("email", rows.get(1).getFieldName());      // DAY_2
        assertEquals("fullName", rows.get(2).getFieldName());   // DAY_1
    }

    @Test
    void rowsFromTheSameChange_keepTheOrderTheyWereWritten() {
        saveRow(1L, "department", "IT", "Finance", DAY_1);
        saveRow(1L, "salary", "50000.00", "55000.00", DAY_1);
        saveRow(1L, "status", "ACTIVE", "INACTIVE", DAY_1);

        List<EmployeeHistory> rows = historyRepo.findByEmployeeIdOrderByChangedAtDescIdAsc(1L);

        assertEquals("department", rows.get(0).getFieldName());
        assertEquals("salary", rows.get(1).getFieldName());
        assertEquals("status", rows.get(2).getFieldName());
    }

    @Test
    void findByEmployeeId_returnsOnlyThatEmployeesRows() {
        saveRow(1L, "salary", "50000.00", "55000.00", DAY_1);
        saveRow(2L, "salary", "60000.00", "65000.00", DAY_1);
        saveRow(2L, "status", "ACTIVE", "INACTIVE", DAY_2);

        assertEquals(1, historyRepo.findByEmployeeIdOrderByChangedAtDescIdAsc(1L).size());
        assertEquals(2, historyRepo.findByEmployeeIdOrderByChangedAtDescIdAsc(2L).size());
    }

    @Test
    void findByEmployeeId_noRows_returnsEmptyList() {
        assertTrue(historyRepo.findByEmployeeIdOrderByChangedAtDescIdAsc(99L).isEmpty());
    }

    @Test
    void history_isStoredWithoutADatabaseLinkToTheEmployee() {
        // employee 500 does not exist in the employees table, and the row is still accepted,
        // so deleting an employee can never be blocked by (or erase) its history
        saveRow(500L, "status", "ACTIVE", "INACTIVE", DAY_1);

        assertEquals(1, historyRepo.findByEmployeeIdOrderByChangedAtDescIdAsc(500L).size());
    }
}