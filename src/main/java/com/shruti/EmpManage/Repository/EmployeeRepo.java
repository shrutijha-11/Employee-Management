package com.shruti.EmpManage.Repository;

import com.shruti.EmpManage.Entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;

import com.shruti.EmpManage.Dto.DepartmentSummaryResponse;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

@Repository
public interface EmployeeRepo extends JpaRepository<Employee,Long>
{
    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<Employee> findByDepartmentIgnoreCase(String department);

    List<Employee> findByFullNameContainingIgnoreCase(String name);

    List<Employee> findBySalaryBetween(BigDecimal min, BigDecimal max);

    @Query("""
            SELECT new com.shruti.EmpManage.Dto.DepartmentSummaryResponse(
                e.department, COUNT(e), AVG(e.salary))
            FROM Employee e
            GROUP BY e.department
            ORDER BY e.department
            """)
    List<DepartmentSummaryResponse> getDepartmentSummary();
}
