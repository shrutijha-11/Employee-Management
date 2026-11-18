package com.shruti.EmpManage;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ManagerTest extends ApiTestBase {

    @BeforeEach
    void setUp() throws Exception {
        createDepartment("Engineering");
    }

    @Test
    void responseShowsManagerIdAndName() throws Exception {
        long managerId = createEmployee("Asha Rao", "asha@example.com", "Engineering", "9000000001", "2026-01-15");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("Ravi Das", "ravi@example.com", "Engineering",
                                "9000000002", "12 Park Street", "2026-01-15", managerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.managerId").value((int) managerId))
                .andExpect(jsonPath("$.managerName").value("Asha Rao"));
    }

    @Test
    void noManagerMeansEmptyManagerFields() throws Exception {
        long id = createEmployee("Asha Rao", "asha@example.com", "Engineering", "9000000001", "2026-01-15");

        mockMvc.perform(get("/api/employees/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.managerId").isEmpty())
                .andExpect(jsonPath("$.managerName").isEmpty());
    }

    @Test
    void createWithMissingManagerReturns404AndSavesNothing() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("Ravi Das", "ravi@example.com", "Engineering",
                                "9000000002", "12 Park Street", "2026-01-15", 9999L)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(0));
    }

    @Test
    void employeeCannotBeTheirOwnManager() throws Exception {
        long id = createEmployee("Asha Rao", "asha@example.com", "Engineering", "9000000001", "2026-01-15");

        mockMvc.perform(put("/api/employees/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("Asha Rao", "asha@example.com", "9000000001", id, 0)))
                .andExpect(status().isBadRequest());

        // nothing was saved
        String after = getEmployeeJson(id);
        assertEquals(0, ((Number) JsonPath.read(after, "$.version")).intValue());
        assertNull(JsonPath.read(after, "$.managerId"));
    }

    @Test
    void updateWithMissingManagerReturns404AndChangesNothing() throws Exception {
        long id = createEmployee("Asha Rao", "asha@example.com", "Engineering", "9000000001", "2026-01-15");

        mockMvc.perform(put("/api/employees/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("Asha Rao", "asha@example.com", "9000000001", 9999L, 0)))
                .andExpect(status().isNotFound());

        String after = getEmployeeJson(id);
        assertEquals(0, ((Number) JsonPath.read(after, "$.version")).intValue());
        assertNull(JsonPath.read(after, "$.managerId"));
    }

    @Test
    void assigningAndClearingAManagerIsWrittenToHistory() throws Exception {
        long managerId = createEmployee("Asha Rao", "asha@example.com", "Engineering", "9000000001", "2026-01-15");
        long id = createEmployee("Ravi Das", "ravi@example.com", "Engineering", "9000000002", "2026-01-15");

        // assign: no manager -> manager
        mockMvc.perform(put("/api/employees/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("Ravi Das", "ravi@example.com", "9000000002", managerId, 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.managerId").value((int) managerId));

        List<Map<String, Object>> rows = managerHistory(id);
        assertEquals(1, rows.size());
        assertNull(rows.get(0).get("oldValue"));
        assertEquals(String.valueOf(managerId), rows.get(0).get("newValue"));

        // clear: manager -> no manager
        long version = ((Number) JsonPath.read(getEmployeeJson(id), "$.version")).longValue();
        mockMvc.perform(put("/api/employees/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("Ravi Das", "ravi@example.com", "9000000002", null, version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.managerId").isEmpty());

        rows = managerHistory(id);
        assertEquals(2, rows.size());
        long clearedRows = rows.stream()
                .filter(r -> String.valueOf(managerId).equals(r.get("oldValue")) && r.get("newValue") == null)
                .count();
        assertEquals(1, clearedRows);
    }

    @Test
    void cannotDeleteAnEmployeeWhoStillManagesSomeone() throws Exception {
        long managerId = createEmployee("Asha Rao", "asha@example.com", "Engineering", "9000000001", "2026-01-15");
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("Ravi Das", "ravi@example.com", "Engineering",
                                "9000000002", "12 Park Street", "2026-01-15", managerId)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/employees/" + managerId))
                .andExpect(status().isConflict());

        // the manager is still in the database
        mockMvc.perform(get("/api/employees/" + managerId))
                .andExpect(status().isOk());
    }

    @Test
    void managerCanBeDeletedOnceNobodyReportsToThem() throws Exception {
        long managerId = createEmployee("Asha Rao", "asha@example.com", "Engineering", "9000000001", "2026-01-15");
        MvcResult created = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("Ravi Das", "ravi@example.com", "Engineering",
                                "9000000002", "12 Park Street", "2026-01-15", managerId)))
                .andExpect(status().isCreated())
                .andReturn();
        long reportId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(delete("/api/employees/" + reportId)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/employees/" + managerId)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/employees/" + managerId)).andExpect(status().isNotFound());
    }

    // ---------- helpers ----------

    // same data as createEmployee(...), so the only difference an update makes is the manager
    private String updateJson(String name, String email, String phone, Long managerId, long version) {
        return """
                {"fullName": "%s", "email": "%s", "department": "Engineering", "salary": 50000,
                 "joiningDate": "2026-01-15", "phoneNumber": "%s", "address": "12 Park Street",
                 "managerId": %s, "version": %d}
                """.formatted(name, email, phone, managerId == null ? "null" : managerId, version);
    }

    private List<Map<String, Object>> managerHistory(long id) throws Exception {
        String body = mockMvc.perform(get("/api/employees/" + id + "/history"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[?(@.fieldName=='managerId')]");
    }
}
