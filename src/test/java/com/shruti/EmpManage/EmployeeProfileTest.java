package com.shruti.EmpManage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmployeeProfileTest extends ApiTestBase {

    @BeforeEach
    void setUp() throws Exception {
        createDepartment("Engineering");
    }

    @Test
    void createReturnsPhoneNumberAndAddress() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("Asha Rao", "asha@example.com", "Engineering",
                                "9876543210", "12 Park Street", "2026-01-15", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phoneNumber").value("9876543210"))
                .andExpect(jsonPath("$.address").value("12 Park Street"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456789", "12345678901", "98765abcde", "98765 4321", ""})
    void phoneThatIsNotTenDigitsReturns400(String badPhone) throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("Asha Rao", "asha@example.com", "Engineering",
                                badPhone, "12 Park Street", "2026-01-15", null)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankAddressReturns400(String badAddress) throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("Asha Rao", "asha@example.com", "Engineering",
                                "9876543210", badAddress, "2026-01-15", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingPhoneAndAddressReturn400() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": "Asha Rao", "email": "asha@example.com", "department": "Engineering",
                                 "salary": 50000, "joiningDate": "2026-01-15"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.phoneNumber").exists())
                .andExpect(jsonPath("$.message.address").exists());
    }

    @Test
    void duplicatePhoneOnCreateReturns409() throws Exception {
        createEmployee("Asha Rao", "asha@example.com", "Engineering", "9876543210", "2026-01-15");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("Ravi Das", "ravi@example.com", "Engineering",
                                "9876543210", "5 Lake Road", "2026-02-01", null)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateCanKeepOwnPhoneButNotTakeAnothers() throws Exception {
        long ashaId = createEmployee("Asha Rao", "asha@example.com", "Engineering", "9876543210", "2026-01-15");
        createEmployee("Ravi Das", "ravi@example.com", "Engineering", "9123456780", "2026-02-01");

        // same phone as her own: fine
        mockMvc.perform(put("/api/employees/" + ashaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("9876543210", 0)))
                .andExpect(status().isOk());

        // Ravi's phone: conflict (her version is now 1)
        mockMvc.perform(put("/api/employees/" + ashaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson("9123456780", 1)))
                .andExpect(status().isConflict());
    }

    private String updateJson(String phone, long version) {
        return """
                {"fullName": "Asha Rao", "email": "asha@example.com", "department": "Engineering",
                 "salary": 50000, "joiningDate": "2026-01-15", "phoneNumber": "%s",
                 "address": "12 Park Street", "managerId": null, "version": %d}
                """.formatted(phone, version);
    }
}
