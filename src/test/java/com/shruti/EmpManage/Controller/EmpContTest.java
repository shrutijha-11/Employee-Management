package com.shruti.EmpManage.Controller;

import com.shruti.EmpManage.Dto.DepartmentSummaryResponse;
import com.shruti.EmpManage.Dto.EmployeeRequest;
import com.shruti.EmpManage.Dto.EmployeeResponse;
import com.shruti.EmpManage.Dto.PageResponse;
import com.shruti.EmpManage.Exception.DuplicateEmailException;
import com.shruti.EmpManage.Exception.EmployeeNotFoundException;
import com.shruti.EmpManage.Exception.InvalidRequestException;
import com.shruti.EmpManage.Service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmpCont.class)
class EmpContTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

    // ---------- helpers ----------

    private static final String VALID_JSON = """
            {
              "fullName": "Amit Sharma",
              "email": "amit@example.com",
              "department": "IT",
              "salary": 50000,
              "joiningDate": "2024-01-15"
            }
            """;

    private EmployeeResponse response(Long id, String name, String email, String dept, String salary) {
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 10, 0);
        return new EmployeeResponse(id, name, email, dept, new BigDecimal(salary),
                LocalDate.of(2024, 1, 15), now, now);
    }

    // ---------- create (POST) ----------

    @Test
    void create_validRequest_returns201() throws Exception {
        when(employeeService.createEmployee(any(EmployeeRequest.class)))
                .thenReturn(response(1L, "Amit Sharma", "amit@example.com", "IT", "50000"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("Amit Sharma"));
    }

    @Test
    void create_idInBodyIsIgnored_returns201() throws Exception {
        when(employeeService.createEmployee(any(EmployeeRequest.class)))
                .thenReturn(response(1L, "Amit Sharma", "amit@example.com", "IT", "50000"));

        String jsonWithId = VALID_JSON.replace("{", "{ \"id\": 50,");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));   // generated id, not 50
    }

    @Test
    void create_negativeSalary_returns400() throws Exception {
        String json = VALID_JSON.replace("50000", "-50000");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.salary").value("Salary cannot be negative"));
    }

    @Test
    void create_invalidEmail_returns400() throws Exception {
        String json = VALID_JSON.replace("amit@example.com", "notanemail");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.email").value("Email must be valid"));
    }

    @Test
    void create_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_badDateFormat_returns400() throws Exception {
        String json = VALID_JSON.replace("2024-01-15", "15-01-2024");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_duplicateEmail_returns409() throws Exception {
        when(employeeService.createEmployee(any(EmployeeRequest.class)))
                .thenThrow(new DuplicateEmailException("amit@example.com"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isConflict());
    }

    // ---------- get all, paginated (GET) ----------

    @Test
    void getAll_defaults_returns200WithPageDetails() throws Exception {
        PageResponse<EmployeeResponse> page = new PageResponse<>(
                List.of(response(1L, "Amit Sharma", "amit@example.com", "IT", "50000")),
                0, 10, 1, 1);
        when(employeeService.getAllEmployees(0, 10, "fullName", "asc")).thenReturn(page);

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fullName").value("Amit Sharma"))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalRecords").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getAll_withPagingAndSorting_passesParametersToService() throws Exception {
        PageResponse<EmployeeResponse> page = new PageResponse<>(List.of(), 1, 2, 5, 3);
        when(employeeService.getAllEmployees(1, 2, "salary", "desc")).thenReturn(page);

        mockMvc.perform(get("/api/employees")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sortBy", "salary")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.totalPages").value(3));

        verify(employeeService).getAllEmployees(1, 2, "salary", "desc");
    }

    @Test
    void getAll_invalidSortBy_returns400() throws Exception {
        when(employeeService.getAllEmployees(anyInt(), anyInt(), eq("password"), anyString()))
                .thenThrow(new InvalidRequestException("sortBy must be one of: fullName, email, department, salary, joiningDate"));

        mockMvc.perform(get("/api/employees").param("sortBy", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "sortBy must be one of: fullName, email, department, salary, joiningDate"));
    }

    @Test
    void getAll_nonNumericPage_returns400() throws Exception {
        mockMvc.perform(get("/api/employees").param("page", "abc"))
                .andExpect(status().isBadRequest());
    }

    // ---------- get by id (GET) ----------

    @Test
    void getById_found_returns200() throws Exception {
        when(employeeService.getEmployeeById(1L))
                .thenReturn(response(1L, "Amit Sharma", "amit@example.com", "IT", "50000"));

        mockMvc.perform(get("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("amit@example.com"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(employeeService.getEmployeeById(99L)).thenThrow(new EmployeeNotFoundException(99L));

        mockMvc.perform(get("/api/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found with id: 99"));
    }

    // ---------- update (PUT) ----------

    @Test
    void update_validRequest_returns200() throws Exception {
        when(employeeService.updateEmployee(eq(1L), any(EmployeeRequest.class)))
                .thenReturn(response(1L, "Amit Sharma", "amit@example.com", "Finance", "65000"));

        mockMvc.perform(put("/api/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").value("Finance"));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        when(employeeService.updateEmployee(eq(99L), any(EmployeeRequest.class)))
                .thenThrow(new EmployeeNotFoundException(99L));

        mockMvc.perform(put("/api/employees/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_duplicateEmail_returns409() throws Exception {
        when(employeeService.updateEmployee(eq(1L), any(EmployeeRequest.class)))
                .thenThrow(new DuplicateEmailException("amit@example.com"));

        mockMvc.perform(put("/api/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void update_invalidBody_returns400() throws Exception {
        String json = VALID_JSON.replace("50000", "-1");

        mockMvc.perform(put("/api/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    // ---------- delete (DELETE) ----------

    @Test
    void delete_existing_returns204() throws Exception {
        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isNoContent());

        verify(employeeService).deleteEmployee(1L);
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(new EmployeeNotFoundException(99L)).when(employeeService).deleteEmployee(99L);

        mockMvc.perform(delete("/api/employees/99"))
                .andExpect(status().isNotFound());
    }

    // ---------- department search (GET) ----------

    @Test
    void byDepartment_returns200() throws Exception {
        when(employeeService.getEmployeesByDepartment("Finance"))
                .thenReturn(List.of(response(2L, "Priya Kumari", "priya@example.com", "Finance", "75000")));

        mockMvc.perform(get("/api/employees/department/Finance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].department").value("Finance"));
    }

    // ---------- name search (GET) ----------

    @Test
    void searchByName_returns200() throws Exception {
        when(employeeService.searchByName("priya"))
                .thenReturn(List.of(response(2L, "Priya Kumari", "priya@example.com", "Finance", "75000")));

        mockMvc.perform(get("/api/employees/search").param("name", "priya"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Priya Kumari"));
    }

    @Test
    void searchByName_missingParam_returns400() throws Exception {
        mockMvc.perform(get("/api/employees/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchByName_blank_returns400() throws Exception {
        when(employeeService.searchByName(" "))
                .thenThrow(new InvalidRequestException("name must not be blank"));

        mockMvc.perform(get("/api/employees/search").param("name", " "))
                .andExpect(status().isBadRequest());
    }

    // ---------- salary range (GET) ----------

    @Test
    void salaryRange_valid_returns200() throws Exception {
        when(employeeService.getEmployeesBySalaryRange(new BigDecimal("50000"), new BigDecimal("80000")))
                .thenReturn(List.of(response(2L, "Priya Kumari", "priya@example.com", "Finance", "75000")));

        mockMvc.perform(get("/api/employees/salary").param("min", "50000").param("max", "80000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    void salaryRange_invalidRange_returns400() throws Exception {
        when(employeeService.getEmployeesBySalaryRange(new BigDecimal("90000"), new BigDecimal("50000")))
                .thenThrow(new InvalidRequestException("min must not be greater than max"));

        mockMvc.perform(get("/api/employees/salary").param("min", "90000").param("max", "50000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("min must not be greater than max"));
    }

    @Test
    void salaryRange_nonNumeric_returns400() throws Exception {
        mockMvc.perform(get("/api/employees/salary").param("min", "abc").param("max", "5000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void salaryRange_missingMax_returns400() throws Exception {
        mockMvc.perform(get("/api/employees/salary").param("min", "1000"))
                .andExpect(status().isBadRequest());
    }

    // ---------- department summary (GET) ----------

    @Test
    void summary_returns200() throws Exception {
        when(employeeService.getDepartmentSummary())
                .thenReturn(List.of(
                        new DepartmentSummaryResponse("Finance", 2L, 70000.0),
                        new DepartmentSummaryResponse("IT", 3L, 55000.5)));

        mockMvc.perform(get("/api/employees/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].department").value("Finance"))
                .andExpect(jsonPath("$[0].employeeCount").value(2))
                .andExpect(jsonPath("$[1].averageSalary").value(55000.5));
    }
}
