package com.shruti.EmpManage;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Every test runs inside a transaction that is rolled back, so tests never affect each other.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class ApiTestBase {

    @Autowired
    protected MockMvc mockMvc;

    protected void createDepartment(String name) throws Exception {
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "location": "Kolkata"}
                                """.formatted(name)))
                .andExpect(status().isCreated());
    }

    protected String employeeJson(String fullName, String email, String department,
                                  String phone, String address, String joiningDate, Long managerId) {
        return """
                {"fullName": "%s", "email": "%s", "department": "%s", "salary": 50000,
                 "joiningDate": "%s", "phoneNumber": "%s", "address": "%s", "managerId": %s}
                """.formatted(fullName, email, department, joiningDate, phone, address,
                managerId == null ? "null" : managerId);
    }

    // creates an employee, expects 201, and returns the new id
    protected long createEmployee(String fullName, String email, String department,
                                  String phone, String joiningDate) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson(fullName, email, department, phone, "12 Park Street", joiningDate, null)))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
    protected String getEmployeeJson(long id) throws Exception {
        return mockMvc.perform(get("/api/employees/" + id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}