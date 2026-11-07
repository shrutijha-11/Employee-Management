package com.shruti.EmpManage.Repository;

import com.shruti.EmpManage.Dto.DepartmentSummaryResponse;
import com.shruti.EmpManage.Entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.shruti.EmpManage.Entity.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface EmployeeRepo extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByDepartmentId(Long departmentId);

    List<Employee> findByDepartmentNameIgnoreCase(String departmentName);

    List<Employee> findByFullNameContainingIgnoreCase(String name);

    List<Employee> findBySalaryBetween(BigDecimal min, BigDecimal max);

    Page<Employee> findByStatus(EmployeeStatus status, Pageable pageable);

    @Query("""
            SELECT new com.shruti.EmpManage.Dto.DepartmentSummaryResponse(
                d.name, COUNT(e), CAST(AVG(e.salary) AS Double))
            FROM Employee e JOIN e.department d
            GROUP BY d.name
            ORDER BY d.name
            """)
    List<DepartmentSummaryResponse> getDepartmentSummary();
}