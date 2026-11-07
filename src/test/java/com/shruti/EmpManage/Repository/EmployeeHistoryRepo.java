package com.shruti.EmpManage.Repository;

import com.shruti.EmpManage.Entity.EmployeeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeHistoryRepo extends JpaRepository<EmployeeHistory, Long> {

    List<EmployeeHistory> findByEmployeeIdOrderByChangedAtDescIdAsc(Long employeeId);
}