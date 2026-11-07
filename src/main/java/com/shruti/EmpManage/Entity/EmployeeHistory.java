package com.shruti.EmpManage.Entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee_history")

public class EmployeeHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private String fieldName;

    @Column(nullable = false)
    private String oldValue;

    @Column(nullable = false)
    private String newValue;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    public EmployeeHistory() {
    }

    public EmployeeHistory(Long employeeId, String fieldName, String oldValue,
                           String newValue, LocalDateTime changedAt) {
        this.employeeId = employeeId;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changedAt = changedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
