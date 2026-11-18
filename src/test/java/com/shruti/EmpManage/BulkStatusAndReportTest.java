package com.shruti.EmpManage;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BulkStatusAndReportTest extends ApiTestBase {

    @BeforeEach
    void setUp() throws Exception {
        createDepartment("Engineering");
    }

    // ---------- bulk status ----------

    @Test
    void bulkChangesOnlyTheEmployeesWhoDiffer() throws Exception {
        long a = createEmployee("Asha Rao", "asha@example.com", "Engineering", "9000000001", "2026-01-15");
        long b = createEmployee("Ravi Das", "ravi@example.com", "Engineering", "9000000002", "2026-01-15");
        long c = createEmployee("Meera Sen", "meera@example.com", "Engineering", "9000000003", "2026-01-15");
        changeStatus(c, "INACTIVE");   // c is already inactive: version 1, one history row

        mockMvc.perform(patch("/api/employees/bulk-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkJson("[" + a + "," + b + "," + c + "]", "INACTIVE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value((int) a))
                .andExpect(jsonPath("$[0].status").value("INACTIVE"))
                .andExpect(jsonPath("$[0].version").value(1))
                .andExpect(jsonPath("$[1].version").value(1))
                .andExpect(jsonPath("$[2].version").value(1));   // c was not touched again

        assertEquals(1, historyCount(a));
        assertEquals(1, historyCount(b));
        assertEquals(1, historyCount(c));   // still one row, not two
    }

    @Test
    void bulkRepeatedWithSameStatusLeavesEverythingUntouched() throws Exception {
        long a = createEmployee("Asha Rao", "asha@example.com", "Engineering", "9000000001", "2026-01-15");
        long b = createEmployee("Ravi Das", "ravi@example.com", "Engineering", "9000000002", "2026-01-15");
        String body = bulkJson("[" + a + "," + b + "]", "INACTIVE");

        mockMvc.perform(patch("/api/employees/bulk-status")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());

        String before = getEmployeeJson(a);
        mockMvc.perform(patch("/api/employees/bulk-status")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        String after = getEmployeeJson(a);

        assertEquals((Object) JsonPath.read(before, "$.version"), JsonPath.read(after, "$.version"));
        assertEquals((Object) JsonPath.read(before, "$.updatedAt"), JsonPath.read(after, "$.updatedAt"));
        assertEquals(1, historyCount(a));
        assertEquals(1, historyCount(b));
    }

    @Test
    void bulkWithOneMissingIdReturns404AndChangesNothing() throws Exception {
        long a = createEmployee("Asha Rao", "asha@example.com", "Engineering", "9000000001", "2026-01-15");

        mockMvc.perform(patch("/api/employees/bulk-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkJson("[" + a + ",9999]", "INACTIVE")))
                .andExpect(status().isNotFound());

        String after = getEmployeeJson(a);
        assertEquals("ACTIVE", JsonPath.read(after, "$.status"));
        assertEquals(0, ((Number) JsonPath.read(after, "$.version")).intValue());
        assertEquals(0, historyCount(a));
    }

    @Test
    void bulkInvalidRequestsReturn400AndChangeNothing() throws Exception {
        long a = createEmployee("Asha Rao", "asha@example.com", "Engineering", "9000000001", "2026-01-15");

        expectBulk400(bulkJson("[" + a + "]", "PENDING"));          // unknown status
        expectBulk400(bulkJson("[]", "INACTIVE"));                  // empty list
        expectBulk400("{\"status\": \"INACTIVE\"}");                // no ids at all
        expectBulk400(bulkJson("[" + a + "," + a + "]", "INACTIVE")); // repeated id
        expectBulk400("{\"ids\": [" + a + "]}");                    // no status

        String after = getEmployeeJson(a);
        assertEquals("ACTIVE", JsonPath.read(after, "$.status"));
        assertEquals(0, ((Number) JsonPath.read(after, "$.version")).intValue());
    }

    // ---------- joining-month report ----------

    @Test
    void reportListsOnlyThatMonthNewestFirst() throws Exception {
        createEmployee("Asha Rao", "asha@example.com", "Engineering", "9000000001", "2026-03-01");
        createEmployee("Ravi Das", "ravi@example.com", "Engineering", "9000000002", "2026-03-15");
        createEmployee("Meera Sen", "meera@example.com", "Engineering", "9000000003", "2026-03-31");
        createEmployee("Feb Person", "feb@example.com", "Engineering", "9000000004", "2026-02-28");
        createEmployee("Apr Person", "apr@example.com", "Engineering", "9000000005", "2026-04-01");

        mockMvc.perform(get("/api/employees/joining-report?year=2026&month=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(3))
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.employees.length()").value(3))
                .andExpect(jsonPath("$.employees[0].joiningDate").value("2026-03-31"))
                .andExpect(jsonPath("$.employees[1].joiningDate").value("2026-03-15"))
                .andExpect(jsonPath("$.employees[2].joiningDate").value("2026-03-01"));
    }

    @Test
    void reportForAMonthWithNobodyIsEmptyButOk() throws Exception {
        createEmployee("Asha Rao", "asha@example.com", "Engineering", "9000000001", "2026-03-01");

        mockMvc.perform(get("/api/employees/joining-report?year=2026&month=7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.employees.length()").value(0));
    }

    @Test
    void reportIncludesLeapDay() throws Exception {
        createEmployee("Leap Person", "leap@example.com", "Engineering", "9000000001", "2024-02-29");

        mockMvc.perform(get("/api/employees/joining-report?year=2024&month=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void reportRejectsBadMonthMissingValuesAndNonNumbers() throws Exception {
        mockMvc.perform(get("/api/employees/joining-report?year=2026&month=0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/employees/joining-report?year=2026&month=13")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/employees/joining-report?month=3")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/employees/joining-report?year=2026")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/employees/joining-report?year=2026&month=abc")).andExpect(status().isBadRequest());
    }

    // ---------- helpers ----------

    private String bulkJson(String idsArray, String status) {
        return "{\"ids\": " + idsArray + ", \"status\": \"" + status + "\"}";
    }

    private void expectBulk400(String json) throws Exception {
        mockMvc.perform(patch("/api/employees/bulk-status")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    private void changeStatus(long id, String status) throws Exception {
        mockMvc.perform(patch("/api/employees/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"" + status + "\"}"))
                .andExpect(status().isOk());
    }

    private int historyCount(long id) throws Exception {
        String body = mockMvc.perform(get("/api/employees/" + id + "/history"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.length()");
    }
}
