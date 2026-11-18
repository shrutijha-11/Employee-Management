package com.shruti.EmpManage;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardAndHistoryTest extends ApiTestBase {

    @BeforeEach
    void setUp() throws Exception {
        createDepartment("Engineering");
        createDepartment("Sales");
    }

    // ---------- dashboard ----------

    @Test
    void dashboardWithNoEmployeesReturnsZerosAndNullTopDepartment() throws Exception {
        mockMvc.perform(get("/api/employees/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmployees").value(0))
                .andExpect(jsonPath("$.activeEmployees").value(0))
                .andExpect(jsonPath("$.inactiveEmployees").value(0))
                .andExpect(jsonPath("$.averageSalary").value(0.0))
                .andExpect(jsonPath("$.topDepartment").isEmpty());
    }

    @Test
    void dashboardWithEmployeesReturnsCountsAverageAndTopDepartment() throws Exception {
        createWithSalary("Asha Rao", "asha@example.com", "Engineering", "9000000001", 50000);
        createWithSalary("Ravi Das", "ravi@example.com", "Engineering", "9000000002", 50000);
        long meeraId = createWithSalary("Meera Sen", "meera@example.com", "Sales", "9000000003", 80000);

        mockMvc.perform(patch("/api/employees/" + meeraId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"INACTIVE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/employees/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmployees").value(3))
                .andExpect(jsonPath("$.activeEmployees").value(2))
                .andExpect(jsonPath("$.inactiveEmployees").value(1))
                .andExpect(jsonPath("$.averageSalary").value(60000.0))
                .andExpect(jsonPath("$.topDepartment.name").value("Engineering"))
                .andExpect(jsonPath("$.topDepartment.employeeCount").value(2));
    }

    @Test
    void dashboardTopDepartmentTieGoesToTheAlphabeticallyFirstName() throws Exception {
        createWithSalary("Meera Sen", "meera@example.com", "Sales", "9000000003", 50000);
        createWithSalary("Asha Rao", "asha@example.com", "Engineering", "9000000001", 50000);

        mockMvc.perform(get("/api/employees/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topDepartment.name").value("Engineering"))
                .andExpect(jsonPath("$.topDepartment.employeeCount").value(1));
    }

    // ---------- paged history ----------

    @Test
    void pagedHistoryReturnsPagesWithTotals() throws Exception {
        long id = createEmployee("Asha Rao", "asha@example.com", "Engineering", "9000000001", "2026-01-15");
        changeStatus(id, "INACTIVE");
        changeStatus(id, "ACTIVE");
        changeStatus(id, "INACTIVE");   // 3 history rows

        MvcResult first = mockMvc.perform(get("/api/employees/" + id + "/history/paged?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(2))
                .andExpect(jsonPath("$.totalRecords").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andReturn();

        // newest first: the first row's time is not earlier than the second row's
        String body = first.getResponse().getContentAsString();
        String newer = JsonPath.read(body, "$.content[0].changedAt");
        String older = JsonPath.read(body, "$.content[1].changedAt");
        assertTrue(newer.compareTo(older) >= 0, "history should be newest first");

        mockMvc.perform(get("/api/employees/" + id + "/history/paged?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.totalRecords").value(3));
    }

    @Test
    void pagedHistoryPastTheLastPageIsEmptyButOk() throws Exception {
        long id = createEmployee("Asha Rao", "asha@example.com", "Engineering", "9000000001", "2026-01-15");
        changeStatus(id, "INACTIVE");

        mockMvc.perform(get("/api/employees/" + id + "/history/paged?page=5&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalRecords").value(1));
    }

    @Test
    void pagedHistoryOfEmployeeWithNoChangesIsEmpty() throws Exception {
        long id = createEmployee("Asha Rao", "asha@example.com", "Engineering", "9000000001", "2026-01-15");

        mockMvc.perform(get("/api/employees/" + id + "/history/paged"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalRecords").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void pagedHistoryRejectsNegativePageAndSizeBelowOne() throws Exception {
        long id = createEmployee("Asha Rao", "asha@example.com", "Engineering", "9000000001", "2026-01-15");

        mockMvc.perform(get("/api/employees/" + id + "/history/paged?page=-1&size=2"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/employees/" + id + "/history/paged?page=0&size=0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pagedHistoryOfMissingEmployeeReturns404() throws Exception {
        mockMvc.perform(get("/api/employees/9999/history/paged"))
                .andExpect(status().isNotFound());
    }

    @Test
    void oldUnpagedHistoryStillReturnsThePlainList() throws Exception {
        long id = createEmployee("Asha Rao", "asha@example.com", "Engineering", "9000000001", "2026-01-15");
        changeStatus(id, "INACTIVE");
        changeStatus(id, "ACTIVE");

        mockMvc.perform(get("/api/employees/" + id + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---------- helpers ----------

    private void changeStatus(long id, String status) throws Exception {
        mockMvc.perform(patch("/api/employees/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"" + status + "\"}"))
                .andExpect(status().isOk());
    }

    private long createWithSalary(String name, String email, String department, String phone, int salary)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": "%s", "email": "%s", "department": "%s", "salary": %d,
                                 "joiningDate": "2026-01-15", "phoneNumber": "%s",
                                 "address": "12 Park Street", "managerId": null}
                                """.formatted(name, email, department, salary, phone)))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
