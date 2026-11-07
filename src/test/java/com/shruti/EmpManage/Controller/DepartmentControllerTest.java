package com.shruti.EmpManage.Controller;

import com.shruti.EmpManage.Dto.DepartmentRequest;
import com.shruti.EmpManage.Dto.DepartmentResponse;
import com.shruti.EmpManage.Exception.DepartmentInUseException;
import com.shruti.EmpManage.Exception.DepartmentNotFoundException;
import com.shruti.EmpManage.Exception.DuplicateDepartmentException;
import com.shruti.EmpManage.Service.DepartmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DepartmentController.class)
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DepartmentService departmentService;

    private static final String VALID_JSON = """
            {
              "name": "IT",
              "location": "Kolkata"
            }
            """;

    // ---------- create (POST) ----------

    @Test
    void create_validRequest_returns201() throws Exception {
        when(departmentService.createDepartment(any(DepartmentRequest.class)))
                .thenReturn(new DepartmentResponse(1L, "IT", "Kolkata"));

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("IT"))
                .andExpect(jsonPath("$.location").value("Kolkata"));
    }

    @Test
    void create_duplicateName_returns409() throws Exception {
        when(departmentService.createDepartment(any(DepartmentRequest.class)))
                .thenThrow(new DuplicateDepartmentException("IT"));

        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A department named 'IT' already exists"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"location\": \"Kolkata\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.name").value("Department name is required"));

        verify(departmentService, never()).createDepartment(any());
    }

    @Test
    void create_missingLocation_returns400() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"IT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.location").value("Location is required"));
    }

    @Test
    void create_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest());
    }

    // ---------- list and get (GET) ----------

    @Test
    void list_returns200WithAllDepartments() throws Exception {
        when(departmentService.getAllDepartments()).thenReturn(List.of(
                new DepartmentResponse(1L, "IT", "Kolkata"),
                new DepartmentResponse(2L, "Finance", "Delhi")));

        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("IT"))
                .andExpect(jsonPath("$[1].name").value("Finance"))
                .andExpect(jsonPath("$[1].location").value("Delhi"));
    }

    @Test
    void list_noDepartments_returns200WithEmptyList() throws Exception {
        when(departmentService.getAllDepartments()).thenReturn(List.of());

        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getById_found_returns200() throws Exception {
        when(departmentService.getDepartmentById(1L)).thenReturn(new DepartmentResponse(1L, "IT", "Kolkata"));

        mockMvc.perform(get("/api/departments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("IT"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(departmentService.getDepartmentById(99L)).thenThrow(new DepartmentNotFoundException(99L));

        mockMvc.perform(get("/api/departments/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Department not found with id: 99"));
    }

    // ---------- update (PUT) ----------

    @Test
    void update_validRequest_returns200() throws Exception {
        when(departmentService.updateDepartment(eq(1L), any(DepartmentRequest.class)))
                .thenReturn(new DepartmentResponse(1L, "IT", "Delhi"));

        mockMvc.perform(put("/api/departments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"IT\", \"location\": \"Delhi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Delhi"));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        when(departmentService.updateDepartment(eq(99L), any(DepartmentRequest.class)))
                .thenThrow(new DepartmentNotFoundException(99L));

        mockMvc.perform(put("/api/departments/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_nameBelongsToAnotherDepartment_returns409() throws Exception {
        when(departmentService.updateDepartment(eq(1L), any(DepartmentRequest.class)))
                .thenThrow(new DuplicateDepartmentException("Finance"));

        mockMvc.perform(put("/api/departments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Finance\", \"location\": \"Kolkata\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void update_invalidBody_returns400AndDoesNotCallTheService() throws Exception {
        mockMvc.perform(put("/api/departments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"location\": \"\"}"))
                .andExpect(status().isBadRequest());

        verify(departmentService, never()).updateDepartment(any(), any());
    }

    // ---------- delete (DELETE) ----------

    @Test
    void delete_departmentWithoutEmployees_returns204() throws Exception {
        mockMvc.perform(delete("/api/departments/1"))
                .andExpect(status().isNoContent());

        verify(departmentService).deleteDepartment(1L);
    }

    @Test
    void delete_departmentStillHasEmployees_returns409() throws Exception {
        doThrow(new DepartmentInUseException(1L)).when(departmentService).deleteDepartment(1L);

        mockMvc.perform(delete("/api/departments/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        "Department 1 cannot be deleted because employees still belong to it"));
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(new DepartmentNotFoundException(99L)).when(departmentService).deleteDepartment(99L);

        mockMvc.perform(delete("/api/departments/99"))
                .andExpect(status().isNotFound());
    }
}
